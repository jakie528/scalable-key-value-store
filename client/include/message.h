#ifndef KVSTORE_SRC_COMMON_MESSAGE_H_
#define KVSTORE_SRC_COMMON_MESSAGE_H_

#define MSG(fd, type, msg, ...) {                                \
  fprintf(fd, "[%s] ", type);                                    \
  fprintf(fd, msg);                                              \
  fprintf(fd, __VA_ARGS__);                                      \
  fprintf(fd, " (%s:%d) \n", __FILE__, __LINE__); }
#define LOG_INFO(...) { MSG(stdout, "INFO", "", __VA_ARGS__); }
#ifndef NDEBUG
#define LOG_DEBUG(...) { MSG(stdout, "DEBUG", "", __VA_ARGS__); }
#else
#define LOG_DEBUG(...) {};
#endif
#define LOG_ERROR(...) { MSG(stderr, "ERROR", "", __VA_ARGS__); exit(1); }
#ifndef NDEBUG
#define M_ASSERT(cond, ...) {                                     \
  if (!(cond)) {                                                  \
        MSG(stderr, "ERROR", "Assertion Failure: ", __VA_ARGS__); \
        assert(false);                                            \
  }}
#else
#define M_ASSERT(cond, ...) {};
#endif

#endif //KVSTORE_SRC_COMMON_MESSAGE_H_