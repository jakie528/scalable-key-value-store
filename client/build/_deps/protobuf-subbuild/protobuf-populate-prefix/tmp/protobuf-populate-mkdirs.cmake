# Distributed under the OSI-approved BSD 3-Clause License.  See accompanying
# file Copyright.txt or https://cmake.org/licensing for details.

cmake_minimum_required(VERSION 3.5)

file(MAKE_DIRECTORY
  "/Users/Patron/Desktop/Fall'24/distributed systems/Projects/CS739-P2/scalable-key-value-store/server/src/main/java/com/kvstore/client/build/_deps/protobuf-src"
  "/Users/Patron/Desktop/Fall'24/distributed systems/Projects/CS739-P2/scalable-key-value-store/server/src/main/java/com/kvstore/client/build/_deps/protobuf-build"
  "/Users/Patron/Desktop/Fall'24/distributed systems/Projects/CS739-P2/scalable-key-value-store/server/src/main/java/com/kvstore/client/build/_deps/protobuf-subbuild/protobuf-populate-prefix"
  "/Users/Patron/Desktop/Fall'24/distributed systems/Projects/CS739-P2/scalable-key-value-store/server/src/main/java/com/kvstore/client/build/_deps/protobuf-subbuild/protobuf-populate-prefix/tmp"
  "/Users/Patron/Desktop/Fall'24/distributed systems/Projects/CS739-P2/scalable-key-value-store/server/src/main/java/com/kvstore/client/build/_deps/protobuf-subbuild/protobuf-populate-prefix/src/protobuf-populate-stamp"
  "/Users/Patron/Desktop/Fall'24/distributed systems/Projects/CS739-P2/scalable-key-value-store/server/src/main/java/com/kvstore/client/build/_deps/protobuf-subbuild/protobuf-populate-prefix/src"
  "/Users/Patron/Desktop/Fall'24/distributed systems/Projects/CS739-P2/scalable-key-value-store/server/src/main/java/com/kvstore/client/build/_deps/protobuf-subbuild/protobuf-populate-prefix/src/protobuf-populate-stamp"
)

set(configSubDirs )
foreach(subDir IN LISTS configSubDirs)
    file(MAKE_DIRECTORY "/Users/Patron/Desktop/Fall'24/distributed systems/Projects/CS739-P2/scalable-key-value-store/server/src/main/java/com/kvstore/client/build/_deps/protobuf-subbuild/protobuf-populate-prefix/src/protobuf-populate-stamp/${subDir}")
endforeach()
if(cfgdir)
  file(MAKE_DIRECTORY "/Users/Patron/Desktop/Fall'24/distributed systems/Projects/CS739-P2/scalable-key-value-store/server/src/main/java/com/kvstore/client/build/_deps/protobuf-subbuild/protobuf-populate-prefix/src/protobuf-populate-stamp${cfgdir}") # cfgdir has leading slash
endif()
