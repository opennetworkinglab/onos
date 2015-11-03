#!/bin/bash
#make sure only root can run our script.
if [ "$(id -u)" != "0" ]; then
   echo "You need to be 'root' dude." 1>&2
   exit 1
fi

_version="1.0"

echo "========================INSTALL ONOS======================="

# Download and unzip apache-karaf
if [ -d /root/Applications ]
then 
	cd /root/Applications
else
	mkdir /root/Applications
	cd /root/Applications
fi

if [ -f apache-karaf-3.0.2.tar.gz ]
then 
	echo "apache-karaf-3.0.2.tar.gz has exist"
else 
	wget http://apache.fayea.com/karaf/3.0.2/apache-karaf-3.0.2.tar.gz
	tar -xzf apache-karaf-3.0.2.tar.gz
fi

# Download and install apache-maven

if [ -f apache-maven-3.2.5-bin.tar.gz ]
then
	echo "apache-maven-3.2.5.bin.tar.gz has exist"
else 
	wget http://mirror.bit.edu.cn/apache/maven/maven-3/3.2.5/binaries/apache-maven-3.2.5-bin.tar.gz
	tar -xzf apache-maven-3.2.5-bin.tar.gz
	mv apache-maven-3.2.5 /usr/local/apache-maven

	# set environment
	echo "export M2_HOME=/usr/local/apache-maven" >> /etc/profile
	source /etc/profile
	echo "export PATH=$PATH:$M2_HOME/bin" >> /etc/profile
	source /etc/profile
	# in case of failure of setting environment
	export PATH=$PATH:$M2_HOME/bin
fi

# Install java-8-oracle

if which java
then 
	echo "java-8 has been installed."
else
        apt-get install python-software-properties
	sudo add-apt-repository ppa:webupd8team/java -y
	sudo apt-get update
	sudo apt-get install oracle-java8-installer oracle-java8-set-default -y
	# set JAVA_HOME
	echo "export JAVA_HOME=/usr/lib/jvm/java-8-oracle" >> /etc/profile
	source /etc/profile
fi
# show the info of java and maven to check.

java -version
mvn --version

# Download ONOS
if [ -d /home/onos ]
then 
	cd /home/onos
else
	mkdir /home/onos
	cd /home/onos
fi

if which zip
then 
	echo "zip has been installed"
else
	apt-get install zip
fi

if [ -f onos-$_version.zip ] 
then 
	echo "onos-$_version.zip has exist"
else
	wget https://github.com/opennetworkinglab/onos/archive/onos-$_version.zip
	unzip onos-$_version.zip

	# set environment of ONOS

	echo "export ONOS_ROOT=/home/onos/onos-onos-$_version" >> /etc/profile
	echo "export KARAF_ROOT=/root/Applications/apache-karaf-3.0.2" >> /etc/profile

	source /etc/profile
	source $ONOS_ROOT/tools/dev/bash_profile
fi
# Build ONOS

cd onos-onos-$_version/
mvn clean install
