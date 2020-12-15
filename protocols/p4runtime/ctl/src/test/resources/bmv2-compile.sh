#!/usr/bin/env bash

set -e

PROFILE=$1
OTHER_FLAGS=$2

SRC_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

echo
echo "## Compiling profile ${PROFILE} in ${SRC_DIR}..."

# Using stable-20210108 because stable doesn't support @p4runtime_translation annotations
dockerImage=opennetworking/p4c:stable-20210108
dockerRun="docker run --rm -w ${SRC_DIR} -v ${SRC_DIR}:${SRC_DIR} ${dockerImage}"


# Generate BMv2 JSON and P4Info.
(set -x; ${dockerRun} p4c-bm2-ss --arch v1model \
        ${OTHER_FLAGS} \
        --p4runtime-files ${SRC_DIR}/${PROFILE}_p4info.txt ${PROFILE}.p4)
