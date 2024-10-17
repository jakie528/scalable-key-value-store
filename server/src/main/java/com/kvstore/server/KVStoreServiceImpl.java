package com.kvstore.server;

import com.kvstore.grpc.*;
import com.kvstore.raft.RaftNode;
import com.kvstore.storage.SQLiteStorage;
import io.grpc.stub.StreamObserver;
import java.util.concurrent.CompletableFuture;

public class KVStoreServiceImpl extends KVStoreServiceGrpc.KVStoreServiceImplBase {
    private final SQLiteStorage storage;
    private final RaftNode raftNode;

    public KVStoreServiceImpl(SQLiteStorage storage, RaftNode raftNode) {
        this.storage = storage;
        this.raftNode = raftNode;
    }

    @Override
    public void get(GetRequest request, StreamObserver<GetResponse> responseObserver) {
        String key = request.getKey();
        String value = storage.get(key);
        GetResponse response = GetResponse.newBuilder()
                .setValue(value != null ? value : "")
                .setFound(value != null)
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void put(PutRequest request, StreamObserver<PutResponse> responseObserver) {
        String key = request.getKey();
        String value = request.getValue();
        CompletableFuture<Boolean> future = raftNode.proposeEntry(key, value);
        future.thenAccept(success -> {
            PutResponse response = PutResponse.newBuilder()
                    .setSuccess(success)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        });
    }

}