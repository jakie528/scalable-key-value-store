cmake_minimum_required(VERSION 3.10)
project(KVClient739 VERSION 1.0)
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Set the path to Protobuf and gRPC
set(Protobuf_ROOT /usr/local)
set(gRPC_ROOT /usr/local)
set(protobuf_MODULE_COMPATIBLE TRUE)

# Find packages
find_package(Protobuf REQUIRED)
find_package(gRPC CONFIG REQUIRED)

# If Protobuf is still not found, try setting these variables manually
if(NOT Protobuf_FOUND)
    set(Protobuf_INCLUDE_DIRS /usr/local/include)
    set(Protobuf_LIBRARIES /usr/local/lib/libprotobuf.a)
    set(Protobuf_PROTOC_EXECUTABLE /usr/local/bin/protoc)
endif()

# Set the path to the gRPC plugin
set(gRPC_CPP_PLUGIN_EXECUTABLE /usr/local/bin/grpc_cpp_plugin)

# Generate gRPC code
get_filename_component(kv_proto "proto/kv_store.proto" ABSOLUTE)
get_filename_component(kv_proto_path "${kv_proto}" PATH)
set(kv_proto_srcs "${CMAKE_CURRENT_BINARY_DIR}/kv_store.pb.cc")
set(kv_proto_hdrs "${CMAKE_CURRENT_BINARY_DIR}/kv_store.pb.h")
set(kv_grpc_srcs "${CMAKE_CURRENT_BINARY_DIR}/kv_store.grpc.pb.cc")
set(kv_grpc_hdrs "${CMAKE_CURRENT_BINARY_DIR}/kv_store.grpc.pb.h")

add_custom_command(
    OUTPUT "${kv_proto_srcs}" "${kv_proto_hdrs}" "${kv_grpc_srcs}" "${kv_grpc_hdrs}"
    COMMAND ${Protobuf_PROTOC_EXECUTABLE}
    ARGS --grpc_out "${CMAKE_CURRENT_BINARY_DIR}"
         --cpp_out "${CMAKE_CURRENT_BINARY_DIR}"
         -I "${kv_proto_path}"
         --plugin=protoc-gen-grpc="${gRPC_CPP_PLUGIN_EXECUTABLE}"
         "${kv_proto}"
    DEPENDS "${kv_proto}"
)

# Include directories
include_directories("${CMAKE_CURRENT_BINARY_DIR}" "include" ${Protobuf_INCLUDE_DIRS})

# Correct path to utf8_range directory
include_directories("/users/wjhu/grpc/third_party/utf8_range")

# Add the utf8_range source files
set(UTF8_RANGE_SOURCES
    /users/wjhu/grpc/third_party/utf8_range/utf8_range.c
    /users/wjhu/grpc/third_party/utf8_range/utf8_validity.cc
)

# Create the shared library
add_library(739kv SHARED
    src/kvclient.cpp
    src/kv_client_impl.cpp
    ${kv_proto_srcs}
    ${kv_grpc_srcs}
    ${UTF8_RANGE_SOURCES} 
)

target_link_libraries(739kv
    gRPC::grpc++
    gRPC::grpc++_reflection
    protobuf::libprotobuf
    /lib/x86_64-linux-gnu/libutf8proc.so 
)

set_target_properties(739kv PROPERTIES
    OUTPUT_NAME "739kv"
    PREFIX "lib"
)

# Create the executable for client_app
add_executable(client_app src/client_app.cpp)
target_link_libraries(client_app
    739kv
    gRPC::grpc++
    gRPC::grpc++_reflection
    protobuf::libprotobuf
    /lib/x86_64-linux-gnu/libutf8proc.so 
)

# Install the library
install(TARGETS 739kv
    LIBRARY DESTINATION lib
)

# Install the header file
install(FILES include/kvclient.h
    DESTINATION include
)

