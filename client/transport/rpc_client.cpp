#include "rpc_client.h"
#include "include/Common.h"
#include "include/Global.h"
#include "include/Message.h"
#include "kvstore.grpc.pb.h"

using std::string;
using grpc::Channel;
using grpc::ClientContext;
using grpc::Status;
using kvstore::KVStoreService;
using kvstore::GetRequest;
using kvstore::GetResponse;
using kvstore::PutRequest;
using kvstore::PutResponse;

// hosts need to be separated by ";"
// e.g., "10.0.0.1;10.0.0.2;10.0.0.3"
KvstoreRPCClient::KvstoreRPCClient(string hosts) {
    g_num_servers = 1;
    _servers = new KvstoreRPCClientStub *[g_num_servers];
    _threads = new std::thread *[g_num_servers];
    int port = 50051;

    std::vector<std::string> host_addrs;
    std::stringstream ss(hosts);
    std::string token;

    while (getline(ss, token, ';')) {
        host_addrs.push_back(token);
    }

    // get server names
    uint32_t node_id = 0;
    while (node_id < g_num_servers) {
        string url = host_addrs[node_id] + ":" + std::to_string(port);
        LOG_INFO("[RPC] init rpc client to - %u at %s", node_id, url.c_str());
        _servers[node_id] = new KvstoreRPCClientStub(
            grpc::CreateChannel(url, grpc::InsecureChannelCredentials()));
        // spawn a reader thread for each server to indefinitely read completion queue
        _threads[node_id] = new std::thread(AsyncCompleteRpc, this, node_id);
        node_id++;
    }
    LOG_INFO("[RPC] rpc client is initialized!");
}

void KvstoreRPCClient::AsyncCompleteRpc(KvstoreRPCClient *s, uint64_t node_id) {
    void* got_tag;
    bool ok = false;
    // Block until the next result is available in the completion queue "cq".
    while (true) {
        s->_servers[node_id]->cq_.Next(&got_tag, &ok);
        auto call = static_cast<AsyncClientCall*>(got_tag);
        if (!call->status.ok()) {
            printf("[REQ] client received response fail from node (%d): (%d) %s\n", node_id,
                   call->status.error_code(), call->status.error_message().c_str());
            continue;
        }
        s->sendRequestDone(call->request, call->reply);
        // Once we're complete, deallocate the call object.
        delete call;
    }
}

void KvstoreRPCClient::sendGetRequest(const std::string &key, std::string &value, uint64_t node_id) {
    // Create GetRequest and GetResponse
    GetRequest request;
    request.set_key(key);
    GetResponse response;

    ClientContext context;
    Status status = _servers[node_id]->stub_->Get(&context, request, &response);

    if (status.ok() && response.found()) {
        value = response.value();
    } else {
        if (!status.ok()) {
            printf("[REQ] client sendGetRequest fail: (%d) %s\n",
                   status.error_code(), status.error_message().c_str());
        }
        value = "";
    }
}

void KvstoreRPCClient::sendPutRequest(const std::string &key, const std::string &value, bool &success, uint64_t node_id) {
    // Create PutRequest and PutResponse
    PutRequest request;
    request.set_key(key);
    request.set_value(value);
    PutResponse response;

    ClientContext context;
    Status status = _servers[node_id]->stub_->Put(&context, request, &response);

    if (status.ok()) {
        success = response.success();
    } else {
        printf("[REQ] client sendPutRequest fail: (%d) %s\n",
               status.error_code(), status.error_message().c_str());
        success = false;
    }
}

void KvstoreRPCClient::sendRequestAsync(const std::string &key, const std::string &value, uint64_t node_id, bool is_put) {
    AsyncClientCall* call = new AsyncClientCall;

    if (is_put) {
        // Create PutRequest for async call
        PutRequest request;
        request.set_key(key);
        request.set_value(value);
        PutResponse response;

        call->response_reader = _servers[node_id]->stub_->PrepareAsyncPut(
            &call->context, request, &_servers[node_id]->cq_);
        call->reply = &response;
    } else {
        // Create GetRequest for async call
        GetRequest request;
        request.set_key(key);
        GetResponse response;

        call->response_reader = _servers[node_id]->stub_->PrepareAsyncGet(
            &call->context, request, &_servers[node_id]->cq_);
        call->reply = &response;
    }

    call->response_reader->StartCall();
    call->response_reader->Finish(call->reply, &(call->status), (void*)call);
}

void KvstoreRPCClient::sendRequestDone(void* request, void* response) {
    // Cast to appropriate request/response types
    auto* get_response = dynamic_cast<GetResponse*>(response);
    auto* put_response = dynamic_cast<PutResponse*>(response);

    if (get_response != nullptr) {
        if (get_response->found()) {
            std::cout << "GET response received: Value = " << get_response->value() << std::endl;
        } else {
            std::cout << "GET response: Key not found." << std::endl;
        }
    } else if (put_response != nullptr) {
        if (put_response->success()) {
            std::cout << "PUT response: Success." << std::endl;
        } else {
            std::cout << "PUT response: Failure." << std::endl;
        }
    } else {
        std::cout << "Unexpected response." << std::endl;
    }
}
