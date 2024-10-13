package com.kvstore.raft;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class RaftStateTest {
    private RaftState raftState;

    @Before
    public void setUp() {
        raftState = new RaftState();
    }

    @Test
    public void testInitialState() {
        assertEquals(RaftState.Role.FOLLOWER, raftState.getCurrentRole());
        assertEquals(0, raftState.getCurrentTerm());
    }

    @Test
    public void testBecomeCandidate() {
        raftState.becomeCandidate();
        assertEquals(RaftState.Role.CANDIDATE, raftState.getCurrentRole());
        assertEquals(1, raftState.getCurrentTerm());
    }

    @Test
    public void testBecomeLeader() {
        raftState.becomeLeader();
        assertEquals(RaftState.Role.LEADER, raftState.getCurrentRole());
    }

    @Test
    public void testBecomeFollower() {
        raftState.becomeFollower(5);
        assertEquals(RaftState.Role.FOLLOWER, raftState.getCurrentRole());
        assertEquals(5, raftState.getCurrentTerm());
    }
}