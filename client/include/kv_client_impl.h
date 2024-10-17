#ifndef KV_CLIENT_IMPL_H
#define KV_CLIENT_IMPL_H

#include <grpcpp/grpcpp.h>
#include "kv_store.grpc.pb.h"
#include <vector>
#include <memory>
#include <string>

class KVClientImpl {
public:
    static KVClientImpl& getInstance();

    int init(const char* config_file);
    int shutdown();
    int get(const char* key, char* value);
    int put(const char* key, const char* value, char* old_value);
    int die(const char* server_name, int clean);

private:
    KVClientImpl() = default;
    ~KVClientImpl() = default;
    KVClientImpl(const KVClientImpl&) = delete;
    KVClientImpl& operator=(const KVClientImpl&) = delete;

    std::vector<std::unique_ptr<kvstore::KVStoreService::Stub>> stubs_;

    bool validateKey(const std::string& key);
    bool validateValue(const std::string& value);
};

#endif // KV_CLIENT_IMPL_H
