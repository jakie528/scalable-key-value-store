package com.kvstore.raft;

public class RaftState {
    public enum Role {
        FOLLOWER, CANDIDATE, LEADER
    }

    private Role currentRole;
    private int currentTerm;
    private String votedFor;

    public RaftState() {
        this.currentRole = Role.FOLLOWER;
        this.currentTerm = 0;
        this.votedFor = null;
    }

    public Role getCurrentRole() {
        return currentRole;
    }

    public int getCurrentTerm() {
        return currentTerm;
    }

    public boolean isLeader() {
        return currentRole == Role.LEADER;
    }

    public void becomeFollower(int term) {
        currentRole = Role.FOLLOWER;
        currentTerm = term;
        votedFor = null;
    }

    public void becomeCandidate() {
        currentRole = Role.CANDIDATE;
        currentTerm++;
        votedFor = null;
    }

    public void becomeLeader() {
        currentRole = Role.LEADER;
    }

    // Add getters and setters as needed
}