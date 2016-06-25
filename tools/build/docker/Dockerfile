FROM debian:jessie
MAINTAINER Ali Al-Shabibi <ali@onlab.us>

# Add Java 8 repository
ENV DEBIAN_FRONTEND noninteractive
RUN echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
    echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee /etc/apt/sources.list.d/webupd8team-java.list && \
    echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886

# Set the environment variables
ENV HOME /root
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle
ENV ONOS_ROOT /src/onos
ENV KARAF_VERSION 3.0.5
ENV KARAF_ROOT /root/onos/apache-karaf-3.0.5
ENV KARAF_LOG /root/onos/apache-karaf-3.0.5/data/log/karaf.log
ENV BUILD_NUMBER docker
ENV PATH $PATH:$KARAF_ROOT/bin

#Download and Build ONOS
WORKDIR /src
RUN     apt-get update && apt-get install -y python maven git curl oracle-java8-installer oracle-java8-set-default && \
        git clone https://github.com/opennetworkinglab/onos.git && cd onos && \
        mkdir -p /root/Downloads && \
        mvn clean install && \
        tools/build/onos-package && \
        rm -rf /root/.m2 && cd .. && \
        rm -rf onos && \
        apt-get remove --purge -y `apt-mark showauto` && \
        apt-get install oracle-java8-set-default -y && \
        apt-get clean && apt-get purge -y && apt-get autoremove -y && \
        rm -rf /var/lib/apt/lists/* && \
        rm -rf /var/cache/oracle-jdk8-installer && \
        rm -rf /root/Downloads

# Change to /root directory
WORKDIR /root

#Install ONOS
RUN mkdir onos && \
   mv /tmp/onos-*.docker.tar.gz . && \
   tar -xf onos-*.docker.tar.gz -C onos --strip-components=1 && \
   rm -rf onos-*.docker.tar.gz


# Ports
# 6653 - OpenFlow
# 8181 - GUI
# 8101 - ONOS CLI
# 9876 - ONOS CLUSTER COMMUNICATION
EXPOSE 6653 8181 8101 9876

# Get ready to run command
WORKDIR /root/onos
ENTRYPOINT ["./bin/onos-service"]
