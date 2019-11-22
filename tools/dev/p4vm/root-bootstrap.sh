#!/usr/bin/env bash

set -xe

VM_TYPE=${1:-dev}

BAZEL_VER="1.0.0"
CORRETTO_URL="https://d3pxv6yz143wms.cloudfront.net/8.212.04.2/java-1.8.0-amazon-corretto-jdk_8.212.04-2_amd64.deb"

# Disable automatic updates
systemctl stop apt-daily.timer
systemctl disable apt-daily.timer
systemctl disable apt-daily.service
systemctl stop apt-daily-upgrade.timer
systemctl disable apt-daily-upgrade.timer
systemctl disable apt-daily-upgrade.service

# Remove Ubuntu user
sudo userdel -r -f ubuntu

# Create user sdn
useradd -m -d /home/sdn -s /bin/bash sdn
usermod -aG sudo sdn
usermod -aG vboxsf sdn
echo "sdn:rocks" | chpasswd
echo "sdn ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/99_sdn
chmod 440 /etc/sudoers.d/99_sdn
update-locale LC_ALL="en_US.UTF-8"

# Update and upgrade.
apt-get update
DEBIAN_FRONTEND=noninteractive apt-get -y -o Dpkg::Options::="--force-confdef" -o Dpkg::Options::="--force-confold" upgrade

wget -O corretto.deb ${CORRETTO_URL}

apt-get -y --no-install-recommends install \
    java-common \
    ./corretto.deb \
    maven \
    avahi-daemon \
    bridge-utils \
    git \
    git-review \
    htop \
    python2.7 \
    python2.7-dev \
    valgrind \
    zip unzip \
    tcpdump \
    vlan \
    ntp \
    wget \
    curl \
    net-tools \
    vim nano emacs \
    arping \
    gawk \
    texinfo \
    build-essential \
    iptables \
    automake \
    autoconf \
    libtool \
    isc-dhcp-server

rm -f corretto.deb

rm -f /usr/bin/python
ln -s `which python2.7` /usr/bin/python

# Install pip and some python deps (others are defined in install-p4-tools.sh)
curl https://bootstrap.pypa.io/get-pip.py -o get-pip.py
python2.7 get-pip.py --force-reinstall
rm -f get-pip.py
pip2.7 install ipaddress

if [[ ${VM_TYPE} = "dev" ]]
then
    # Install Bazel
    BAZEL_SH="bazel-${BAZEL_VER}-installer-linux-x86_64.sh"
    wget https://github.com/bazelbuild/bazel/releases/download/${BAZEL_VER}/${BAZEL_SH}
    chmod +x ${BAZEL_SH}
    ./${BAZEL_SH}
    rm -f ${BAZEL_SH}
fi

tee -a /etc/ssh/sshd_config <<EOF

UseDNS no
EOF

sed -i 's/PasswordAuthentication no/PasswordAuthentication yes/g' /etc/ssh/sshd_config