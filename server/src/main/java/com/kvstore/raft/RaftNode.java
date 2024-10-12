package com.kvstore.raft;

public class RaftNode {
    private boolean isLeader;

    public RaftNode() {
        // Initialize Raft node
        this.isLeader = false;
    }

    public boolean isLeader() {
        return isLeader;
    }

    // Add other Raft-related methods here
}