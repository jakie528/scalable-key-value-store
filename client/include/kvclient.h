#ifndef KVCLIENT_H
#define KVCLIENT_H

#ifdef __cplusplus
extern "C" {
#endif

int kv739_init(char *config_file);
int kv739_shutdown(void);
int kv739_get(char *key, char *value);
int kv739_put(char *key, char *value, char *old_value);
int kv739_die(char *server_name, int clean);

#ifdef __cplusplus
}
#endif

#endif // KVCLIENT_H
