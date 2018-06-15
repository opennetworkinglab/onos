#!/usr/bin/env bash

set -ex

BMV2_CPU_PORT="255"
BMV2_PP_FLAGS="-DTARGET_BMV2 -DCPU_PORT=${BMV2_CPU_PORT}"

PROFILE=$1
OTHER_PP_FLAGS=$2

OUT_DIR=./p4c-out/${PROFILE}/bmv2/default

mkdir -p ${OUT_DIR}

p4c-bm2-ss --arch v1model \
        -o ${OUT_DIR}/bmv2.json \
        ${BMV2_PP_FLAGS} ${OTHER_PP_FLAGS} \
        --p4runtime-file ${OUT_DIR}/p4info.txt \
        --p4runtime-format text \
        fabric.p4
