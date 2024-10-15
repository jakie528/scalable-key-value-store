#include "KvStore.h"

#include <include/MemoryAllocator.h>
#include <sqlite3.h>
#include <stdio.h>
#include <string.h>
#include "include/Common.h"
#include "include/Message.h"

KvStore::KvStore() {}
int KvStore::EnableWALMode() {
    char *err_msg = 0;
    const char *sql = "PRAGMA journal_mode = WAL;";

    int rc = sqlite3_exec(db, sql, 0, 0, &err_msg);
    if (rc != SQLITE_OK) {
        LOG_ERROR("Failed to enable WAL mode: %s", err_msg);
        sqlite3_free(err_msg);
        return rc;
    }
    LOG_INFO("WAL mode enabled successfully!");
    return SQLITE_OK;
}

int KvStore::CreateTable() {
    char *errMsg = 0;
    // SQL statement to create a key-value store table
    const char *sql = "CREATE TABLE IF NOT EXISTS kv_store ("
                      "KEY TEXT PRIMARY KEY, " 
                      "VALUE TEXT NOT NULL);";

    // Execute SQL statement to create the table
    int rc = sqlite3_exec(db, sql, 0, 0, &errMsg);
    if (rc != SQLITE_OK) {
        LOG_ERROR("Cannot create table: %s", errMsg);
        sqlite3_free(errMsg);
    } else {
        LOG_INFO("Table created successfully!");
    }
}

int KvStore::Init(const char *filename) {
    // Set SQLite to serialized mode
    filename_ = filename;
    sqlite3_config(SQLITE_CONFIG_SERIALIZED);
    int rc = sqlite3_open(filename, &db);
    if (rc) {
        LOG_ERROR("Cannot open database: %s", sqlite3_errmsg(db));
        return rc;
    } else {
        LOG_INFO("Opened database successfully!");
    }
    EnableWALMode();
    CreateTable();
}

void KvStore::Close() {
     // Close the database connection
    int rc = sqlite3_close(db);
    if (rc != SQLITE_OK) {
        LOG_ERROR("Cannot close database: %s", sqlite3_errmsg(db));
    }
}

void KvStore::Clear() {
    if (std::remove(filename_) != 0) {
        throw std::runtime_error("Failed to delete file: " + std::string(filename_));
    }
}

// kv739_get: Retrieve the value corresponding to the key
// retrieve the value corresponding to the key. If the key is present, it should
// return 0 and store the value in the provided string. The string must be at
// least 1 byte larger than the maximum allowed value. If the key is not
// present, it should return 1. If there is a failure, it should return -1.
int KvStore::Get(const char *key, char **value, uint32_t &size) {
    sqlite3_stmt *stmt;
    const char *sql = "SELECT value FROM kv_store WHERE key = ?";

    if (sqlite3_prepare_v2(db, sql, -1, &stmt, NULL) != SQLITE_OK) {
        return -1;  // SQL error
    }

    sqlite3_bind_text(stmt, 1, key, -1, SQLITE_STATIC);

    int rc = sqlite3_step(stmt);
    if (rc == SQLITE_ROW) {
        const char *val = (const char *)sqlite3_column_text(stmt, 0);
        size = strlen(val) + 1;
        *value = (char *)MemoryAllocator::Alloc(size);
        strcpy(*value, val); 
        sqlite3_finalize(stmt);
        return 0;  // Key found
    } else if (rc == SQLITE_DONE) {
        sqlite3_finalize(stmt);
        return 1;  // Key not found
    } else {
        const char *errMsg = sqlite3_errmsg(db);
        LOG_ERROR("sqlite3_step for Get failed: %s", errMsg); 
        sqlite3_finalize(stmt);
        return -1;  // Failure
    }
}

// kv739_put: Perform get on the current value and then store the new value
// Perform a get operation on the current value into old_value and then store
// the specified value. Should return 0 on success if there is an old value, 1
// on success if there was no old value, and -1 on failure. The old_value
// parameter must be at least one byte larger than the maximum value size
int KvStore::Put(const char *key, const char *value, char **old_value,
                 uint32_t &size) {
    sqlite3_stmt *stmt;
    sqlite3_exec(db, "BEGIN TRANSACTION;", NULL, NULL, NULL);

    // get the old value (if exists)
    const char *get_sql = "SELECT value FROM kv_store WHERE key = ?";
    int rc = sqlite3_prepare_v2(db, get_sql, -1, &stmt, NULL); 
    if (rc != SQLITE_OK) {
        const char *errMsg = sqlite3_errmsg(db);
        LOG_ERROR("prepare stmt failed: %s", errMsg);
        return -1;
    }
    sqlite3_bind_text(stmt, 1, key, -1, SQLITE_STATIC);

    rc = sqlite3_step(stmt);
    int ret;
    if (rc == SQLITE_ROW) {
        const char *old_val = (const char *)sqlite3_column_text(stmt, 0);
        size = strlen(old_val) + 1;
        *old_value = (char *)MemoryAllocator::Alloc(size);
        strcpy(*old_value, old_val);  
        ret = 0;                     // Key exists
    } else if (rc == SQLITE_DONE) {
        ret = 1;  // Key does not exist 
    } else {
        const char *errMsg = sqlite3_errmsg(db);
        LOG_ERROR("sqlite3_step failed: %s", errMsg); 
        sqlite3_finalize(stmt);
        return -1;  // SQL error
    }
    sqlite3_finalize(stmt);

    // insert/update the new value
    const char *put_sql =
        "INSERT OR REPLACE INTO kv_store (key, value) VALUES (?, ?)";
    if (sqlite3_prepare_v2(db, put_sql, -1, &stmt, NULL) != SQLITE_OK) {
        return -1;  // SQL error
    }
    sqlite3_bind_text(stmt, 1, key, -1, SQLITE_STATIC);
    sqlite3_bind_text(stmt, 2, value, -1, SQLITE_STATIC);

    rc = sqlite3_step(stmt);
    if (rc != SQLITE_DONE) {
        const char *errMsg = sqlite3_errmsg(db);
        LOG_ERROR("sqlite3_step failed: %s", errMsg);  
        sqlite3_finalize(stmt);
        sqlite3_exec(db, "ROLLBACK;", NULL, NULL, NULL);
        return -1;  // Failure
    }

    sqlite3_finalize(stmt);
    sqlite3_exec(db, "COMMIT;", NULL, NULL, NULL);
    return ret;
}