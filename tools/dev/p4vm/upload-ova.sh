#!/usr/bin/env bash

VM_TYPE=${P4_VM_TYPE:-dev}

python $ONOS_ROOT/tools/build/uploadToS3.py -f onos-p4-${VM_TYPE}.ova ./onos-p4-${VM_TYPE}.ova

