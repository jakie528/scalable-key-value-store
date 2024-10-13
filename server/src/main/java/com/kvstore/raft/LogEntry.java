package com.kvstore.raft;

public class LogEntry {
    private final long term;
    private final String key;
    private final String value;

    public LogEntry(long term, String key, String value) {
        this.term = term;
        this.key = key;
        this.value = value;
    }

    public long getTerm() {
        return term;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}