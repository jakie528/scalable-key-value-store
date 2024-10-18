package com.kvstore.raft;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import java.util.concurrent.*;
import java.util.*;
import com.kvstore.storage.SQLiteStorage;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.kvstore.raft.RequestVoteRequest;
import com.kvstore.raft.RequestVoteResponse;
import com.kvstore.raft.RaftServiceGrpc;
import com.kvstore.raft.AppendEntriesRequest;
import com.kvstore.raft.AppendEntriesResponse;
import com.kvstore.raft.LogEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaftNode {
    private final String nodeId;
    private final ConcurrentHashMap<String, NodeInfo> peers;
    private final SQLiteStorage storage;
    private final RaftState state;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService workerPool;
    private final ConcurrentHashMap<String, ManagedChannel> grpcChannels;
    private static final Logger logger = LoggerFactory.getLogger(RaftNode.class);

    // Optimized data structures for large clusters
    private final ConcurrentHashMap<String, Long> nextIndex;
    private final ConcurrentHashMap<String, Long> matchIndex;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private String votedFor;
    private ScheduledFuture<?> electionTimer;

    public RaftNode(String nodeId, List<String> peerIds, SQLiteStorage storage) {
        this.nodeId = nodeId;
        this.peers = new ConcurrentHashMap<>();
        peerIds.forEach(id -> this.peers.put(id, new NodeInfo(id, "localhost", 50051)));  // Use appropriate host and port
        this.storage = storage;
        this.state = new RaftState();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.workerPool = Executors.newWorkStealingPool();
        this.nextIndex = new ConcurrentHashMap<>();
        this.matchIndex = new ConcurrentHashMap<>();
        this.votedFor = null;
        this.grpcChannels = new ConcurrentHashMap<>();
        initializeGrpcChannels(peerIds);
    }


    private void initializeGrpcChannels(List<String> peerIds) {
        for (String peerId : peerIds) {
            NodeInfo peerInfo = peers.get(peerId);
            ManagedChannel channel = ManagedChannelBuilder.forAddress(peerInfo.getHost(), peerInfo.getPort())
                    .usePlaintext()
                    .build();
            grpcChannels.put(peerId, channel);
        }
    }

    public void start() {
        scheduler.scheduleAtFixedRate(this::leaderElectionTask, 0, 150, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::heartbeatTask, 0, 50, TimeUnit.MILLISECONDS);
    }

    private void leaderElectionTask() {
        if (state.getCurrentRole() != RaftState.Role.LEADER) {
            startElection();
        }
    }

    /*
    At the beginning of each term, nodes start as followers.
    If a follower doesn’t hear from the leader for a certain period (election timeout), it transitions to the candidate state.
    */
    public void startElection() {
        state.becomeCandidate();
        votedFor = nodeId;

        long lastLogIndex = storage.getLastLogIndex();
        long lastLogTerm = storage.getLastLogTerm();

        CompletableFuture<Boolean>[] voteFutures = peers.values().stream()
                .map(peer -> CompletableFuture.supplyAsync(() -> requestVote(peer, lastLogIndex, lastLogTerm), workerPool))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(voteFutures).thenAccept(v -> {
            long voteCount = Arrays.stream(voteFutures)
                    .map(CompletableFuture::join)
                    .filter(Boolean::booleanValue)
                    .count() + 1; // Add 1 for self-vote

            //The candidate requests votes from other nodes. If it receives votes from the majority, it becomes the leader.
            if (voteCount > peers.size() / 2) {
                becomeLeader();
            }
        });
    }

    private void heartbeatTask() {
        if (state.getCurrentRole() == RaftState.Role.LEADER) {
            peers.values().forEach(peer ->
                    workerPool.submit(() -> sendAppendEntries(peer))
            );
        }
    }

    //
//    public boolean requestVote(NodeInfo peer, long lastLogIndex, long lastLogTerm) {
//        // Implement RPC call to peer for vote request
//        return false; // Placeholder
//    }
    public CompletableFuture<Boolean> requestVote(NodeInfo peer, long lastLogIndex, long lastLogTerm) {
        return CompletableFuture.supplyAsync(() -> {
            rwLock.readLock().lock();
            try {
                RequestVoteRequest request = RequestVoteRequest.newBuilder()
                        .setTerm(state.getCurrentTerm())
                        .setCandidateId(nodeId)
                        .setLastLogIndex(lastLogIndex)
                        .setLastLogTerm(lastLogTerm)
                        .build();

                RaftServiceGrpc.RaftServiceBlockingStub stub = RaftServiceGrpc.newBlockingStub(grpcChannels.get(peer.getId()));
                RequestVoteResponse response = stub.requestVote(request);

                if (response.getTerm() > state.getCurrentTerm()) {
                    state.setCurrentTerm(response.getTerm());
                    state.becomeFollower(response.getTerm());
                    votedFor = null;
                    return false;
                }

                return response.getVoteGranted();
            } catch (Exception e) {
                // Handle gRPC exceptions (e.g., network issues)
                logger.error("Error during RequestVote RPC to " + peer.getId(), e);
                return false;
            } finally {
                rwLock.readLock().unlock();
            }
        }, workerPool);
    }

    //Sends the log entry to followers, which replicate the log entry
    private void sendAppendEntries(NodeInfo follower) {
        long nextIdx = nextIndex.getOrDefault(follower.getId(), 1L);
        List<LogEntry> entries = storage.getLogEntries(nextIdx, storage.getLastLogIndex());
        long prevLogIndex = nextIdx - 1;
        long prevLogTerm = prevLogIndex > 0 ? storage.getLogEntry(prevLogIndex).getTerm() : 0;

        boolean success = appendEntries(follower.getId(), state.getCurrentTerm(), prevLogIndex, prevLogTerm, entries, storage.getCommitIndex());

        if (success) {
            nextIndex.put(follower.getId(), storage.getLastLogIndex() + 1);
            matchIndex.put(follower.getId(), storage.getLastLogIndex());
            updateCommitIndex();
        } else {
            nextIndex.put(follower.getId(), Math.max(1, nextIndex.getOrDefault(follower.getId(), 1L) - 1));
        }
    }

    private void updateCommitIndex() {
        long lastLogIndex = storage.getLastLogIndex();
        for (long n = storage.getCommitIndex() + 1; n <= lastLogIndex; n++) {
            if (storage.getLogEntry(n).getTerm() == state.getCurrentTerm()) {
                int replicationCount = 1; // Count self
                for (long matchIdx : matchIndex.values()) {
                    if (matchIdx >= n) {
                        replicationCount++;
                    }
                }
                if (replicationCount > peers.size() / 2) {
                    storage.setCommitIndex(n);
                    applyCommittedEntries();
                }
            }
        }
    }

    public boolean appendEntries(String leaderId, long term, long prevLogIndex, long prevLogTerm,
                                 List<LogEntry> entries, long leaderCommit) {
        rwLock.writeLock().lock();
        try {
            if (term < state.getCurrentTerm()) {
                return false;
            }

            if (term > state.getCurrentTerm()) {
                state.setCurrentTerm(term);
                state.becomeFollower(term);
                votedFor = null;
            }

            resetElectionTimer();

            if (storage.getLastLogIndex() < prevLogIndex ||
                    (prevLogIndex > 0 && storage.getLogEntry(prevLogIndex).getTerm() != prevLogTerm)) {
                return false;
            }

            for (int i = 0; i < entries.size(); i++) {
                long index = prevLogIndex + 1 + i;
                if (storage.getLastLogIndex() >= index) {
                    if (storage.getLogEntry(index).getTerm() != entries.get(i).getTerm()) {
                        // Delete conflicting entries and all that follow
                        storage.deleteLogEntriesFrom(index);
                    } else {
                        continue;
                    }
                }
                storage.appendLogEntry(entries.get(i));
            }

            if (leaderCommit > storage.getCommitIndex()) {
                storage.setCommitIndex(Math.min(leaderCommit, storage.getLastLogIndex()));
                applyCommittedEntries();
            }

            return true;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void applyCommittedEntries() {
        long lastApplied = storage.getLastApplied();
        long commitIndex = storage.getCommitIndex();
        while (lastApplied < commitIndex) {
            lastApplied++;
            LogEntry entry = storage.getLogEntry(lastApplied);
            applyToStateMachine(entry);
        }
        storage.setLastApplied(lastApplied);
    }

    // the leader commits the operation to its state machine.
    private void applyToStateMachine(LogEntry entry) {
        storage.updateKeyValue(entry.getKey(), entry.getValue());
    }

    private void resetElectionTimer() {
        if (electionTimer != null) {
            electionTimer.cancel(false);
        }

        long timeout = 150 + (long) (Math.random() * 150);
        electionTimer = scheduler.schedule(this::startElection, timeout, TimeUnit.MILLISECONDS);
    }

    public CompletableFuture<Boolean> proposeEntry(String key, String value) {
        if (state.getCurrentRole() != RaftState.Role.LEADER) {
            return CompletableFuture.completedFuture(false);
        }

        LogEntry entry = LogEntry.newBuilder()
                .setTerm(state.getCurrentTerm())
                .setKey(key)
                .setValue(value)
                .build();

        storage.appendLogEntry(entry);
        return replicateEntry(storage.getLastLogIndex());
    }

    private CompletableFuture<Boolean> replicateEntry(long index) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        for (NodeInfo peer : peers.values()) {
            sendAppendEntries(peer);
        }
        updateCommitIndex();
        future.complete(true);
        return future;
    }

    private void becomeLeader() {
        state.becomeLeader();
        long lastLogIndex = storage.getLastLogIndex();
        peers.keySet().forEach(peerId -> {
            nextIndex.put(peerId, lastLogIndex + 1);
            matchIndex.put(peerId, 0L);
        });
    }

    public long getCurrentTerm() {
        return state.getCurrentTerm();
    }

    private void setCurrentTerm(long term) {
        state.setCurrentTerm(term);
    }

    public RaftState getState() {
        return this.state;
    }


    public String getVotedFor() {
        return this.votedFor;
    }

    public void shutdown() {
        scheduler.shutdown();
        workerPool.shutdown();
        storage.close();
        grpcChannels.values().forEach(ManagedChannel::shutdown);
    }
}

class NodeInfo {
    private String id;
    private String host;
    private int port;

    public NodeInfo(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public String getId() { return id; }
    public String getHost() { return host; }
    public int getPort() { return port; }

}