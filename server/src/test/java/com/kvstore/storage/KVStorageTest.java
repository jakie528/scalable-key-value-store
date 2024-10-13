package com.kvstore.storage;

import com.kvstore.raft.LogEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.lang.reflect.Field;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class KVStorageTest {

    private KVStorage kvStorage;
    private PersistentStorage mockPersistentStorage;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() throws Exception {
        mockPersistentStorage = mock(PersistentStorage.class);
        kvStorage = new KVStorage(tempDir.toString());

        // Use reflection to replace the real PersistentStorage with our mock
        Field persistentStorageField = KVStorage.class.getDeclaredField("persistentStorage");
        persistentStorageField.setAccessible(true);
        persistentStorageField.set(kvStorage, mockPersistentStorage);
    }

    @Test
    void testPutAndGet() {
        kvStorage.put("key1", "value1", 1);
        assertEquals("value1", kvStorage.get("key1"));
    }

    @Test
    void testGetNonExistentKey() {
        assertNull(kvStorage.get("nonexistent"));
    }

    @Test
    void testGetLastLogIndex() {
        when(mockPersistentStorage.getLastLogIndex()).thenReturn(5L);
        assertEquals(5L, kvStorage.getLastLogIndex());
    }

    @Test
    void testGetLastLogTerm() {
        when(mockPersistentStorage.getLastLogTerm()).thenReturn(3L);
        assertEquals(3L, kvStorage.getLastLogTerm());
    }

    @Test
    void testRecoverFromLog() throws Exception {
        // Create a new KVStorage instance for this test
        KVStorage testStorage = new KVStorage(tempDir.toString());

        // Use reflection to replace the real PersistentStorage with our mock
        Field persistentStorageField = KVStorage.class.getDeclaredField("persistentStorage");
        persistentStorageField.setAccessible(true);
        persistentStorageField.set(testStorage, mockPersistentStorage);

        when(mockPersistentStorage.getLastLogIndex()).thenReturn(2L);
        when(mockPersistentStorage.getLogEntry(1L)).thenReturn(new LogEntry(1, "key1", "value1"));
        when(mockPersistentStorage.getLogEntry(2L)).thenReturn(new LogEntry(2, "key2", "value2"));

        // Trigger recovery
        testStorage.recoverFromLog();

        assertEquals("value1", testStorage.get("key1"));
        assertEquals("value2", testStorage.get("key2"));
    }
}