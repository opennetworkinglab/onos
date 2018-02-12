#!/usr/bin/env bash

set -xe

# Delete vagrant user
sudo userdel -r -f vagrant

# Free space on disk
cd ~/p4tools/protobuf && make clean
cd ~/p4tools/grpc && make clean
cd ~/p4tools/bmv2 && make clean
cd ~/p4tools/bmv2/targets && make clean
cd ~/p4tools/p4runtime && make clean
rm -rf ~/p4tools/p4c/build
rm -rf ~/p4tools/libyang/build
rm -rf ~/p4tools/sysrepo/build

cat /dev/null > ~/.bash_history
