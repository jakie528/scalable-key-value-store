package com.kvstore.raft;

import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.Arrays;
import java.util.List;
import com.kvstore.storage.KVStorage;

@Ignore
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
    @Test
    void testAsyncStartElection() throws Exception {
        CompletableFuture<Void> electionFuture = CompletableFuture.runAsync(() -> raftNode.startElection());

        // Wait for the election to complete (with a timeout)
        electionFuture.get(5, TimeUnit.SECONDS);

        // Now check the results
        RaftState.Role currentRole = raftNode.getState().getCurrentRole();
        assertTrue(currentRole == RaftState.Role.CANDIDATE || currentRole == RaftState.Role.LEADER,
                "Role should be either CANDIDATE or LEADER after starting election");
        assertEquals(1, raftNode.getCurrentTerm(), "Term should be incremented to 1 after starting election");
        assertEquals("node1", raftNode.getVotedFor(), "Node should vote for itself");
    }


    // Add more tests...
}