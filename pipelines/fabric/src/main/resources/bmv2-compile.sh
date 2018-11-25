#!/usr/bin/env bash

set -e

BMV2_CPU_PORT="255"
BMV2_PP_FLAGS="-DTARGET_BMV2 -DCPU_PORT=${BMV2_CPU_PORT} -DWITH_PORT_COUNTER"

PROFILE=$1
OTHER_PP_FLAGS=$2
OUT_DIR=./p4c-out/${PROFILE}/bmv2/default


mkdir -p ${OUT_DIR}
mkdir -p ${OUT_DIR}/graphs

echo
echo "## Compiling profile ${PROFILE} in ${OUT_DIR}..."
(set -x; p4c-bm2-ss --arch v1model \
        -o ${OUT_DIR}/bmv2.json \
        ${BMV2_PP_FLAGS} ${OTHER_PP_FLAGS} \
        --p4runtime-file ${OUT_DIR}/p4info.txt \
        --p4runtime-format text \
        fabric.p4)
(set -x; p4c-graphs ${BMV2_PP_FLAGS} ${OTHER_PP_FLAGS} --graphs-dir ${OUT_DIR}/graphs fabric.p4)
for f in ${OUT_DIR}/graphs/*.dot; do
    (set -x; dot -Tpdf ${f} > ${f}.pdf)
    rm -f ${f}
done

(set -x; echo ${BMV2_CPU_PORT} > ${OUT_DIR}/cpu_port.txt)
