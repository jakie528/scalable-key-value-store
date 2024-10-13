package com.kvstore.storage;

import java.util.concurrent.ConcurrentHashMap;
import java.io.*;

public class KVStorage {
    private ConcurrentHashMap<String, String> store;
    private String persistentStorePath;

    public KVStorage(String persistentStorePath) {
        this.persistentStorePath = persistentStorePath;
        this.store = new ConcurrentHashMap<>();
        loadFromDisk();
    }

    public String get(String key) {
        return store.get(key);
    }

    public boolean put(String key, String value) {
        store.put(key, value);
        return persistToDisk();
    }

    private boolean persistToDisk() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(persistentStorePath))) {
            oos.writeObject(store);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void loadFromDisk() {
        File file = new File(persistentStorePath);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                store = (ConcurrentHashMap<String, String>) ois.readObject();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}