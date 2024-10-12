package com.kvstore.storage;

import java.util.concurrent.ConcurrentHashMap;

public class KVStorage {
    private final ConcurrentHashMap<String, String> store;

    public KVStorage() {
        this.store = new ConcurrentHashMap<>();
    }

    public String get(String key) {
        return store.get(key);
    }

    public boolean put(String key, String value) {
        store.put(key, value);
        return true;
    }
}