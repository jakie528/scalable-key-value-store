
#include <sqlite3.h>
#include "include/Common.h"

class KvStore {
    public:
      KvStore();
      int Init(const char *filename);
      int Get(const char *key, char **value, uint32_t& size);
      int Put(const char *key, const char *value, char **old_value, uint32_t& size);

      void Close();
      void Clear();
    
    private:
      int EnableWALMode();
      int CreateTable();

      sqlite3 *db;
      const char* filename_;

};