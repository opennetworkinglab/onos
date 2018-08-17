#!/usr/bin/env bash

set -xe

# Delete vagrant user
sudo userdel -r -f vagrant

# Free space on disk
sudo rm -rf ~/p4tools/protobuf
sudo rm -rf ~/p4tools/grpc
sudo rm -rf ~/p4tools/bmv2
sudo rm -rf ~/p4tools/PI
sudo rm -rf ~/p4tools/p4c
sudo rm -rf ~/p4tools/libyang
sudo rm -rf ~/p4tools/sysrepo
sudo rm -rf ~/p4tools/scapy-vxlan
sudo rm -rf ~/p4tools/ptf
sudo rm -rf ~/quagga

sudo apt-get clean
sudo apt-get -y autoremove
sudo sudo rm -rf /tmp/*

# Zerofill virtual hd to save space when exporting
time sudo dd if=/dev/zero of=/tmp/zero bs=1M || true
sync ; sleep 1 ; sync ; sudo rm -f /tmp/zero

history -c
rm -f ~/.bash_history
