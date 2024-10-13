package com.kvstore.storage;

import java.io.*;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import com.kvstore.raft.LogEntry;

//for RAFT log and state machine

public class PersistentStorage {
    private final String dataDir;
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private long lastIndex = 0;
    private Map<Long, LogEntry> logEntries = new HashMap<>();

    public PersistentStorage(String dataDir) {
        this.dataDir = dataDir;
        File dir = new File(dataDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        loadLogEntries();
    }

    public void appendLogEntry(LogEntry entry) {
        rwLock.writeLock().lock();
        try {
            lastIndex++;
            logEntries.put(lastIndex, entry);
            saveLogEntry(lastIndex, entry);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public LogEntry getLogEntry(long index) {
        rwLock.readLock().lock();
        try {
            return logEntries.get(index);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public long getLastLogIndex() {
        return lastIndex;
    }

    public long getLastLogTerm() {
        return lastIndex > 0 ? logEntries.get(lastIndex).getTerm() : 0;
    }

    private void saveLogEntry(long index, LogEntry entry) {
        File file = new File(dataDir, "log_" + index);
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(entry);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadLogEntries() {
        File dir = new File(dataDir);
        File[] files = dir.listFiles((d, name) -> name.startsWith("log_"));
        if (files != null) {
            for (File file : files) {
                try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                    LogEntry entry = (LogEntry) ois.readObject();
                    long index = Long.parseLong(file.getName().substring(4));
                    logEntries.put(index, entry);
                    lastIndex = Math.max(lastIndex, index);
                } catch (IOException | ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}