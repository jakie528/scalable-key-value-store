# Distributed under the OSI-approved BSD 3-Clause License.  See accompanying
# file Copyright.txt or https://cmake.org/licensing for details.

cmake_minimum_required(VERSION 3.5)

file(MAKE_DIRECTORY
  "/Users/Patron/Desktop/Fall'24/distributed systems/Projects/CS739-P2/scalable-key-value-store/server/src/main/java/com/kvstore/client/build/_deps/grpc-src"
  "/Users/Patron/Desktop/Fall'24/distributed systems/Projects/CS739-P2/scalable-key-value-store/server/src/main/java/com/kvstore/client/build/_deps/grpc-build"
  "/Users/Patron/Desktop/Fall'24/distributed systems/Projects/CS739-P2/scalable-key-value-store/server/src/main/java/com/kvstore/client/build/_deps/grpc-subbuild/grpc-populate-prefix"
  "/Users/Patron/Desktop/Fall'24/distributed systems/Projects/CS739-P2/scalable-key-value-store/server/src/main/java/com/kvstore/client/build/_deps/grpc-subbuild/grpc-populate-prefix/tmp"
  "/Users/Patron/Desktop/Fall'24/distributed systems/Projects/CS739-P2/scalable-key-value-store/server/src/main/java/com/kvstore/client/build/_deps/grpc-subbuild/grpc-populate-prefix/src/grpc-populate-stamp"
  "/Users/Patron/Desktop/Fall'24/distributed systems/Projects/CS739-P2/scalable-key-value-store/server/src/main/java/com/kvstore/client/build/_deps/grpc-subbuild/grpc-populate-prefix/src"
  "/Users/Patron/Desktop/Fall'24/distributed systems/Projects/CS739-P2/scalable-key-value-store/server/src/main/java/com/kvstore/client/build/_deps/grpc-subbuild/grpc-populate-prefix/src/grpc-populate-stamp"
)

set(configSubDirs )
foreach(subDir IN LISTS configSubDirs)
    file(MAKE_DIRECTORY "/Users/Patron/Desktop/Fall'24/distributed systems/Projects/CS739-P2/scalable-key-value-store/server/src/main/java/com/kvstore/client/build/_deps/grpc-subbuild/grpc-populate-prefix/src/grpc-populate-stamp/${subDir}")
endforeach()
if(cfgdir)
  file(MAKE_DIRECTORY "/Users/Patron/Desktop/Fall'24/distributed systems/Projects/CS739-P2/scalable-key-value-store/server/src/main/java/com/kvstore/client/build/_deps/grpc-subbuild/grpc-populate-prefix/src/grpc-populate-stamp${cfgdir}") # cfgdir has leading slash
endif()
