#ifndef KVCLIENT_H
#define KVCLIENT_H

#include <string>
#include <memory>
#include "kvstore.grpc.pb.h"
#include <grpcpp/grpcpp.h>

class KVClient {
public:
    KVClient(const std::string& server_address);

    ~KVClient();
    int kv739_init(const std::string& config_file);

    int kv739_shutdown();
    int kv739_get(const std::string& key, std::string& value);

    int kv739_put(const std::string& key, const std::string& value, std::string& old_value);
    int kv739_die(const std::string& server_name, bool clean);

private:
    std::unique_ptr<kvstore::KVStoreService::Stub> stub_;
    std::shared_ptr<grpc::Channel> channel_;
    std::string server_address_;
};

#endif // KVCLIENT_H