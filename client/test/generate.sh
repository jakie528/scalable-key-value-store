#!/bin/bash

# Create or overwrite raft_config.txt with 100 instances
for i in $(seq 50051 50150); do
    echo "0.0.0.0:$i" >> raft_config.txt
done

echo "raft_config.txt with 100 instances generated."

