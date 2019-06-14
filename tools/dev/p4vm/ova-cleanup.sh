#!/usr/bin/env bash

set -xe

# Free space on disk
sudo rm -rf ~/p4tools
sudo rm -rf ~/quagga
sudo rm -rf ~/mininet
sudo rm -rf ~/.mininet_history
sudo rm -rf ~/.viminfo
sudo rm -rf ~/.ssh
sudo rm -rf ~/.cache/pip

sudo apt-get clean
sudo apt-get -y autoremove

sudo rm -rf /tmp/*

# Zerofill virtual hd to save space when exporting
time sudo dd if=/dev/zero of=/tmp/zero bs=1M || true
sync ; sleep 1 ; sync ; sudo rm -f /tmp/zero

history -c
rm -f ~/.bash_history

# Delete vagrant user
sudo userdel -r -f vagrant

