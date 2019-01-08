#!/bin/bash
set -xe

ONOS_BRANCH=${1:-master}

cp /etc/skel/.bashrc ~/
cp /etc/skel/.profile ~/
cp /etc/skel/.bash_logout ~/

# ONOS
git clone https://github.com/opennetworkinglab/onos.git --depth 1 -b ${ONOS_BRANCH}
tee -a ~/.bashrc <<EOF

# ONOS
export ONOS_ROOT=~/onos
source ~/onos/tools/dev/bash_profile
source ~/onos/tools/dev/p4vm/bm-commands.sh
EOF

# Build and install P4 tools
bash /vagrant/install-p4-tools.sh
# We'll delete bmv2 sources later...
cp ~/p4tools/bmv2/tools/veth_setup.sh ~/veth_setup.sh
cp ~/p4tools/bmv2/tools/veth_teardown.sh ~/veth_teardown.sh

# Mininet
git clone git://github.com/mininet/mininet
sudo ~/mininet/util/install.sh -nv

# Trellis - checkout routing repo
git clone https://github.com/opennetworkinglab/routing.git

# Trellis - install Quagga
git clone -b onos-1.11 https://gerrit.opencord.org/quagga
cd quagga
./bootstrap.sh
./configure --enable-fpm --sbindir=/usr/lib/quagga enable_user=root enable_group=root
make
sudo make install
cd ..
sudo ldconfig

# Trellis - modify apparmor for the DHCP to run properly
sudo /etc/init.d/apparmor stop
sudo ln -s /etc/apparmor.d/usr.sbin.dhcpd /etc/apparmor.d/disable/
sudo apparmor_parser -R /etc/apparmor.d/usr.sbin.dhcpd
sudo sed -i '30i  /var/lib/dhcp{,3}/dhcpclient* lrw,' /etc/apparmor.d/sbin.dhclient
sudo /etc/init.d/apparmor start

# fabric-p4test
git clone https://github.com/opennetworkinglab/fabric-p4test.git

# Set Python path for bmv2 in fabric.p4
echo 'export PYTHONPATH=$PYTHONPATH:$ONOS_ROOT/tools/dev/mininet' >> ~/.bashrc

# FIXME: for some reason protobuf python bindings are not properly installed
cd ~/p4tools/protobuf/python
sudo pip install .
