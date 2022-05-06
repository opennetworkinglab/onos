# Copyright 2015-present Open Networking Foundation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# With this dockerfile you can build a ONOS Docker container

ARG JOBS=2
ARG PROFILE=default
ARG TAG=11.0.13-11.52.13
ARG JAVA_PATH=/usr/lib/jvm/zulu11

# First stage is the build environment.
# zulu-openjdk images are based on Ubuntu.
FROM azul/zulu-openjdk:${TAG} as builder

ENV BUILD_DEPS \
    ca-certificates \
    zip \
    python3 \
    git \
    bzip2 \
    build-essential \
    curl \
    unzip
RUN apt-get update && apt-get install -y ${BUILD_DEPS}

# Install Bazelisk, which will download the version of bazel specified in
# .bazelversion
RUN curl -L -o bazelisk https://github.com/bazelbuild/bazelisk/releases/download/v1.11.0/bazelisk-linux-amd64
RUN chmod +x bazelisk && mv bazelisk /usr/bin

# Build-stage environment variables
ENV ONOS_ROOT /src/onos
ENV BUILD_NUMBER docker
ENV JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF8

# Copy in the sources
COPY . ${ONOS_ROOT}
WORKDIR ${ONOS_ROOT}

# Build ONOS using the JDK pre-installed in the base image, instead of the
# Bazel-provided remote one. By doing wo we make sure to build with the most
# updated JDK, including bug and security fixes, independently of the Bazel
# version. NOTE that WORKSPACE-docker file defines dockerjdk
ARG JOBS
ARG JAVA_PATH
ARG PROFILE
RUN cat WORKSPACE-docker >> WORKSPACE && bazelisk build onos \
    --jobs ${JOBS} \
    --verbose_failures \
    --java_runtime_version=dockerjdk_11 \
    --tool_java_runtime_version=dockerjdk_11 \
    --define profile=${PROFILE}

# We extract the tar in the build environment to avoid having to put the tar in
# the runtime stage. This saves a lot of space.
RUN mkdir /output
RUN tar -xf bazel-bin/onos.tar.gz -C /output --strip-components=1

# Second and final stage is the runtime environment.
FROM azul/zulu-openjdk:${TAG}

LABEL org.label-schema.name="ONOS" \
      org.label-schema.description="SDN Controller" \
      org.label-schema.usage="http://wiki.onosproject.org" \
      org.label-schema.url="http://onosproject.org" \
      org.label-scheme.vendor="Open Networking Foundation" \
      org.label-schema.schema-version="1.0" \
      maintainer="onos-dev@onosproject.org"

RUN apt-get update && apt-get install -y curl && \
	rm -rf /var/lib/apt/lists/*

# Install ONOS in /root/onos
COPY --from=builder /output/ /root/onos/
WORKDIR /root/onos

# Set JAVA_HOME (by default not exported by zulu images)
ARG JAVA_PATH
ENV JAVA_HOME ${JAVA_PATH}

# Ports
# 6653 - OpenFlow
# 6640 - OVSDB
# 8181 - GUI
# 8101 - ONOS CLI
# 9876 - ONOS intra-cluster communication
EXPOSE 6653 6640 8181 8101 9876

# Run ONOS
ENTRYPOINT ["./bin/onos-service"]
CMD ["server"]
