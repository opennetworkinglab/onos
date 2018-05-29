#!/bin/bash
set -xe

cp /etc/skel/.bashrc ~/
cp /etc/skel/.profile ~/
cp /etc/skel/.bash_logout ~/

# ONOS
git clone https://github.com/opennetworkinglab/onos.git
tee -a ~/.bashrc <<EOF

# ONOS
export ONOS_ROOT=~/onos
source ~/onos/tools/dev/bash_profile
source ~/onos/tools/dev/p4vm/bm-commands.sh
EOF

# Build and install P4 tools
bash /vagrant/install-p4-tools.sh

# Mininet
git clone git://github.com/mininet/mininet ~/mininet
sudo ~/mininet/util/install.sh -nv

# Trellis routing repo
git clone https://github.com/opennetworkinglab/routing.git

# Set Python path for bmv2 in fabric.p4
echo 'export PYTHONPATH=$PYTHONPATH:~/onos/tools/dev/mininet/bmv2.py' >> ~/.bashrc
