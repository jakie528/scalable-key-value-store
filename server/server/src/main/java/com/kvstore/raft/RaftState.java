package com.kvstore.raft;

public class RaftState {
    public enum Role {
        FOLLOWER, CANDIDATE, LEADER
    }

    private Role currentRole;
    private long currentTerm;
    private String votedFor;

    public RaftState() {
        this.currentRole = Role.FOLLOWER;
        this.currentTerm = 0;
        this.votedFor = null;
    }

    public Role getCurrentRole() {
        return currentRole;
    }

    public long getCurrentTerm() {
        return currentTerm;
    }

    public boolean isLeader() {
        return currentRole == Role.LEADER;
    }

    public void becomeFollower(long term) {
        currentRole = Role.FOLLOWER;
        currentTerm = term;
        votedFor = null;
    }

    public void becomeCandidate() {
        currentRole = Role.CANDIDATE;
        currentTerm++;
//        votedFor = null;
    }

    public void becomeLeader() {
        currentRole = Role.LEADER;
    }

    public void setCurrentTerm(long term) {
        this.currentTerm = term;
    }


    // Add getters and setters as needed
}