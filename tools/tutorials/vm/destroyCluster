#!/bin/bash

# Destroy the ONOS cluster running as docker images
for i in {1..3}; do
    echo "Destroying onos-$i..."
    docker stop onos-$i
done

# Destroy Atomix cluster running as docker images
for i in {1..3}; do
    echo "Destroying atomix-$i..."
    docker stop atomix-$i
done

docker container prune --force
