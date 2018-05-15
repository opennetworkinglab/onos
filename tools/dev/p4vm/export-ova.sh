#!/usr/bin/env bash

set -xe

VM_TYPE=${P4_VM_TYPE:-dev}

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

# Remove references to the existing vagrant-built VM (if any).
# We want to build a new one from scratch, not start an existing one.
rm -rf .vagrant/
vagrant up

SSH_PORT=`vagrant port --guest 22`
VB_UUID=`cat .vagrant/machines/default/virtualbox/id`

# Take snapshot before cleanup for local use
# e.g. to avoid re-building P4 tools from scratch
vboxmanage controlvm ${VB_UUID} acpipowerbutton
wait_vm_shutdown ${VB_UUID}
VBoxManage snapshot ${VB_UUID} take "pre-cleanup"

# Cleanup
vagrant up
# SSH port forwarding might change after vagrant up.
SSH_PORT=`vagrant port --guest 22`
wait_for_tcp_port 127.0.0.1 ${SSH_PORT}
sshpass -p 'rocks' \
    ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no \
    -p ${SSH_PORT} sdn@127.0.0.1 "bash /vagrant/pre-ova-cleanup.sh"
sleep 5
vboxmanage controlvm ${VB_UUID} acpipowerbutton
wait_vm_shutdown ${VB_UUID}

# Remove vagrant shared folder
vboxmanage sharedfolder remove ${VB_UUID} -name "vagrant"

rm -f onos-p4-${VM_TYPE}.ova
vboxmanage export ${VB_UUID} -o onos-p4-${VM_TYPE}.ova

sleep 1
vboxmanage snapshot ${VB_UUID} restore pre-cleanup
sleep 1
vboxmanage snapshot ${VB_UUID} delete pre-cleanup
