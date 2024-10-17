#include "kv_client_impl.h"
#include <fstream>
#include <regex>
#include <cstring>
#include <thread>   
#include <chrono> 

KVClientImpl& KVClientImpl::getInstance() {
    static KVClientImpl instance;
    return instance;
}

bool KVClientImpl::validateKey(const std::string& key) {
    return key.length() <= 128 && 
           std::regex_match(key, std::regex("^[\\x20-\\x5A\\x5C\\x5E-\\x7E]+$"));
}

bool KVClientImpl::validateValue(const std::string& value) {
    return value.length() <= 2048 && 
           std::regex_match(value, std::regex("^[\\x20-\\x5A\\x5C\\x5E-\\x7E]+$"));
}

int KVClientImpl::init(const char* config_file) {
    std::ifstream file(config_file);
    if (!file.is_open()) {
        return -1;
    }

    std::string line;
    while (std::getline(file, line)) {
        auto channel = grpc::CreateChannel(line, grpc::InsecureChannelCredentials());
        stubs_.push_back(kvstore::KVStoreService::NewStub(channel));
    }

    return stubs_.empty() ? -1 : 0;
}

int KVClientImpl::shutdown() {
    stubs_.clear();
    return 0;
}

int KVClientImpl::get(const char* key, char* value) {
    if (!validateKey(key)) {
        return -1;
    }
    kvstore::GetRequest request;
    request.set_key(key);
    kvstore::GetResponse response;

    int retries = 3;
    while (retries > 0) {
        grpc::ClientContext context;
        for (const auto& stub : stubs_) {
            grpc::Status status = stub->Get(&context, request, &response);
            if (status.ok()) {
                if (response.found()) {
                    strncpy(value, response.value().c_str(), 2049);
                    value[2048] = '\0';
                    return 0;
                }
                return 1; // Key not found
            } else if (status.error_code() == grpc::StatusCode::NOT_FOUND) {
                return 1; // Key not found
            } else if (status.error_code() == grpc::StatusCode::UNAVAILABLE) {
                // Node might not be the leader, try next node
                continue;
            }
        }
        retries--;
        std::this_thread::sleep_for(std::chrono::milliseconds(100));
    }
    return -1;
}

int KVClientImpl::put(const char* key, const char* value, char* old_value) {
    if (!validateKey(key) || !validateValue(value)) {
        return -1;
    }

    kvstore::PutRequest request;
    request.set_key(key);
    request.set_value(value);
    kvstore::PutResponse response;
    grpc::ClientContext context;

    for (const auto& stub : stubs_) {
        grpc::Status status = stub->Put(&context, request, &response);
        if (status.ok()) {
            if (response.success()) {
                return old_value[0] = '\0', 1;
            }
	    std::cout<<"Error: " <<status.error_message()<<std::endl <<status.error_details()<<std::endl;
            return -1;
        }
    }

    return -2;
}

int KVClientImpl::die(const char* server_name, int clean) {
    return -1;
}
