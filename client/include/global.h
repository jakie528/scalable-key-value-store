#include "Common.h"

class KvStore; 
class NodeManager;

namespace kvstore_globals {
    extern KvStore* kvStore;
    extern NodeManager* g_node_manager;
    extern uint32_t g_value_maxlen;
    extern uint32_t g_key_maxlen;
    extern uint32_t g_num_servers;
}

void initializeKvStore(const std::string& db_filename);
void cleanupGlobals();