# Scalable key-value store

## Service Implementation
### Configuration file

pom.xml: includes all necessary dependencies for gRPC and Protobuf.

### Proto file
`kv_store.proto`: define the public-facing API for key-value store service, used by clients to interact with distributed key-value store.

`Raft.proto`: for internal Raft communication between nodes, used internally by the nodes in Raft cluster to maintain consensus.

### Run the service
`cd server`

Run the following Maven commands:
`mvn clean`,
`mvn generate-sources`,
`mvn compile`

Note: Check if the gRPC classes are generated. They should be in `target/generated-sources/protobuf/java` and `target/generated-sources/protobuf/grpc-java`

This should generate the necessary gRPC classes and compile the project successfully.

` java -jar ./target/kv-store-1.0-SNAPSHOT.jar /users/wjhu/P2/server/server/config.txt` to lauch the server
### Unit Test 

Run the test command `mvn test` under the server directory.
![image](https://github.com/user-attachments/assets/0af7c83c-1cd6-4f2c-8174-472ca25b69b4)


Client side

Setup client
`mkdir build && cd build`
`cmake ..`
`make -j8`

Launch client
`./client_app ../src/raft_config.txt`

