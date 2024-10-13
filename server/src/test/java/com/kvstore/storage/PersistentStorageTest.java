package com.kvstore.storage;

import com.kvstore.raft.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PersistentStorageTest {

    private PersistentStorage persistentStorage;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        persistentStorage = new PersistentStorage(tempDir.toString());
    }

    @Test
    void testAppendAndGetLogEntry() {
        LogEntry entry = new LogEntry(1, "key1", "value1");
        persistentStorage.appendLogEntry(entry);
        assertEquals(entry, persistentStorage.getLogEntry(1));
    }

    @Test
    void testGetLastLogIndex() {
        assertEquals(0, persistentStorage.getLastLogIndex());
        persistentStorage.appendLogEntry(new LogEntry(1, "key1", "value1"));
        assertEquals(1, persistentStorage.getLastLogIndex());
    }

    @Test
    void testGetLastLogTerm() {
        assertEquals(0, persistentStorage.getLastLogTerm());
        persistentStorage.appendLogEntry(new LogEntry(1, "key1", "value1"));
        persistentStorage.appendLogEntry(new LogEntry(2, "key2", "value2"));
        assertEquals(2, persistentStorage.getLastLogTerm());
    }

    @Test
    void testPersistenceAcrossRestarts() {
        persistentStorage.appendLogEntry(new LogEntry(1, "key1", "value1"));
        persistentStorage.appendLogEntry(new LogEntry(2, "key2", "value2"));

        // Simulate a restart by creating a new instance
        PersistentStorage newStorage = new PersistentStorage(tempDir.toString());

        assertEquals(2, newStorage.getLastLogIndex());
        assertEquals(2, newStorage.getLastLogTerm());
        assertEquals("value1", newStorage.getLogEntry(1).getValue());
        assertEquals("value2", newStorage.getLogEntry(2).getValue());
    }

    @Test
    void testGetNonExistentLogEntry() {
        assertNull(persistentStorage.getLogEntry(1));
    }
}