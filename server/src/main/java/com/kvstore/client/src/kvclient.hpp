#include <iostream>
#include <memory>
#include <optional>
#include <string>
#include <regex>
#include <grpcpp/grpcpp.h>
#include "kvstore.grpc.pb.h"
#include <exception>
#include <pybind11/pybind11.h>
using std::exception; 
using grpc::Channel;
using grpc::ClientContext;
using grpc::Status;
using kvstore_rpc::KvstoreRPC;
using kvstore_rpc::KvstoreRequest;
using kvstore_rpc::KvstoreResponse;

#define HOSTNAME "128.105.144.27:50051"

class KVStoreException : public exception {
public:
    KVStoreException(const char* msg) : message_(msg) {}
    KVStoreException(std::string msg) : message_(msg) {}

    const char* what() const throw() {
        return message_.c_str();
    }

  private:
    std::string message_;
};

class KVClient {
public:
    /////////////////////////////////////////////////////////////////////////////////
    //////////////////////////////// PUBLIC INTERFACE ///////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    KVClient(std::string& name) :
    channel_(grpc::CreateChannel(name, grpc::InsecureChannelCredentials()))
    , stub_(KvstoreRPC::NewStub(channel_)) {}

    std::string SendPutRequest(const std::string& key, const std::string& value) {
        if (!validateKey(key)) {
            throw KVStoreException("SendPutRequest: invalid key.");
        }

        if (!validateValue(value)) {
            throw KVStoreException("SendPutRequest: invalid value.");
        }

        KvstoreRequest request;
        request.set_request_type(KvstoreRequest::PUT_REQ);

        KvstoreRequest::TupleData* key_data = request.add_tuple_data();
        key_data->set_size(key.size());
        key_data->set_data(key);
        KvstoreRequest::TupleData* value_data = request.add_tuple_data();
        value_data->set_size(value.size());
        value_data->set_data(value);

        KvstoreResponse response;
        ClientContext context;

        Status status = stub_->contactRemote(&context, request, &response);

        if (status.ok()) {
            int response_code = response.response_code();
            if (response_code == 0) {
              return response.tuple_data(0).data();
            } else if (response_code == 1) {
              return "";
            } else {
              throw KVStoreException("SendPutRequest: kv server error.");
            }
        } else {
            throw KVStoreException(status.error_message());
        }
    }

    std::string SendGetRequest(const std::string& key) {
        if (!validateKey(key)) {
            throw KVStoreException("SendGetRequest: invalid key.");
        }

        KvstoreRequest request;
        request.set_request_type(KvstoreRequest::GET_REQ);
        KvstoreRequest::TupleData* data = request.add_tuple_data();
        data->set_size(key.size());
        data->set_data(key);

        KvstoreResponse response;
        ClientContext context;

        Status status = stub_->contactRemote(&context, request, &response);

        if (status.ok()) {
            int response_code = response.response_code();
           if (response_code == 0) {
              return response.tuple_data(0).data();
            } else if (response_code == 1) {
              return "";
            } else {
              throw KVStoreException("SendGetRequest: kv server error.");
            }
        } else {
            throw KVStoreException(status.error_message());
        }
    }

    void Shutdown() {
      KvstoreRequest request;
      request.set_request_type(KvstoreRequest::STOP_REQ);

      KvstoreResponse response;
      ClientContext context;

      Status status = stub_->contactRemote(&context, request, &response);
      if (status.ok()) {
        stub_.reset();
        return;
      }

      throw KVStoreException(status.error_message());
    }

private:
    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////// PRIVATE METHODS ///////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////

    bool validateKey(const std::string& key) {
        if (key.length() > 128) {
            std::cerr << "Error: Key exceeds 128 bytes in length." << std::endl;
            return false;
        }

        static const std::regex key_regex("^[\\x20-\\x7E]+$");
        if (!std::regex_match(key, key_regex) || key.find('[') != std::string::npos || key.find(']') != std::string::npos) {
            std::cerr << "Error: Key contains invalid characters. Only printable ASCII characters are allowed, without '[' or ']'." << std::endl;
            return false;
        }

        return true;
    }

    bool validateValue(const std::string& value) {
        if (value.length() > 2048) {
            std::cerr << "Error: Value exceeds 2048 bytes in length." << std::endl;
            return false;
        }

        static const std::regex value_regex("^[\\x20-\\x7E]+$");
        if (!std::regex_match(value, value_regex) || value.find('[') != std::string::npos || value.find(']') != std::string::npos) {
            std::cerr << "Error: Value contains invalid characters. Only printable ASCII characters are allowed, without '[' or ']'." << std::endl;
            return false;
        }

        return true;
    }

    /////////////////////////////////////////////////////////////////////////////////
    ///////////////////////////////// PRIVATE MEMBERS ///////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////
    std::shared_ptr<Channel> channel_;
    std::unique_ptr<KvstoreRPC::Stub> stub_;
};