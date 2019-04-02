#!/bin/bash

# remove existed onos and atomix containers and images, stop emulator containers
containers=(`docker ps -a | grep -E 'onos-|atomix-|emulator' | awk '{print $1}'`)
if [[ ${#containers} != 0 ]]; then
    for var in  ${containers[*]}; do
        docker stop ${var} &
    done
    wait
else
    echo "There is no container existed."
fi
if [[ $# != 1 || "$1" != "stop-docker" ]]; then
    if [[ ${#containers} != 0 ]]; then
        for var in ${containers[*]}; do
            docker rm ${var} &
        done
        wait
    fi
    images=(`docker images | grep -e onos -e none -e "<none>" | awk '{print $3}'`)
    if [[ ${#images} != 0 ]]; then
        for var in ${images[*]}; do
            docker rmi ${var} &
        done
        wait
    else
        echo "There is no onos/atomix docker image existed."
    fi
else
    echo "There is no container and image to be removed."
fi
