package com.kvstore.server;

import com.kvstore.grpc.GetRequest;
import com.kvstore.grpc.GetResponse;
import com.kvstore.grpc.PutRequest;
import com.kvstore.grpc.PutResponse;
import com.kvstore.raft.RaftNode;
import com.kvstore.storage.KVStorage;
import io.grpc.stub.StreamObserver;
import org.junit.Before;
import org.junit.Test;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CompletableFuture;

public class KVStoreServiceImplTest {
    private KVStoreServiceImpl kvStoreService;
    private KVStorage mockStorage;
    private RaftNode mockRaftNode;

    @Before
    public void setUp() {
        mockStorage = mock(KVStorage.class);
        mockRaftNode = mock(RaftNode.class);
        kvStoreService = new KVStoreServiceImpl(mockStorage, mockRaftNode);
    }

    @Test
    public void testGet() {
        when(mockStorage.get("testKey")).thenReturn("testValue");

        GetRequest request = GetRequest.newBuilder().setKey("testKey").build();
        StreamObserver<GetResponse> responseObserver = mock(StreamObserver.class);

        kvStoreService.get(request, responseObserver);

        verify(responseObserver).onNext(argThat(response ->
                response.getValue().equals("testValue") && response.getFound()
        ));
        verify(responseObserver).onCompleted();
    }

    @Test
    public void testPut() {
        when(mockRaftNode.proposeEntry("testKey", "testValue"))
                .thenReturn(CompletableFuture.completedFuture(true));

        PutRequest request = PutRequest.newBuilder()
                .setKey("testKey")
                .setValue("testValue")
                .build();
        StreamObserver<PutResponse> responseObserver = mock(StreamObserver.class);

        kvStoreService.put(request, responseObserver);

        verify(responseObserver).onNext(argThat(response -> response.getSuccess()));
        verify(responseObserver).onCompleted();
    }
}