ARG JDK_VER=11
ARG BAZEL_VER=0.27.0
ARG JOBS=2

# First stage is the build environment.
FROM ubuntu:18.04 as builder

ENV BUILD_DEPS \
    ca-certificates \
    zip \
    python \
    python3 \
    git \
    bzip2 \
    build-essential \
    curl \
    unzip
RUN apt-get update
RUN apt-get install -y ${BUILD_DEPS}

# Install Bazel
ARG BAZEL_VER
RUN curl -L -o bazel.sh https://github.com/bazelbuild/bazel/releases/download/${BAZEL_VER}/bazel-${BAZEL_VER}-installer-linux-x86_64.sh
RUN chmod +x bazel.sh && ./bazel.sh --user

# Build-stage environment variables
ENV ONOS_ROOT=/src/onos
ENV BUILD_NUMBER docker
ENV JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8

# Build ONOS. We extract the tar in the build environment to avoid having to put
# the tar in the runtime stage. This saves a lot of space.
# Note: we don't install a JDK but instead we rely on that provided by Bazel.

# Copy in the sources
COPY . ${ONOS_ROOT}
WORKDIR ${ONOS_ROOT}

ARG JOBS
ENV BAZEL_BUILD_ARGS \
    --jobs ${JOBS} \
    --verbose_failures
RUN ~/bin/bazel build onos ${BAZEL_BUILD_ARGS}

RUN mkdir /src/tar
RUN tar -xf bazel-bin/onos.tar.gz -C /src/tar --strip-components=1

# Second stage is the runtime environment.
# We use Amazon Corretto official Docker image, bazed on Amazon Linux 2 (rhel/fedora like)
FROM amazoncorretto:${JDK_VER}

MAINTAINER Ray Milkey <ray@opennetworking.org>

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

# Get ready to run command
ENTRYPOINT ["./bin/onos-service"]
CMD ["server"]
