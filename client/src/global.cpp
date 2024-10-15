#include "Global.h"
#include "Common.h"
#include "db/NodeManager.h"
#include "db/KvStore.h"

namespace kvstore_globals {
    KvStore* kvStore = nullptr;
    NodeManager* g_node_manager = nullptr;
    uint32_t g_value_maxlen = 2048;
    uint32_t g_key_maxlen = 128;
    uint32_t g_num_servers = 1;
}

void initializeKvStore(const std::string& db_filename) {
    kvstore_globals::kvStore = new KvStore();
    kvstore_globals::kvStore->Init(db_filename.c_str());
}

void cleanupGlobals() {
    if (kvstore_globals::kvStore) {
        kvstore_globals::kvStore->Close();
        delete kvstore_globals::kvStore;
        kvstore_globals::kvStore = nullptr;
    }
    if (kvstore_globals::g_node_manager) {
        delete kvstore_globals::g_node_manager;
        kvstore_globals::g_node_manager = nullptr;
    }
}