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
ENV BUILD_NUMBER docker
ENV JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8

# Copy in the source
COPY . /src/onos/

# Build ONOS
WORKDIR /src
RUN     apt-get update && \
        apt-get install -y python less zip curl oracle-java8-installer oracle-java8-set-default ssh && \
        cd onos && \
        tools/build/onos-buck build onos && \
        cp buck-out/gen/tools/package/onos-package/onos.tar.gz /tmp/ && \
        cd .. && \
        rm -rf onos && \
        apt-get clean && apt-get purge -y && apt-get autoremove -y && \
        rm -rf /var/lib/apt/lists/* && \
        rm -rf /var/cache/oracle-jdk8-installer

# Change to /root directory
WORKDIR /root

# Install ONOS
RUN mkdir onos && \
   mv /tmp/onos.tar.gz . && \
   tar -xf onos.tar.gz -C onos --strip-components=1 && \
   rm -rf onos.tar.gz


# Ports
# 6653 - OpenFlow
# 6640 - OVSDB
# 8181 - GUI
# 8101 - ONOS CLI
# 9876 - ONOS CLUSTER COMMUNICATION
EXPOSE 6653 6640 8181 8101 9876

# Get ready to run command
WORKDIR /root/onos
ENTRYPOINT ["./bin/onos-service"]
