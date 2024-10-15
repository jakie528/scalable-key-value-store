#ifndef KVSTORE_SRC_TRANSPORT_RPC_CLIENT_H_
#define KVSTORE_SRC_TRANSPORT_RPC_CLIENT_H_

#include "kvstore.grpc.pb.h"
#include "kvstore.pb.h"
#include <iostream>
#include <memory>
#include <string>
#include <grpcpp/grpcpp.h>
#include <thread>

using grpc::Channel;
using grpc::ClientAsyncResponseReader;
using grpc::ClientContext;
using grpc::CompletionQueue;
using grpc::Status;
using kvstore::KVStoreService;
using kvstore::GetRequest;
using kvstore::GetResponse;
using kvstore::PutRequest;
using kvstore::PutResponse;

using std::string;

struct AsyncClientCall {
    // Container for the data we expect from the server.
    void *reply;
    // Context for the client. It could be used to convey extra information to
    // the server and/or tweak certain RPC behaviors.
    ClientContext context;
    // Storage for the status of the RPC upon completion.
    Status status;
    std::unique_ptr<ClientAsyncResponseReader<void>> response_reader;
};

class KvstoreRPCClientStub {
public:
    KvstoreRPCClientStub(std::shared_ptr<Channel> channel)
        : stub_(KVStoreService::NewStub(channel)) {}

    // Synchronous Get and Put RPC methods
    Status Get(ClientContext* context, const GetRequest& request, GetResponse* response) {
        return stub_->Get(context, request, response);
    }

    Status Put(ClientContext* context, const PutRequest& request, PutResponse* response) {
        return stub_->Put(context, request, response);
    }

    // Async stubs
    std::unique_ptr<KVStoreService::Stub> stub_;
    CompletionQueue cq_;  // Create a completion queue for each server
};

class KvstoreRPCClient {
public:
    KvstoreRPCClient(string hosts);
    
    static void AsyncCompleteRpc(KvstoreRPCClient* s, uint64_t node_id = 0);
    
    // Synchronous request methods
    void sendGetRequest(const std::string& key, std::string& value, uint64_t node_id = 0);
    void sendPutRequest(const std::string& key, const std::string& value, bool& success, uint64_t node_id = 0);

    // Asynchronous request methods
    void sendRequestAsync(const std::string& key, const std::string& value, uint64_t node_id = 0, bool is_put = true);
    void sendRequestDone(void* request, void* response);

private:
    KvstoreRPCClientStub** _servers;
    std::thread** _threads;
};

#endif // KVSTORE_SRC_TRANSPORT_RPC_CLIENT_H_
