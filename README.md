# Scalable key-value store

## Service Implementation
### Configuration file

pom.xml: includes all necessary dependencies for gRPC and Protobuf.

### Run the service
`cd server`

Run the following Maven commands:
`mvn clean`,
`mvn generate-sources`,
`mvn compile`

Note: Check if the gRPC classes are generated. They should be in `target/generated-sources/protobuf/java` and `target/generated-sources/protobuf/grpc-java`

This should generate the necessary gRPC classes and compile the project successfully.