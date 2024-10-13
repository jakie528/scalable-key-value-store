#include "kvclient.hpp"
#include <iostream>

int main() {
    char config_file[] = "config.txt";  

    if (kv739_init(config_file) != 0) {
        std::cerr << "Client initialization failed." << std::endl;
        return -1;
    }

    char server_name[] = "127.0.0.1:50051";  // Replace with the actual server name
    int clean = 1;  // 1 for clean shutdown, 0 for immediate exit

    int result = kv739_die(server_name, clean);
    if (result == 0) {
        std::cout << "Server terminated successfully." << std::endl;
    } else {
        std::cerr << "Failed to terminate server." << std::endl;
    }

    kv739_shutdown();

    return 0;
}
