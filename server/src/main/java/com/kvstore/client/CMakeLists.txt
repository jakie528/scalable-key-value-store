cmake_minimum_required(VERSION 3.10)

# Project name and version
project(KVStore VERSION 1.0)

# Set the C++ standard
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED True)

set(CMAKE_POSITION_INDEPENDENT_CODE ON)

# Find Protobuf and gRPC
find_package(Protobuf CONFIG REQUIRED)
find_package(gRPC CONFIG REQUIRED)

# Set directories for headers and source files
include_directories(${Protobuf_INCLUDE_DIRS} ${gRPC_INCLUDE_DIRS})
include_directories(${CMAKE_CURRENT_BINARY_DIR})

# Define paths for proto files
set(PROTO_SRC_DIR ${PROJECT_SOURCE_DIR}/proto)
set(PROTO_SRC ${PROTO_SRC_DIR}/kv_store.proto)

# Define generated files for protobuf and gRPC
set(kvstore_proto_cc "${CMAKE_CURRENT_BINARY_DIR}/kv_store.pb.cc")
set(kvstore_proto_hdr "${CMAKE_CURRENT_BINARY_DIR}/kv_store.pb.h")
set(kvstore_grpc_cc "${CMAKE_CURRENT_BINARY_DIR}/kv_store.grpc.pb.cc")
set(kvstore_grpc_hdr "${CMAKE_CURRENT_BINARY_DIR}/kv_store.grpc.pb.h")

# Generate the protobuf and gRPC files
execute_process(COMMAND which grpc_cpp_plugin OUTPUT_VARIABLE GRPC_CPP_PLUGIN OUTPUT_STRIP_TRAILING_WHITESPACE)
add_custom_command(
  OUTPUT "${kvstore_grpc_cc}" "${kvstore_grpc_hdr}" "${kvstore_proto_cc}" "${kvstore_proto_hdr}"
  COMMAND protobuf::protoc
  ARGS --grpc_out "${CMAKE_CURRENT_BINARY_DIR}"
       --cpp_out "${CMAKE_CURRENT_BINARY_DIR}"
       -I "${PROTO_SRC_DIR}"
       --plugin=protoc-gen-grpc=${GRPC_CPP_PLUGIN}
       "${PROTO_SRC}"
  DEPENDS "${PROTO_SRC}"
)

# Create a static library for protobuf and gRPC
add_library(kvstore_grpc_proto STATIC ${kvstore_proto_cc} ${kvstore_proto_hdr} ${kvstore_grpc_cc} ${kvstore_grpc_hdr})
target_link_libraries(kvstore_grpc_proto
  gRPC::grpc++
  gRPC::grpc++_reflection
  protobuf::libprotobuf
)

###############################################################################
# Add client source files #####################################################
###############################################################################
# Client source files are located in client/src/
file(GLOB CLIENT_SRC_FILES
  ${PROJECT_SOURCE_DIR}/client/src/*.cpp
)

if (CLIENT_SRC_FILES)
    # Add the client executable
    add_executable(kvstore_client ${CLIENT_SRC_FILES})
    target_link_libraries(kvstore_client kvstore_grpc_proto gRPC::grpc++ gRPC::grpc++_reflection protobuf::libprotobuf)
else()
    message(WARNING "No client source files found in ${PROJECT_SOURCE_DIR}/client/src/")
endif()

###############################################################################
# Optional: Google Test (if you have tests) ###################################
###############################################################################
include(FetchContent)
FetchContent_Declare(
  googletest
  URL https://github.com/google/googletest/archive/03597a01ee50ed33e9dfd640b249b4be3799d395.zip
)
set(gtest_force_shared_crt ON CACHE BOOL "" FORCE)
FetchContent_MakeAvailable(googletest)

# If you have unit tests, include them
file(GLOB TEST_SRC_FILES
  ${PROJECT_SOURCE_DIR}/client/test/*.cpp
)

if (TEST_SRC_FILES)
    # Add the test executable
    add_executable(kvstore_test ${TEST_SRC_FILES})
    target_link_libraries(kvstore_test gtest_main kvstore_grpc_proto gRPC::grpc++ gRPC::grpc++_reflection protobuf::libprotobuf)

    enable_testing()
    add_test(NAME kvstore_test COMMAND kvstore_test)
else()
    message(WARNING "No test source files found in ${PROJECT_SOURCE_DIR}/client/test/")
endif()
