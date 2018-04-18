#!/bin/bash
set -xe

VM_TYPE=${1:-dev}

# Create user sdn
useradd -m -d /home/sdn -s /bin/bash sdn
echo "sdn:rocks" | chpasswd
echo "sdn ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/99_sdn
chmod 440 /etc/sudoers.d/99_sdn
usermod -aG vboxsf sdn
update-locale LC_ALL="en_US.UTF-8"

if [ ${VM_TYPE} = "tutorial" ]
then
    cp /vagrant/tutorial-bootstrap.sh /home/sdn/tutorial.sh
    sudo chown sdn:sdn /home/sdn/tutorial.sh
    su sdn <<'EOF'
bash /home/sdn/tutorial.sh
EOF
    rm -rf /home/sdn/tutorial.sh
fi

# Java 8
apt-get install software-properties-common -y
add-apt-repository ppa:webupd8team/java -y
apt-get update
echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | debconf-set-selections

# Workaround to: https://stackoverflow.com/questions/46815897/jdk-8-is-not-installed-error-404-not-found
set +e
apt-get install -y oracle-java8-installer
set -e
sed -i 's|JAVA_VERSION=8u161|JAVA_VERSION=8u171|' /var/lib/dpkg/info/oracle-java8-installer.*
sed -i 's|PARTNER_URL=http://download.oracle.com/otn-pub/java/jdk/8u161-b12/2f38c3b165be4555a1fa6e98c45e0808/|PARTNER_URL=http://download.oracle.com/otn-pub/java/jdk/8u171-b11/512cd62ec5174c3487ac17c61aaa89e8/|' /var/lib/dpkg/info/oracle-java8-installer.*
sed -i 's|SHA256SUM_TGZ="6dbc56a0e3310b69e91bb64db63a485bd7b6a8083f08e48047276380a0e2021e"|SHA256SUM_TGZ="b6dd2837efaaec4109b36cfbb94a774db100029f98b0d78be68c27bec0275982"|' /var/lib/dpkg/info/oracle-java8-installer.*
sed -i 's|J_DIR=jdk1.8.0_161|J_DIR=jdk1.8.0_171|' /var/lib/dpkg/info/oracle-java8-installer.*

apt-get -y --no-install-recommends install \
    oracle-java8-installer \
    oracle-java8-set-default \
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
