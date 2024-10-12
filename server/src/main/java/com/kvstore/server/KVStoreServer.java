package com.kvstore.server;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import com.kvstore.raft.RaftNode;
import com.kvstore.storage.KVStorage;

import java.io.IOException;

public class KVStoreServer {
    private final int port;
    private final Server server;

    public KVStoreServer(int port) {
        this.port = port;
        KVStorage storage = new KVStorage();
        RaftNode raftNode = new RaftNode(); // Initialize with appropriate parameters
        KVStoreServiceImpl service = new KVStoreServiceImpl(storage, raftNode);
        this.server = ServerBuilder.forPort(port)
                .addService(service)
                .build();
    }

    public void start() throws IOException {
        server.start();
        System.out.println("Server started, listening on " + port);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("Shutting down gRPC server");
                KVStoreServer.this.stop();
                System.err.println("Server shut down");
            }
        });
    }

    public void stop() {
        if (server != null) {
            server.shutdown();
        }
    }

    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = 8080; // Configure port as needed
        final KVStoreServer server = new KVStoreServer(port);
        server.start();
        server.blockUntilShutdown();
    }
}