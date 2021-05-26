#!/usr/bin/env bash

set -e

BMV2_CPU_PORT="255"
BMV2_PP_FLAGS="-DTARGET_BMV2 -DCPU_PORT=${BMV2_CPU_PORT} -DWITH_PORT_COUNTER -DWITH_DEBUG"

PROFILE=$1
OTHER_PP_FLAGS=$2

SRC_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
OUT_DIR=${SRC_DIR}/p4c-out/${PROFILE}/bmv2/default

mkdir -p ${OUT_DIR}
mkdir -p ${OUT_DIR}/graphs

echo
echo "## Compiling profile ${PROFILE} in ${OUT_DIR}..."

dockerImage=opennetworking/p4c:stable-20210108
dockerRun="docker run --rm -w ${SRC_DIR} -v ${SRC_DIR}:${SRC_DIR} -v ${OUT_DIR}:${OUT_DIR} ${dockerImage}"

# Generate preprocessed P4 source (for debugging).
(set -x; ${dockerRun} p4c-bm2-ss --arch v1model \
        ${BMV2_PP_FLAGS} ${OTHER_PP_FLAGS} \
        --pp ${OUT_DIR}/_pp.p4 fabric.p4)

# Generate BMv2 JSON and P4Info.
(set -x; ${dockerRun} p4c-bm2-ss --arch v1model -o ${OUT_DIR}/bmv2.json \
        ${BMV2_PP_FLAGS} ${OTHER_PP_FLAGS} \
        --p4runtime-files ${OUT_DIR}/p4info.txt fabric.p4)

# Graphs.
(set -x; ${dockerRun} p4c-graphs ${BMV2_PP_FLAGS} ${OTHER_PP_FLAGS} \
        --graphs-dir ${OUT_DIR}/graphs fabric.p4)

# Convert .dot graphs to PDFs.
for f in ${OUT_DIR}/graphs/*.dot; do
    (set -x; ${dockerRun} dot -Tpdf ${f} > ${f}.pdf)
    rm -f ${f}
done

# CPU port.
(set -x; echo ${BMV2_CPU_PORT} > ${OUT_DIR}/cpu_port.txt)
