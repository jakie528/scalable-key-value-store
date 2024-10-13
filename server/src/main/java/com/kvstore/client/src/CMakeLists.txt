cmake_minimum_required(VERSION 3.10)

project(KVStoreClient VERSION 1.0)

set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED True)

find_package(gRPC REQUIRED)
find_package(Protobuf REQUIRED)

include_directories(${Protobuf_INCLUDE_DIRS} ${gRPC_INCLUDE_DIRS})

include_directories(src)

set(PROTO_SRC_DIR ${PROJECT_SOURCE_DIR}/proto)
set(PROTO_FILES ${PROTO_SRC_DIR}/kv_store.proto)

protobuf_generate_cpp(PROTO_SRCS PROTO_HDRS ${PROTO_FILES})
grpc_generate_cpp(GRPC_SRCS GRPC_HDRS ${PROTO_FILES})

set(CLIENT_SRCS
    src/kvclient.cpp
    src/kvclientimpl.cpp
    ${PROTO_SRCS}
    ${GRPC_SRCS}
)

add_executable(kvstore_client ${CLIENT_SRCS})

target_link_libraries(kvstore_client ${Protobuf_LIBRARIES} ${gRPC_LIBRARIES} grpc++ grpc++_reflection)
