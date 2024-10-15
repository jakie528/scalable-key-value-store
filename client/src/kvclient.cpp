#include "include/KVClient.h"
#include <memory>
#include <fstream>
#include <sstream>
#include <vector>

std::shared_ptr<KVClient> kv_client = nullptr;

std::vector<std::string> parseConfigFile(const char *config_file) {
  std::vector<std::string> server_list;
  std::ifstream infile(config_file);
  std::string line;
  while (std::getline(infile, line)) {
    server_list.push_back(line);
  }
  return server_list;
}

int kv739_init(char *config_file) {
  try {
    std::vector<std::string> server_list = parseConfigFile(config_file);
    if (server_list.empty()) {
      return -1;
    }
    kv_client = std::make_shared<KVClient>(server_list);
  } catch (...) {
    return -1;
  }

  return 0;
}

int kv739_shutdown(void) {
  try {
    kv_client->Shutdown();
    kv_client = nullptr;
  } catch (...) {
    return -1;
  }
  return 0;
}

int kv739_get(char *key, char *value) {
  if (key == NULL || value == NULL) {
    return -1;
  }

  std::string modern_key(key);
  try {
    std::string modern_val = kv_client->SendGetRequest(modern_key);
    if (modern_val.empty()) {
      return 1;
    }
    uint64_t val_len = modern_val.length();
    uint64_t num_bytes = val_len > 2047 ? 2047 : val_len;
    modern_val.copy(value, num_bytes);
    value[num_bytes] = '\0';
  } catch (...) {
    return -1;
  }

  return 0;
}

int kv739_put(char *key, char *value, char *old_value) {
  if (key == NULL || value == NULL || old_value == NULL) {
    return -1;
  }

  std::string modern_key(key);
  std::string modern_value(value);

  try {
    std::string modern_old_value = kv_client->SendPutRequest(modern_key, modern_value);
    if (modern_old_value.empty()) {
      return 1;
    }
    uint64_t val_len = modern_old_value.length();
    uint64_t num_bytes = val_len > 2047 ? 2047 : val_len;
    modern_old_value.copy(old_value, num_bytes);
    old_value[num_bytes] = '\0';
  } catch (...) {
    return -1;
  }

  return 0;
}

int kv739_die(char *server_name, int clean) {
  if (server_name == NULL) {
    return -1;
  }

  std::string modern_server_name(server_name);
  try {
    kv_client->SendDieRequest(modern_server_name, clean);
  } catch (...) {
    return -1;
  }

  return 0;
}