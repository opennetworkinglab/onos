#!/usr/bin/env bash

set -xe

# Delete vagrant user
sudo userdel -r -f vagrant

# Free space on disk
rm -rf ~/p4tools/protobuf
rm -rf ~/p4tools/grpc
rm -rf ~/p4tools/bmv2
rm -rf ~/p4tools/p4runtime
rm -rf ~/p4tools/p4c
rm -rf ~/p4tools/libyang
rm -rf ~/p4tools/sysrepo

sudo apt-get clean
sudo apt-get -y autoremove
sudo rm -rf /tmp/*

# Zerofill virtual hd to save space when exporting
time sudo dd if=/dev/zero of=/tmp/zero bs=1M || true
sync ; sleep 1 ; sync ; sudo rm -f /tmp/zero

history -c
rm -f ~/.bash_history
