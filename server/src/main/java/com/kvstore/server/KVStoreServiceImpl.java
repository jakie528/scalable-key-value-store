package com.kvstore.server;

import com.kvstore.grpc.*;
import com.kvstore.raft.RaftNode;
import com.kvstore.storage.KVStorage;
import com.kvstore.grpc.KVStoreServiceGrpc;
import io.grpc.stub.StreamObserver;

public class KVStoreServiceImpl extends KVStoreServiceGrpc.KVStoreServiceImplBase {
    private final KVStorage storage;
    private final RaftNode raftNode;

    public KVStoreServiceImpl(KVStorage storage, RaftNode raftNode) {
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
        if (raftNode.isLeader()) {
            String key = request.getKey();
            String value = request.getValue();
            boolean success = storage.put(key, value);
            PutResponse response = PutResponse.newBuilder()
                    .setSuccess(success)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } else {
            // Forward to leader or return an error
            PutResponse response = PutResponse.newBuilder()
                    .setSuccess(false)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}