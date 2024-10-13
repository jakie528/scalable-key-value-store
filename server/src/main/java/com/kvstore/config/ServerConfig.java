package com.kvstore.config;

import java.util.List;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Arrays;

public class ServerConfig {
    private String nodeId;
    private List<String> peers;
    private String storagePath;

    public ServerConfig(String configPath) throws IOException {
        Properties props = new Properties();
        props.load(new FileInputStream(configPath));

        this.nodeId = props.getProperty("node.id");
        this.peers = Arrays.asList(props.getProperty("peers").split(","));
        this.storagePath = props.getProperty("storage.path");
    }

    public String getNodeId() {
        return nodeId;
    }

    public List<String> getPeers() {
        return peers;
    }

    public String getStoragePath() {
        return storagePath;
    }
}