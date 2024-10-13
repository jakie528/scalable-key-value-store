cmake_minimum_required(VERSION 3.10)
project(KVStoreClient)

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include(FetchContent)

# Download and build gRPC and Protobuf using FetchContent
FetchContent_Declare(
    protobuf
    GIT_REPOSITORY https://github.com/protocolbuffers/protobuf.git
    GIT_TAG v3.21.7
)
FetchContent_Declare(
    grpc
    GIT_REPOSITORY https://github.com/grpc/grpc.git
    GIT_TAG v1.53.0
)

# Make available
FetchContent_MakeAvailable(protobuf grpc)

# Include directories
include_directories(
    ${PROJECT_SOURCE_DIR}/src
    ${PROJECT_SOURCE_DIR}/proto
    ${PROJECT_SOURCE_DIR}/server/src/main/java/com/kvstore/server
)

# Add source files
set(SOURCES
    src/kvclient.cpp
    src/kvclientimpl.cpp
    src/test/test_kv739_die.cpp
)

# Link libraries
add_executable(kvstore_client ${SOURCES})
target_link_libraries(kvstore_client
    gRPC::grpc++
    gRPC::grpc
    protobuf::libprotobuf
    pthread
)

# Add custom commands to generate .h and .cc from .proto files
set(PROTO_FILES proto/kv_store.proto)

foreach(proto_file ${PROTO_FILES})
    get_filename_component(proto_name ${proto_file} NAME_WE)
    set(proto_src "${CMAKE_CURRENT_BINARY_DIR}/${proto_name}.pb.cc")
    set(proto_hdr "${CMAKE_CURRENT_BINARY_DIR}/${proto_name}.pb.h")
    set(grpc_src "${CMAKE_CURRENT_BINARY_DIR}/${proto_name}.grpc.pb.cc")
    set(grpc_hdr "${CMAKE_CURRENT_BINARY_DIR}/${proto_name}.grpc.pb.h")

    add_custom_command(
        OUTPUT ${proto_src} ${proto_hdr} ${grpc_src} ${grpc_hdr}
        COMMAND protobuf::protoc
        ARGS --grpc_out=${CMAKE_CURRENT_BINARY_DIR}
             --cpp_out=${CMAKE_CURRENT_BINARY_DIR}
             --plugin=protoc-gen-grpc=$<TARGET_FILE:gRPC::grpc_cpp_plugin>
             -I ${CMAKE_CURRENT_SOURCE_DIR}/proto
             ${proto_file}
        DEPENDS ${proto_file}
    )

    list(APPEND PROTO_SRCS ${proto_src} ${grpc_src})
    list(APPEND PROTO_HDRS ${proto_hdr} ${grpc_hdr})
endforeach()

# Add the generated sources to the target
add_executable(kvstore_client ${SOURCES} ${PROTO_SRCS})
target_include_directories(kvstore_client PRIVATE ${CMAKE_CURRENT_BINARY_DIR})

# Link libraries again including protobuf and gRPC
target_link_libraries(kvstore_client
    gRPC::grpc++
    gRPC::grpc
    protobuf::libprotobuf
    pthread
)

# Install target
install(TARGETS kvstore_client DESTINATION bin) 
install(FILES ${PROTO_HDRS} DESTINATION include) 
install(FILES ${PROJECT_SOURCE_DIR}/src/kvclient.h DESTINATION include) 
install(FILES ${PROJECT_SOURCE_DIR}/src/kvclientimpl.h DESTINATION include)