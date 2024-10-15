cmake_minimum_required(VERSION 3.10)

# Project name and version
project(QuorumP1 VERSION 1.0)
message(STATUS "Current project dir: ${PROJECT_SOURCE_DIR}")

# Set the C++ standard
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED True)

set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Add server service
file(GLOB_RECURSE COMPUTE_CXX_SOURCE_FILES
${PROJECT_SOURCE_DIR}/src/service/*.cpp)

# Find required packages
find_package(Protobuf CONFIG REQUIRED PATHS /home/rodda/miniconda3)
find_package(gRPC CONFIG REQUIRED PATHS /home/rodda/miniconda3)

# Find SQLite3 package
find_package(SQLite3 REQUIRED)
if (SQLite3_FOUND)
    message(STATUS "Found SQLite3")
else()
    message(FATAL_ERROR "SQLite3 not found. Please install SQLite3.")
endif()

find_library(JEMALLOC_LIB jemalloc REQUIRED)

##############################################################################
# Set Google Test ###########################################################
##############################################################################
include(FetchContent)
FetchContent_Declare(
        googletest
        URL https://github.com/google/googletest/archive/03597a01ee50ed33e9dfd640b249b4be3799d395.zip
)
set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(googletest)

##############################################################################
# Set proto file #############################################################
##############################################################################
set(PROTO_SRC_DIR ${PROJECT_SOURCE_DIR}/proto)
set(PROTO_SRC ${PROTO_SRC_DIR}/kv_store.proto)  # Corrected the proto file name
set(kvstore_proto_cc "${CMAKE_CURRENT_BINARY_DIR}/kv_store.pb.cc")
set(kvstore_proto_hdr "${CMAKE_CURRENT_BINARY_DIR}/kv_store.pb.h")
set(kvstore_grpc_cc "${CMAKE_CURRENT_BINARY_DIR}/kv_store.grpc.pb.cc")
set(kvstore_grpc_hdr "${CMAKE_CURRENT_BINARY_DIR}/kv_store.grpc.pb.h")
execute_process(COMMAND which protoc OUTPUT_VARIABLE PROTOC_PATH OUTPUT_STRIP_TRAILING_WHITESPACE)
execute_process(COMMAND which grpc_cpp_plugin OUTPUT_VARIABLE GRPC_CPP_PLUGIN OUTPUT_STRIP_TRAILING_WHITESPACE)
add_custom_command(
       OUTPUT "${kvstore_grpc_cc}" "${kvstore_grpc_hdr}" "${kvstore_proto_cc}" "${kvstore_proto_hdr}"
       COMMAND ${PROTOC_PATH}
       ARGS --grpc_out "${CMAKE_CURRENT_BINARY_DIR}"
       --cpp_out "${CMAKE_CURRENT_BINARY_DIR}"
       -I "${PROTO_SRC_DIR}"
       --plugin=protoc-gen-grpc=${GRPC_CPP_PLUGIN}
       "${PROTO_SRC}"
       DEPENDS "${PROTO_SRC}")
include_directories(${CMAKE_CURRENT_BINARY_DIR})
add_library(kvstore_grpc_proto STATIC ${kvstore_proto_cc} ${kvstore_proto_hdr} ${kvstore_grpc_cc} ${kvstore_grpc_hdr})
target_compile_options(kvstore_grpc_proto PRIVATE -fPIC)
target_link_libraries(kvstore_grpc_proto
       gRPC::grpc++
       gRPC::grpc++_reflection
       protobuf::libprotobuf)
set_target_properties(kvstore_grpc_proto PROPERTIES POSITION_INDEPENDENT_CODE ON)

###############################################################################
# Add header files ############################################################
###############################################################################
set(COMMON_HEADER_DIRS
        client/include
        client/include/common
        client/include/db
        client/include/transport
        )
include_directories(${COMMON_HEADER_DIRS})

###############################################################################
# Add source files ############################################################
###############################################################################
# Add common src files
file(GLOB_RECURSE COMMON_CXX_SOURCE_FILES
        ${PROJECT_SOURCE_DIR}/client/src/*.cpp
        ${PROJECT_SOURCE_DIR}/client/db/*.cpp
        ${PROJECT_SOURCE_DIR}/client/transport/*.cpp
)

# Add client executable
add_executable(kvstore_client ${COMMON_CXX_SOURCE_FILES})
target_include_directories(kvstore_client PRIVATE ${COMMON_HEADER_DIRS})
target_link_libraries(kvstore_client kvstore_grpc_proto gRPC::grpc++ gRPC::grpc++_reflection protobuf::libprotobuf SQLite::SQLite3 ${JEMALLOC_LIB})