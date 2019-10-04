#!/usr/bin/env bash

set -e

PROFILE=$1
OTHER_FLAGS=$2

SRC_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
OUT_DIR=${SRC_DIR}/p4c-out/bmv2

mkdir -p ${OUT_DIR}
mkdir -p ${OUT_DIR}/graphs

echo
echo "## Compiling profile ${PROFILE} in ${OUT_DIR}..."

dockerImage=opennetworking/p4c:stable
dockerRun="docker run --rm -w ${SRC_DIR} -v ${SRC_DIR}:${SRC_DIR} -v ${OUT_DIR}:${OUT_DIR} ${dockerImage}"

# Generate preprocessed P4 source (for debugging).
(set -x; ${dockerRun} p4c-bm2-ss --arch v1model \
        ${OTHER_FLAGS} \
        --pp ${OUT_DIR}/${PROFILE}_pp.p4 ${PROFILE}.p4)

# Generate BMv2 JSON and P4Info.
(set -x; ${dockerRun} p4c-bm2-ss --arch v1model -o ${OUT_DIR}/${PROFILE}.json \
        ${OTHER_FLAGS} \
        --p4runtime-files ${OUT_DIR}/${PROFILE}_p4info.txt ${PROFILE}.p4)
