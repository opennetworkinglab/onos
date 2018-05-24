#!/bin/bash

set -xe

sudo apt-get update
sudo apt-get install -y subversion

# Install Docker
sudo apt-get install -y \
    apt-transport-https \
    ca-certificates \
    curl \
    software-properties-common

curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
sudo add-apt-repository \
   "deb [arch=amd64] https://download.docker.com/linux/ubuntu \
   $(lsb_release -cs) \
   stable"
sudo apt-get update
sudo apt-get install -y docker-ce

# Clone tutorials/P4D2_2017_Fall
cd /home/sdn/p4tools
mkdir tutorials
cd tutorials
svn export https://github.com/p4lang/tutorials/trunk/P4D2_2017_Fall
cd P4D2_2017_Fall/exercises

# Clone INT and get INT Telemetry lua script for Wireshark Dissector
git clone https://github.com/MehmedGIT/INT-InBand-Network-Telemetry-.git ./INT
sudo mv ./INT/run_exercise.py ../utils/
cd INT
svn export https://github.com/MehmedGIT/P4_INT_Wireshark_Dissector/trunk/int_telemetry-report.lua
cd ..
sudo chmod 755 ./INT -R
cd ~

# Install mvn
sudo apt-get install -y maven

# Clone prometheus_int_exporter
git clone https://github.com/serkantul/prometheus_int_exporter
cd prometheus_int_exporter

# Install jnetpcap
wget -O jnetpcap-1.3.0.tgz https://sourceforge.net/projects/jnetpcap/files/jnetpcap/1.3/jnetpcap-1.3.0-1.ubuntu.x86_64.tgz/download
tar xzf jnetpcap-1.3.0.tgz
mvn install:install-file -Dfile="./jnetpcap-1.3.0/jnetpcap.jar" -DgroupId="jnetpcap" -DartifactId="jnetpcap" -Dversion="1.3.0" -Dpackaging="jar"

# Produce single .jar file
mvn package

# Install Prometheus
sudo docker pull prom/prometheus

# Install Pushgateway
sudo docker pull prom/pushgateway



