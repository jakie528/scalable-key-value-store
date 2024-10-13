package com.kvstore.raft;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.List;
import com.kvstore.storage.KVStorage;

class RaftNodeTest {
    private RaftNode raftNode;
    private KVStorage mockStorage;
    private List<String> peerIds;

    @BeforeEach
    void setUp() {
        mockStorage = mock(KVStorage.class);

        // List of peer IDs as Strings, matching the RaftNode constructor signature
        List<String> peerIds = Arrays.asList("node2", "node3", "node4");

        // Creating the RaftNode instance
        raftNode = new RaftNode("node1", peerIds, mockStorage);
    }

    @Test
    void testInitialState() {
        assertEquals(RaftState.Role.FOLLOWER, raftNode.getState().getCurrentRole());
        assertEquals(0, raftNode.getCurrentTerm());
        assertNull(raftNode.getVotedFor());
    }

    // TODO: asynchronous start election?
//    @Test
//    void testStartElection() {
//        raftNode.startElection();
//        assertEquals(RaftState.Role.CANDIDATE, raftNode.getState().getCurrentRole());
//        assertEquals(1, raftNode.getCurrentTerm());
//        assertEquals("node1", raftNode.getVotedFor());
//    }


    // Add more tests...
}