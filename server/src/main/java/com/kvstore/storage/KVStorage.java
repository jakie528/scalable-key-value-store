package com.kvstore.storage;

import java.util.concurrent.ConcurrentHashMap;
import com.kvstore.raft.LogEntry;

public class KVStorage {
    private final ConcurrentHashMap<String, String> store;
    private final PersistentStorage persistentStorage;

    public KVStorage(String dataDir) {
        this.store = new ConcurrentHashMap<>();
        this.persistentStorage = new PersistentStorage(dataDir);
        recoverFromLog();
    }

    public String get(String key) {
        return store.get(key);
    }

    public void put(String key, String value, long term) {
        LogEntry entry = new LogEntry(term, key, value);
        persistentStorage.appendLogEntry(entry);
        store.put(key, value);
    }

    public long getLastLogIndex() {
        return persistentStorage.getLastLogIndex();
    }

    public long getLastLogTerm() {
        return persistentStorage.getLastLogTerm();
    }

    void recoverFromLog() {
        long lastIndex = persistentStorage.getLastLogIndex();
        for (long i = 1; i <= lastIndex; i++) {
            LogEntry entry = persistentStorage.getLogEntry(i);
            if (entry != null) {
                store.put(entry.getKey(), entry.getValue());
            }
        }
    }

//    protected abstract PersistentStorage createPersistentStorage(String dataDir);
}