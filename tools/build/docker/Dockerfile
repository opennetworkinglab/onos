FROM debian
MAINTAINER Ali Al-Shabibi <ali@onlab.us>

# Install required software
ENV DEBIAN_FRONTEND noninteractive
RUN echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && \
    echo "deb http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee /etc/apt/sources.list.d/webupd8team-java.list && \
    echo "deb-src http://ppa.launchpad.net/webupd8team/java/ubuntu trusty main" | tee -a /etc/apt/sources.list.d/webupd8team-java.list && \
    apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys EEA14886 && \
    apt-get update && \
    apt-get install -y oracle-java8-installer oracle-java8-set-default maven git curl && \
    apt-get clean && apt-get purge

# Set the environment variables
ENV HOME /root
ENV JAVA_HOME /usr/lib/jvm/java-8-oracle
ENV ONOS_ROOT /src/onos
ENV KARAF_VERSION 3.0.3
ENV KARAF_ROOT /root/onos/apache-karaf-3.0.3
ENV KARAF_LOG /root/onos/apache-karaf-3.0.3/data/log/karaf.log
ENV BUILD_NUMBER docker
ENV PATH $PATH:$KARAF_ROOT/bin


# Change to /src directory
WORKDIR /src

# Download ONOS
RUN git clone https://github.com/opennetworkinglab/onos.git

#Install ONOS
WORKDIR /src/onos
RUN     mkdir -p /root/Downloads && \
        mvn clean install && \
        ./tools/build/onos-package

# Change to /root directory
WORKDIR /root

RUN mkdir onos && \
   cp /tmp/onos-*.docker.tar.gz . && \
   tar -xf onos-*.docker.tar.gz -C onos --strip-components=1 && \
   rm -rf onos-*.docker.tar.gz


# Ports
# 6633 - OpenFlow
# 8181 - GUI
# 8101 - ONOS CLI
# 9876 - ONOS CLUSTER COMMUNICATION
EXPOSE 6633 8181 8101 9876

# Get ready to run command
WORKDIR /root/onos
CMD ["./bin/onos-service"]
