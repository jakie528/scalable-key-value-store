#ifndef KVSTORE_SRC_COMMON_MEMORYALLOCATOR_H_
#define KVSTORE_SRC_COMMON_MEMORYALLOCATOR_H_

#include <jemalloc/jemalloc.h>

class MemoryAllocator {
 public:
  static void *Alloc(size_t size, size_t alignment = 0) {
    if (alignment == 0) {
      return malloc(size);  // Use jemalloc's malloc
    }
    return aligned_alloc(alignment, size);  // Use jemalloc's aligned alloc
  };
  static void Dealloc(void *ptr) { free(ptr); };  // Use jemalloc's free
};


// class MemoryAllocator {
//  public:
//   static void *Alloc(size_t size, size_t alignment = 0) {
//     if (alignment == 0) {
//       return je_malloc(size);  // Use jemalloc's malloc
//     }
//     return je_aligned_alloc(alignment, size);  // Use jemalloc's aligned alloc
//   };
//   static void Dealloc(void *ptr) { je_free(ptr); };  // Use jemalloc's free
// };

#endif //KVSTORE_SRC_COMMON_MEMORYALLOCATOR_H_