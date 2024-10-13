#include "kvclient.hpp"
#include <iostream>
#include <stdexcept>
#include <memory>
#include <vector>

std::shared_ptr<KVClient> kv_client = nullptr;
std::vector<std::string> server_list;
std::string current_leader;
std::vector<std::string> read_server_list(const std::string& config_file);

int kv739_init(char *config_file) {
    server_list = read_server_list(config_file);
    for (const std::string& server : server_list) {
        try {
            kv_client = std::make_shared<KVClient>(server);
            if (kv_client->IsLeader()) {  // Synchronous check for leader
                current_leader = server;
                return 0;  // Successfully initialized with the leader
            }
        } catch (const std::exception& e) {
            std::cerr << "Error connecting to server: " << server << " - " << e.what() << std::endl;
            continue;
        }
    }
    return -1;  // Failure to find a leader or connect to any server
}

int kv739_shutdown(void) {
    try {
        if (kv_client) {
            kv_client->Shutdown();  // Synchronous shutdown
            kv_client = nullptr;
        }
    } catch (const std::exception& e) {
        std::cerr << "Error during shutdown: " << e.what() << std::endl;
        return -1;
    }
    return 0;
}

int kv739_die(char *server_name, int clean) {
    try {
        std::string modern_server_name(server_name);
        return kv_client->TerminateServer(modern_server_name, clean);  // Synchronous termination
    } catch (const std::exception& e) {
        std::cerr << "Error during server termination: " << e.what() << std::endl;
        return -1;
    }
}

int kv739_get(char *key, char *value) {
    if (key == NULL || value == NULL) {
        return -1;
    }

    std::string modern_key(key);
    try {
        std::string modern_val = kv_client->SendGetRequest(modern_key);  // Synchronous get request
        if (modern_val.empty()) {
            return 1;  // Key not found
        }
        uint64_t val_len = modern_val.length();
        uint64_t num_bytes = val_len > 2047 ? 2047 : val_len;
        modern_val.copy(value, num_bytes);
        value[num_bytes] = '\0';  // Null-terminate the value string
        return 0;
    } catch (const std::exception& e) {
        std::cerr << "GET request failed: " << e.what() << std::endl;
        return -1;
    }
}

int kv739_put(char *key, char *value, char *old_value) {
    if (key == NULL || value == NULL || old_value == NULL) {
        return -1;
    }

    std::string modern_key(key);
    std::string modern_value(value);
    int retry_count = 3;

    while (retry_count > 0) {
        try {
            std::string modern_old_value = kv_client->SendPutRequest(modern_key, modern_value);  // Synchronous put request

            if (modern_old_value.empty()) {
                return 1;  // No previous value existed
            }

            uint64_t val_len = modern_old_value.length();
            uint64_t num_bytes = val_len > 2047 ? 2047 : val_len;
            modern_old_value.copy(old_value, num_bytes);
            old_value[num_bytes] = '\0';  // Null-terminate the old value string
            return 0;

        } catch (const LeaderRedirectException& e) {
            std::cerr << "Redirected to new leader: " << e.new_leader_address << std::endl;
            current_leader = e.new_leader_address;
            kv_client = std::make_shared<KVClient>(current_leader);  // Retry with the new leader
            retry_count--;
        } catch (const std::exception& e) {
            std::cerr << "PUT request failed: " << e.what() << std::endl;
            return -1;
        }
    }
    return -1;
}
