#include <iostream>
#include <fstream>
#include <sstream>
#include <vector>
#include <string>
#include <cassert>
#include "include/kvclient.h"

std::vector<std::string> load_servers_from_config(const char* config_file) {
    std::vector<std::string> server_list;
    std::ifstream infile(config_file);

    if (!infile.is_open()) {
        std::cerr << "Error opening config file: " << config_file << std::endl;
        return server_list;
    }

    std::string line;
    while (std::getline(infile, line)) {
        if (!line.empty()) {
            server_list.push_back(line);
        }
    }

    infile.close();
    return server_list;
}

void test_availability(const char* config_file) {
    std::cout << "Running Availability Tests..." << std::endl;

    // Step 1: Test with all servers available
    assert(kv739_init(config_file) == 0);
    std::cout << "All servers are up, performing operations..." << std::endl;

    // Perform a basic put operation
    char value[2049];
    int result = kv739_put("test_key1", "test_value1", value);
    assert(result == 0 || result == 1);  // 0 if new key, 1 if overwriting existing key

    // Perform a basic get operation
    result = kv739_get("test_key1", value);
    assert(result == 0);  // Value should be retrievable
    std::cout << "Put and Get operations successful with all servers up." << std::endl;

    kv739_shutdown();  // Shutdown gracefully when all servers are operational

    // Step 2: Test with a subset of servers down
    assert(kv739_init(config_file) == 0);
    std::cout << "Simulating server failure..." << std::endl;

    // Simulate server failure (this function would be part of your Raft implementation)
    kv739_die("localhost:50051", 1);  // Example: Kill one of the Raft servers

    // Step 3: Perform operations with some servers down
    result = kv739_put("test_key2", "test_value2", value);
    assert(result == 0 || result == 1);  // Should still succeed due to Raft consensus

    result = kv739_get("test_key2", value);
    assert(result == 0);  // Should still succeed, Raft ensures availability

    std::cout << "Put and Get operations successful with some servers down." << std::endl;

    kv739_shutdown();  // Shutdown gracefully after failure test

    // Step 4: Check behavior with a majority of servers down
    assert(kv739_init(config_file) == 0);
    std::cout << "Simulating majority server failure..." << std::endl;

    for (int i = 0; i < 49; ++i) {
        std::string server = "localhost:" + std::to_string(50050 + i);  // Assuming your servers start from port 50050
        kv739_die(server.c_str(), 1);  // Simulate failure of 49 servers
    }

    result = kv739_put("test_key3", "test_value3", value);
    assert(result != 0);  // Should fail because Raft cannot achieve consensus with a majority down

    result = kv739_get("test_key2", value);  // Testing previously inserted key
    assert(result != 0);  // Should fail because Raft cannot achieve consensus with majority down

    kv739_shutdown();
    std::cout << "Availability Tests Passed with majority failure handled correctly." << std::endl;
}

int main() {
    std::string config_file;

    std::cout << "Enter the configuration file name: ";
    std::cin >> config_file;

    // Run availability tests
    test_availability(config_file.c_str());

    return 0;
}

