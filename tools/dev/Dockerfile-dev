# Dockerfile for ODTN container

# First stage is the build environment
FROM picoded/ubuntu-openjdk-8-jdk as builder
MAINTAINER Boyuan Yan <boyuan@opennetworking.org>

# Set the environment variables
ENV HOME /root
ENV BUILD_NUMBER docker
ENV JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8

# Copy in the source
COPY onos.tar.gz /src/onos/

# Build ONOS
# We extract the tar in the build environment to avoid having to put the tar
# in the runtime environment - this saves a lot of space
# FIXME - dependence on ONOS_ROOT and git at build time is a hack to work around
# build problems
WORKDIR /src/onos
RUN apt-get update && \
        DEBIAN_FRONTEND=noninteractive \
        apt-get install -y zip python git bzip2 build-essential && \
        export ONOS_ROOT=/src/onos && \
        mkdir -p /src/tar && \
        cd /src/tar && \
        tar -xf /src/onos/onos.tar.gz --strip-components=1 && \
        rm -rf /src/onos/bazel-* .git

# Second stage is the runtime environment
FROM adoptopenjdk/openjdk11:x86_64-ubuntu-jdk-11.0.1.13-slim

# Change to /root directory
RUN     mkdir -p /root/onos
WORKDIR /root/onos

# Install ONOS
COPY --from=builder /src/tar/ .

# Configure ONOS to log to stdout
RUN sed -ibak '/log4j.rootLogger=/s/$/, stdout/' $(ls -d apache-karaf-*)/etc/org.ops4j.pax.logging.cfg

LABEL org.label-schema.name="ONOS" \
      org.label-schema.description="SDN Controller" \
      org.label-schema.usage="http://wiki.onosproject.org" \
      org.label-schema.url="http://onosproject.org" \
      org.label-scheme.vendor="Open Networking Foundation" \
      org.label-schema.schema-version="1.0"

# Ports
# 6653 - OpenFlow
# 6640 - OVSDB
# 8181 - GUI
# 8101 - ONOS CLI
# 9876 - ONOS intra-cluster communication
EXPOSE 6653 6640 8181 8101 9876

# Open SSH server
RUN apt-get update && apt-get install -y openssh-server
RUN mkdir /var/run/sshd
RUN echo 'root:rocks' | chpasswd
EXPOSE 22

# Get ready to run command
ENTRYPOINT ["./bin/onos-service"]
CMD ["server"]
