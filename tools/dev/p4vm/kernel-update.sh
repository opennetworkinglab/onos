#!/usr/bin/env bash

set -xe

# There is a known issue with some kernel versions that affects PTF tests:
# https://github.com/jafingerhut/p4-guide/tree/master/linux-veth-bug

# Run this script to update the kernel inside the VM.

KERNEL_VER="4.15.0-46-generic"

apt-get update
apt-get -y --no-install-recommends install \
    linux-image-${KERNEL_VER} linux-headers-${KERNEL_VER}

apt-mark hold ${KERNEL_VER}