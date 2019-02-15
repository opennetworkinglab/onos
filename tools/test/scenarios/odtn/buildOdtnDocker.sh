#!/bin/bash

# This script is used to build onos by using bazel, and create related docker image from ${ONOS_ROOT}/tools/dev/Dockerfile-dev

# Initialize the environment
shopt -s expand_aliases
export PATH="$PATH:$HOME/bin:onos/bin"
export ONOS_ROOT=~/onos
source ${ONOS_ROOT}/tools/dev/bash_profile

# Compile and Package ONOS
cd ${ONOS_ROOT}
# ob is replaced by bazel build onos
bazel build onos
rtn=$?
if [[ ${rtn} -ne 0 ]]
then
    exit ${rtn}
fi
# Re-deploy ONOS
[ -f tools/dev/onos.tar.gz ] && rm -f tools/dev/onos.tar.gz
cp bazel-bin/onos.tar.gz tools/dev/
# Build ONOS's docker image, and start ONOS cluster through docker
cd tools/dev/
docker build -t onos -f Dockerfile-dev .
rm -f onos.tar.gz
exit $?
