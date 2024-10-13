package com.kvstore.raft;

import java.util.concurrent.*;
import java.util.*;
import com.kvstore.storage.KVStorage;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RaftNode {
    private final String nodeId;
    private final ConcurrentHashMap<String, NodeInfo> peers;
    private final KVStorage storage;
    private final RaftState state;
    private final ScheduledExecutorService scheduler;
    private final ExecutorService workerPool;

    // Optimized data structures for large clusters
    private final ConcurrentHashMap<String, Long> nextIndex;
    private final ConcurrentHashMap<String, Long> matchIndex;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

    private List<LogEntry> log;
    private long commitIndex;
    private long lastApplied;
//    private long currentTerm;
    private String votedFor;
    private ScheduledFuture<?> electionTimer;

    public RaftNode(String nodeId, List<String> peerIds, KVStorage storage) {
        this.nodeId = nodeId;
        this.peers = new ConcurrentHashMap<>();
        peerIds.forEach(id -> this.peers.put(id, new NodeInfo(id)));
        this.storage = storage;
        this.state = new RaftState();
        this.scheduler = Executors.newScheduledThreadPool(2);
        this.workerPool = Executors.newWorkStealingPool();
        this.nextIndex = new ConcurrentHashMap<>();
        this.matchIndex = new ConcurrentHashMap<>();
        this.log = new ArrayList<>();
        this.commitIndex = 0;
        this.lastApplied = 0;
//        this.currentTerm = 0;
        this.votedFor = null;
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

    public void startElection() {
        state.becomeCandidate();  // increment the term
        votedFor = nodeId; // Vote for itself

        long lastLogIndex = storage.getLastLogIndex();
        long lastLogTerm = storage.getLastLogTerm();

        //candidate requests votes from other nodes
        CompletableFuture<Boolean>[] voteFutures = peers.values().stream()
                .map(peer -> CompletableFuture.supplyAsync(() -> requestVote(peer, lastLogIndex, lastLogTerm), workerPool))
                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(voteFutures).thenAccept(v -> {
//            long voteCount = Arrays.stream(voteFutures)
//                    .map(future -> {
//                        try {
//                            return (Boolean) future.get();
//                        } catch (Exception e) {
//                            return false;
//                        }
//                    })
//                    .filter(Boolean::valueOf)
//                    .count();
            long voteCount = Arrays.stream(voteFutures)
                    .map(CompletableFuture::join)
                    .filter(Boolean::booleanValue)
                    .count() + 1; // Add 1 for self-vote

            //If it receives votes from the majority, it becomes the leader.
            if (voteCount > peers.size() / 2) {
                becomeLeader();
            }
        });
    }

    public RaftState getState() {
        return this.state;
    }

    private void heartbeatTask() {
        if (state.getCurrentRole() == RaftState.Role.LEADER) {
            peers.values().forEach(peer ->
                    workerPool.submit(() -> sendAppendEntries(peer))
            );
        }
    }
  //  leader's behavior to send AppendEntries
  private void sendHeartbeat() {
      if (state.getCurrentRole() == RaftState.Role.LEADER) {
          for (NodeInfo follower : peers.values()) {
              sendAppendEntries(follower);
          }
      }
  }

    public long getCurrentTerm() {
        return state.getCurrentTerm();
    }

    private void setCurrentTerm(long term) {
        state.setCurrentTerm(term);
    }

//    public long getCurrentTerm() {
//        return this.state.getCurrentTerm();
//    }

    public String getVotedFor() {
        return this.votedFor;
    }

    public boolean requestVote(NodeInfo peer, long lastLogIndex, long lastLogTerm) {
        // Implement RPC call to peer for vote request
        return false; // Placeholder
    }

    private void sendAppendEntries(NodeInfo follower) {
        long nextIdx = nextIndex.getOrDefault(follower.getId(), 1L);
        List<LogEntry> entries = log.subList((int) nextIdx, log.size());
        long prevLogIndex = nextIdx - 1;
        long prevLogTerm = prevLogIndex >= 0 && prevLogIndex < log.size() ? log.get((int) prevLogIndex).getTerm() : 0;

        boolean success = appendEntries(follower.getId(), state.getCurrentTerm(), prevLogIndex, prevLogTerm, entries, commitIndex);

        if (success) {
            nextIndex.put(follower.getId(), (long) log.size());
            matchIndex.put(follower.getId(), (long) (log.size() - 1));
            updateCommitIndex();
        } else {
            nextIndex.put(follower.getId(), Math.max(1, nextIndex.getOrDefault(follower.getId(), 1L) - 1));
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

            if (log.size() <= prevLogIndex || (prevLogIndex >= 0 && log.get((int) prevLogIndex).getTerm() != prevLogTerm)) {
                return false;
            }

            for (int i = 0; i < entries.size(); i++) {
                long index = prevLogIndex + 1 + i;
                if (log.size() > index) {
                    if (log.get((int) index).getTerm() != entries.get(i).getTerm()) {
                        // Delete conflicting entries and all that follow
                        while (log.size() > index) {
                            log.remove(log.size() - 1);
                        }
                    } else {
                        continue;
                    }
                }
                log.add(entries.get(i));
            }

            if (leaderCommit > commitIndex) {
                commitIndex = Math.min(leaderCommit, log.size() - 1);
                applyCommittedEntries();
            }

            return true;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private void resetElectionTimer() {
        // Cancel the existing election timer if it exists
        if (electionTimer != null) {
            electionTimer.cancel(false);
        }

        // Schedule a new election timer
        long timeout = 150 + (long) (Math.random() * 150); // Random timeout between 150-300ms
        electionTimer = scheduler.schedule(this::startElection, timeout, TimeUnit.MILLISECONDS);
    }

    private void updateCommitIndex() {
        for (long n = commitIndex + 1; n < log.size(); n++) {
            if (log.get((int) n).getTerm() == state.getCurrentTerm()) {
                int replicationCount = 1; // Count self
                for (long matchIdx : matchIndex.values()) {
                    if (matchIdx >= n) {
                        replicationCount++;
                    }
                }
                if (replicationCount > peers.size() / 2) {
                    commitIndex = n;
                    applyCommittedEntries();
                }
            }
        }
    }

    private void applyCommittedEntries() {
        while (lastApplied < commitIndex) {
            lastApplied++;
            LogEntry entry = log.get((int) lastApplied);
            applyToStateMachine(entry);
        }
    }

    private void applyToStateMachine(LogEntry entry) {
        storage.put(entry.getKey(), entry.getValue(), entry.getTerm());
    }

    // Add this method to handle client requests
    public CompletableFuture<Boolean> proposeEntry(String key, String value) {
        if (state.getCurrentRole() != RaftState.Role.LEADER) {
            return CompletableFuture.completedFuture(false);
        }

        LogEntry entry = new LogEntry(state.getCurrentTerm(), key, value);
        log.add(entry);
        return replicateEntry(log.size() - 1);
    }

    private CompletableFuture<Boolean> replicateEntry(int index) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        int replicationCount = 1; // Count self
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

}

class NodeInfo {
    private String id;

    public NodeInfo(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}



