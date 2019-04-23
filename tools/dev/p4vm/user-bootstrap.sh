#!/bin/bash
set -xe

VM_TYPE=${1:-unknown}
USE_STRATUM=${2:-false}

cd /home/sdn

cp /etc/skel/.bashrc ~/
cp /etc/skel/.profile ~/
cp /etc/skel/.bash_logout ~/

#  With Ubuntu 18.04 sometimes .cache is owned by root...
mkdir -p ~/.cache
sudo chown -hR sdn:sdn ~/.cache

echo 'export JAVA_HOME=$(readlink -f /usr/bin/java | sed "s:bin/java::")' >>  ~/.bash_aliases

if [[ ${VM_TYPE} = "dev" ]]
then
    git clone https://github.com/opennetworkinglab/onos.git
    tee -a ~/.bash_aliases <<'EOF'

# ONOS
export ONOS_ROOT=~/onos
source ~/onos/tools/dev/bash_profile
source ~/onos/tools/dev/p4vm/bm-commands.sh

export OCI=127.0.0.1
# Uncomment if ONOS runs on the host system and we access the VM via ssh
# export OCI=`echo $SSH_CLIENT | awk '{ print $1}'`

export OC1=$OCI
export ONOS_APPS=gui,drivers.bmv2,lldpprovider,hostprovider
EOF
else
    # Tutorial. Install ONOS release.
    cp /vagrant/tmp/onos.tar.gz ~/
    echo 'export OCI=127.0.0.1' >> ~/.bash_aliases
    echo 'export OC1=$OCI' >> ~/.bash_aliases
    echo 'export ONOS_INSTANCES="$OC1"' >> ~/.bash_aliases
    echo 'export ONOS_WEB_USER=onos' >> ~/.bash_aliases
    echo 'export ONOS_WEB_PASS=rocks' >> ~/.bash_aliases
    echo 'export ONOS_APPS=gui,drivers.bmv2,lldpprovider,hostprovider' >> ~/.bash_aliases
    cp /vagrant/start_onos.sh ~/
    chmod +x ~/start_onos.sh
    # onos-admin commands
    mkdir ~/onos-admin
    tar xzf /vagrant/tmp/onos-admin.tar.gz -C onos-admin --strip-components 1
    echo 'export PATH=$PATH:~/onos-admin' >> ~/.bash_aliases
    # Maven artifacts
    mkdir -p ~/.m2/repository/org/onosproject
    cp -r /vagrant/tmp/artifacts/* ~/.m2/repository/org/onosproject/
    # Export alias for bm-* commands
    cp /vagrant/bm-commands.sh ~/
    echo 'source ~/bm-commands.sh' >> ~/.bash_aliases
    # BMv2 custom Mininet switch classes.
    cp /vagrant/tmp/bmv2.py ~/
    echo 'export BMV2_MN_PY=~/bmv2.py' >> ~/.bash_aliases
    if [[ ${USE_STRATUM} = true ]]
    then
        # Install stratum_bmv2 binary.
        mkdir stratum
        tar xzf /vagrant/tmp/stratum_bmv2.tar.gz -C stratum --strip-components 1
    fi
fi

# Build and install P4 tools
DEBUG_FLAGS=true FAST_BUILD=true USE_STRATUM=false bash /vagrant/install-p4-tools.sh
echo 'export BMV2_INSTALL=/usr/local' >> ~/.bash_aliases
if [[ ${USE_STRATUM} = true ]]
then
    # Rebuild and install PI/BMv2 with stratum config parameters. Building first
    # without stratum parameters is useful to get P4Runtime Python binding
    # installed as well as simple_switch_grpc.
    rm -rf ~/p4tools/bmv2/.last_built_commit*
    rm -rf ~/p4tools/PI/.last_built_commit*
    # Build up until bmv2. No need to re-build p4c and others.
    DEBUG_FLAGS=true FAST_BUILD=true USE_STRATUM=true bash /vagrant/install-p4-tools.sh bmv2
    echo 'export STRATUM_ROOT=~/stratum' >> ~/.bash_aliases
fi

# We'll delete bmv2 sources later...
cp ~/p4tools/bmv2/tools/veth_setup.sh ~/veth_setup.sh
cp ~/p4tools/bmv2/tools/veth_teardown.sh ~/veth_teardown.sh

# Mininet
git clone git://github.com/mininet/mininet
sudo ~/mininet/util/install.sh -nv

if [[ ${VM_TYPE} = "dev" ]]
then
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
fi
