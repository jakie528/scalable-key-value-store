package com.kvstore.raft;

import com.kvstore.storage.KVStorage;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class RaftNode {
    private String nodeId;
    private List<String> peers;
    private KVStorage storage;
    private RaftState state;
    private ScheduledExecutorService executor;
    private Random random;
    private AtomicBoolean running;

    private List<LogEntry> log;
    private int commitIndex;
    private int lastApplied;
    private Map<String, Integer> nextIndex;
    private Map<String, Integer> matchIndex;

    public RaftNode(String nodeId, List<String> peers, KVStorage storage) {
        this.nodeId = nodeId;
        this.peers = peers;
        this.storage = storage;
        this.state = new RaftState();
        this.executor = Executors.newScheduledThreadPool(2);
        this.random = new Random();
        this.running = new AtomicBoolean(false);

        this.log = new ArrayList<>();
        this.commitIndex = 0;
        this.lastApplied = 0;
        this.nextIndex = new HashMap<>();
        this.matchIndex = new HashMap<>();
    }

    public void start() {
        running.set(true);
        resetElectionTimeout();
        executor.scheduleAtFixedRate(this::heartbeat, 0, 100, TimeUnit.MILLISECONDS);
    }

    private void resetElectionTimeout() {
        if (running.get()) {
            int timeout = 300 + random.nextInt(150);
            executor.schedule(this::startElection, timeout, TimeUnit.MILLISECONDS);
        }
    }

    private void startElection() {
        if (state.getCurrentRole() != RaftState.Role.LEADER) {
            state.becomeCandidate();
            requestVotes();
        }
        resetElectionTimeout();
    }

    private void requestVotes() {
        int votesReceived = 1; // Vote for self
        for (String peer : peers) {
            // Send RequestVote RPC to peer
            boolean voteGranted = sendRequestVote(peer);
            if (voteGranted) {
                votesReceived++;
            }
        }
        if (votesReceived > (peers.size() + 1) / 2) {
            state.becomeLeader();
            // Initialize leader state
            for (String peer : peers) {
                nextIndex.put(peer, log.size());
                matchIndex.put(peer, 0);
            }
        }
    }

    private boolean sendRequestVote(String peer) {
        // Implement RequestVote RPC
        // Return true if vote granted, false otherwise
        return false; // Placeholder
    }

    private void heartbeat() {
        if (state.getCurrentRole() == RaftState.Role.LEADER) {
            for (String peer : peers) {
                appendEntries(peer);
            }
        }
    }

    private void appendEntries(String peer) {
        // Implement AppendEntries RPC
        // Send log entries or heartbeat
    }

    public boolean isLeader() {
        return state.getCurrentRole() == RaftState.Role.LEADER;
    }

    public CompletableFuture<Boolean> proposeEntry(String key, String value) {
        if (!isLeader()) {
            return CompletableFuture.completedFuture(false);
        }
        LogEntry entry = new LogEntry(state.getCurrentTerm(), key, value);
        log.add(entry);
        return replicateEntry(log.size() - 1);
    }

    private CompletableFuture<Boolean> replicateEntry(int index) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        int replicationCount = 1; // Count self
        for (String peer : peers) {
            boolean success = sendAppendEntries(peer, index);
            if (success) {
                replicationCount++;
            }
        }
        boolean majorityReplicated = replicationCount > (peers.size() + 1) / 2;
        if (majorityReplicated) {
            commitIndex = Math.max(commitIndex, index);
            applyCommittedEntries();
        }
        future.complete(majorityReplicated);
        return future;
    }

    private boolean sendAppendEntries(String peer, int index) {
        // Implement AppendEntries RPC for log replication
        // Return true if entry was successfully replicated
        return false; // Placeholder
    }

    private void applyCommittedEntries() {
        while (lastApplied < commitIndex) {
            lastApplied++;
            LogEntry entry = log.get(lastApplied);
            storage.put(entry.getKey(), entry.getValue());
        }
    }
}