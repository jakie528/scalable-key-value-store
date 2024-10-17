#include "kvclient.h"
#include <iostream>
#include <string>

int main(int argc, char* argv[]) {
    if (argc != 2) {
        std::cerr << "Usage: " << argv[0] << " <config_file>" << std::endl;
        return 1;
    }

    // Initialize the client
    if (kv739_init(argv[1]) != 0) {
        std::cerr << "Failed to initialize client" << std::endl;
        return 1;
    }

    std::string command, key, value;
    char retrieved_value[2049];
    char old_value[2049];

    while (true) {
        std::cout << "Enter command (get/put/quit): ";
        std::cin >> command;

        if (command == "quit") {
            break;
        } else if (command == "get") {
            std::cout << "Enter key: ";
            std::cin >> key;
            int result = kv739_get((char*)key.c_str(), retrieved_value);
            if (result == 0) {
                std::cout << "Value: " << retrieved_value << std::endl;
            } else if (result == 1) {
                std::cout << "Key not found" << std::endl;
            } else {
                std::cout << "Error occurred" << std::endl;
            }
        } else if (command == "put") {
            std::cout << "Enter key: ";
            std::cin >> key;
            std::cout << "Enter value: ";
            std::cin >> value;
            int result = kv739_put((char*)key.c_str(), (char*)value.c_str(), old_value);
            if (result == 0 || result == 1) {
                std::cout << "Put successful" << std::endl;
            } else {
                std::cout << "Put failed with code " << result << std::endl;
            }
        } else {
            std::cout << "Unknown command" << std::endl;
        }
    }

    kv739_shutdown();
    return 0;
}
