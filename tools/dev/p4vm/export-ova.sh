#!/usr/bin/env bash

set -xe

VM_TYPE=${1:-dev}
USE_STRATUM=${USE_STRATUM:-false}
STRATUM_BMV2_TAR=${STRATUM_BMV2_TAR-unknown}

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

function wait_vm_shutdown {
    set +x
    while vboxmanage showvminfo $1 | grep -c "running (since"; do
    echo "Waiting for VM to shutdown..."
    sleep 1
    done
    sleep 2
    set -x
}

function wait_for_tcp_port {
    set +x
    while ! nc -z $1 $2; do
    echo "Waiting for TCP port $2 on $1 to be open..."
    sleep 1
    done
    sleep 2
    set -x
}

rm -rf ./tmp
if [[ ${VM_TYPE} = "tutorial" ]]
then
    bazel build //:onos
    bazel build //:onos-package-admin
    rm -rf ~/.m2/repository/org/onosproject
    cd ${ONOS_ROOT}
    onos-publish -l
    cd ${DIR}
    mkdir -p ./tmp
    cp ../../../bazel-bin/onos.tar.gz ./tmp/onos.tar.gz
    cp ../../../bazel-bin/onos-admin.tar.gz ./tmp/onos-admin.tar.gz
    cp ../mininet/bmv2.py ./tmp/bmv2.py
    mv ~/.m2/repository/org/onosproject ./tmp/artifacts
    if [[ ${USE_STRATUM} = true ]]
    then
        cp ${STRATUM_BMV2_TAR} ./tmp/stratum_bmv2.tar.gz
    fi
fi

# Initial provisioning if necessary.
USE_STRATUM=${USE_STRATUM} vagrant up ${VM_TYPE}

rm -rf ./tmp

SSH_PORT=`vagrant port --guest 22 ${VM_TYPE}`
VB_UUID=`cat .vagrant/machines/${VM_TYPE}/virtualbox/id`

if [[ ${VM_TYPE} = "dev" ]]
then
    # Take snapshot before cleanup for local use
    # e.g. to avoid re-building P4 tools from scratch
    vboxmanage controlvm ${VB_UUID} acpipowerbutton
    wait_vm_shutdown ${VB_UUID}
    VBoxManage snapshot ${VB_UUID} take "pre-cleanup"
    vagrant up ${VM_TYPE}
    # SSH port forwarding might change after vagrant up.
    SSH_PORT=`vagrant port --guest 22 ${VM_TYPE}`
    wait_for_tcp_port 127.0.0.1 ${SSH_PORT}
fi

# Cleanup
vagrant ssh -c 'bash /vagrant/ova-cleanup.sh' ${VM_TYPE}
sleep 5
vboxmanage controlvm ${VB_UUID} acpipowerbutton
wait_vm_shutdown ${VB_UUID}

# Remove vagrant shared folder
vboxmanage sharedfolder remove ${VB_UUID} -name "vagrant"

rm -f onos-p4-${VM_TYPE}.ova
vboxmanage export ${VB_UUID} -o onos-p4-${VM_TYPE}.ova

if [[ ${VM_TYPE} = "dev" ]]
then
    sleep 1
    vboxmanage snapshot ${VB_UUID} restore pre-cleanup
    sleep 1
    vboxmanage snapshot ${VB_UUID} delete pre-cleanup
fi
