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
### Unit Test 

Run the test command `mvn test` under the server directory.
![image](https://github.com/user-attachments/assets/80e37143-2f1a-49ca-a4e9-77e3a07c0f2b)

(old)

Note: check  `Sqlite_As_Storage` branch for updated unit test results
 
