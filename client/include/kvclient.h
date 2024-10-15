#ifndef KVCLIENT_H
#define KVCLIENT_H

#include <string>
#include <memory>
#include <vector>
#include "kvstore.grpc.pb.h"
#include <grpcpp/grpcpp.h>
#include <thread>
#include "transport/rpc_client.h"

class KVClient {
public:
    KVClient(const std::vector<std::string>& server_addresses);
    ~KVClient();

    int kv739_init(const std::string& config_file);
    int kv739_shutdown();
    int kv739_get(const std::string& key, std::string& value);
    int kv739_put(const std::string& key, const std::string& value, std::string& old_value);
    int kv739_die(const std::string& server_name, bool clean);

private:
    std::vector<std::unique_ptr<kvstore::KVStoreService::Stub>> stubs_;
    std::vector<std::shared_ptr<grpc::Channel>> channels_;
    std::vector<std::string> server_addresses_;
    KvstoreRPCClient rpc_client_;
    std::vector<std::thread> rpc_threads_;
};

#endif // KVCLIENT_H