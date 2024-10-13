#include "rpc_client.h"
#include "common/Common.h"
#include "common/Global.h"
#include "common/Message.h"
#include <iostream>

using std::string;

KvstoreRPCClient::KvstoreRPCClient(string hosts) {
    std::vector<std::string> host_addrs;
    std::stringstream ss(hosts);
    std::string token;

    while (getline(ss, token, ';')) {
        host_addrs.push_back(token);
    }

    g_num_servers = host_addrs.size();

    _servers = new KvstoreRPCClientStub * [g_num_servers];
    _threads = new std::thread * [g_num_servers];
    int port = 50051;

    uint32_t node_id = 0;
    for (const std::string& host : host_addrs) {
        string url = host + ":" + std::to_string(port);
        LOG_INFO("[RPC] Initializing RPC client to - %u at %s", node_id, url.c_str());

        _servers[node_id] = new KvstoreRPCClientStub(
            grpc::CreateChannel(url, grpc::InsecureChannelCredentials()));

        _threads[node_id] = new std::thread(&KvstoreRPCClient::AsyncCompleteRpc, this, node_id);
        node_id++;
    }
    LOG_INFO("[RPC] RPC client is initialized with %u servers!", g_num_servers);
}

void KvstoreRPCClient::AsyncCompleteRpc(KvstoreRPCClient *s, uint64_t node_id) {
    void* got_tag;
    bool ok = false;

    while (true) {
        bool next_status = s->_servers[node_id]->cq_.Next(&got_tag, &ok);
        if (!next_status) {
            break;
        }

        auto call = static_cast<AsyncClientCall*>(got_tag);

        if (!call->status.ok()) {
            std::cerr << "[REQ] Client received response failure from node (" 
                      << node_id << "): (" << call->status.error_code() << ") "
                      << call->status.error_message().c_str() << std::endl;
            continue;
        }

        s->sendRequestDone(call->request, call->reply);

        delete call;
    }
}

void KvstoreRPCClient::sendRequest(KvstoreRequest &request, KvstoreResponse &response, uint64_t node_id) {
    ClientContext context;
    context.set_deadline(std::chrono::system_clock::now() + std::chrono::milliseconds(500)); 

    Status status = _servers[node_id]->contactRemote(&context, request, &response);

    if (!status.ok()) {
        std::cerr << "[REQ] Client sendRequest failed: (" 
                  << status.error_code() << ") " << status.error_message() << std::endl;

        throw std::runtime_error("Failed to send request to server.");
    }

    LOG_INFO("[RPC] Client successfully sent request to node %u", node_id);
}

void KvstoreRPCClient::sendRequestAsync(KvstoreRequest &request, KvstoreResponse &response, uint64_t node_id) {
    AsyncClientCall* call = new AsyncClientCall;
    
    context.set_deadline(std::chrono::system_clock::now() + std::chrono::milliseconds(500));  
    call->response_reader = _servers[node_id]->stub_->PrepareAsynccontactRemote(
        &call->context, request, &_servers[node_id]->cq_);

    call->request = &request;
    call->reply = &response;
    call->response_reader->StartCall();
    call->response_reader->Finish(call->reply, &(call->status), (void*)call);
}

void KvstoreRPCClient::sendRequestDone(KvstoreRequest *request, KvstoreResponse *response) {
    switch (response->request_type()) {
        case KvstoreResponse::GET_REQ:
            LOG_INFO("[RPC] GET request completed successfully.");
            break;
        case KvstoreResponse::PUT_REQ:
            LOG_INFO("[RPC] PUT request completed successfully.");
            break;
        case KvstoreResponse::STOP_REQ:
            LOG_INFO("[RPC] STOP request completed successfully.");
            break;
        default:
            std::cerr << "Unexpected request type: " << response->request_type() << std::endl;
            break;
    }
}

KvstoreRPCClient::~KvstoreRPCClient() {
    for (uint32_t i = 0; i < g_num_servers; i++) {
        _servers[i]->cq_.Shutdown();
        _threads[i]->join();
        delete _threads[i];
        delete _servers[i];
    }
    delete[] _threads;
    delete[] _servers;

    LOG_INFO("[RPC] RPC client resources cleaned up.");
}
