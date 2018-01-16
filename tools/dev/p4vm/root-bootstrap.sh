#!/bin/bash
set -xe

# Create user sdn
useradd -m -d /home/sdn -s /bin/bash sdn
echo "sdn:rocks" | chpasswd
echo "sdn ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/99_sdn
chmod 440 /etc/sudoers.d/99_sdn
usermod -aG vboxsf sdn
update-locale LC_ALL="en_US.UTF-8"

# Java 8
apt-get install software-properties-common -y
add-apt-repository ppa:webupd8team/java -y
apt-get update
echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | debconf-set-selections

apt-get -y install \
    oracle-java8-installer oracle-java8-set-default \
    zip unzip \
    bridge-utils \
    avahi-daemon \
    htop \
    valgrind \
    git-review

tee -a /etc/ssh/sshd_config <<EOF

UseDNS no
EOF

su sdn <<'EOF'
cd /home/sdn
bash /vagrant/user-bootstrap.sh
EOF
