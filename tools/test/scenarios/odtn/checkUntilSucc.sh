#!/bin/bash

# This script is used to execute some checking commands in period to confirm whether specific requirement is satisfied.
# $1 - the command to be executed in this script, whose parameter splitter is +, but ont space. This command could use |, &&, || to concatenate multiple shell commands.
# $2 - Optional. If exists, it means the output (Note: not returned value) of $1 should equals $2.

cmd=${1//'+'/' '}
if [ $# == 1 ]; then
    for i in {1..60}; do
        eval ${cmd}
        rtn=$?
        if [[ ${rtn} -ne 0 ]]
        then
            echo "$i-th execution returns $rtn"
            sleep 3
        else
            exit 0
        fi
    done
elif [ $# == 2 ]; then
    for i in {1..60}; do
        out=`eval ${cmd}`
        rtn=$?
        if [[ ${rtn} -ne 0 || "$out" != $2 ]]; then
            echo "$i-th execution fails"
            sleep 3
        else
            exit 0
        fi
    done
fi
