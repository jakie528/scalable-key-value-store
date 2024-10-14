package com.kvstore.raft;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.kvstore.storage.SQLiteStorage;

public class RaftNodeTest {
    private RaftNode raftNode;
    private List<String> peerIds;
    private SQLiteStorage mockStorage;

    @Before
    public void setUp() {
        mockStorage = mock(SQLiteStorage.class);
        peerIds = Arrays.asList("node2", "node3", "node4");
        raftNode = new RaftNode("node1", peerIds, mockStorage);
    }

    @Test
    public void testInitialState() {
        assertEquals(RaftState.Role.FOLLOWER, raftNode.getState().getCurrentRole());
        assertEquals(0, raftNode.getCurrentTerm());
        assertNull(raftNode.getVotedFor());
    }

    @Test
    public void testStartElection() {
        when(mockStorage.getLastLogIndex()).thenReturn(0L);
        when(mockStorage.getLastLogTerm()).thenReturn(0L);

        raftNode.startElection();
        assertEquals(RaftState.Role.CANDIDATE, raftNode.getState().getCurrentRole());
        assertEquals(1, raftNode.getCurrentTerm());
        assertEquals("node1", raftNode.getVotedFor());
    }

    @Test
    public void testAsyncStartElection() throws Exception {
        when(mockStorage.getLastLogIndex()).thenReturn(0L);
        when(mockStorage.getLastLogTerm()).thenReturn(0L);

        CompletableFuture<Void> electionFuture = CompletableFuture.runAsync(() -> raftNode.startElection());
        electionFuture.get(5, TimeUnit.SECONDS);

        RaftState.Role currentRole = raftNode.getState().getCurrentRole();
        assertTrue("Role should be either CANDIDATE or LEADER after starting election",
                currentRole == RaftState.Role.CANDIDATE || currentRole == RaftState.Role.LEADER);
        assertEquals("Term should be incremented to 1 after starting election", 1, raftNode.getCurrentTerm());
        assertEquals("Node should vote for itself", "node1", raftNode.getVotedFor());
    }

    @Test
    public void testProposeEntry() throws Exception {
        raftNode.getState().becomeLeader();
        when(mockStorage.getLastLogIndex()).thenReturn(0L);
        when(mockStorage.appendLogEntry(any(LogEntry.class))).thenReturn(1L);
        when(mockStorage.getCommitIndex()).thenReturn(0L);

        CompletableFuture<Boolean> result = raftNode.proposeEntry("testKey", "testValue");
        assertTrue(result.get(5, TimeUnit.SECONDS));

        verify(mockStorage).appendLogEntry(any(LogEntry.class));
//        verify(mockStorage).updateKeyValue("testKey", "testValue");
    }

    @Test
    public void testAppendEntries() {
        when(mockStorage.getLastLogIndex()).thenReturn(0L);
        when(mockStorage.getLogEntry(anyLong())).thenReturn(null);

        LogEntry entry = new LogEntry(1, "testKey", "testValue");
        List<LogEntry> entries = Arrays.asList(entry);
        boolean result = raftNode.appendEntries("leader", 1, 0, 0, entries, 0);

        assertTrue(result);
        verify(mockStorage).appendLogEntry(any(LogEntry.class));
//        verify(mockStorage).updateKeyValue("testKey", "testValue");
    }

    // Add more tests as needed...
}