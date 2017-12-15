#!/bin/bash
set -xe

cp /etc/skel/.bashrc ~/
cp /etc/skel/.profile ~/
cp /etc/skel/.bash_logout ~/

# ONOS
git clone https://github.com/opennetworkinglab/onos.git
echo "export ONOS_ROOT=~/onos" >> ~/.bashrc
echo "source ~/onos/tools/dev/bash_profile" >> ~/.bashrc

# Build and install P4 tools
bash ~/onos/tools/dev/bin/onos-setup-p4-dev

# Mininet
git clone git://github.com/mininet/mininet ~/mininet
sudo ~/mininet/util/install.sh -nwv
