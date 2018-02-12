#!/usr/bin/env bash

set -xe

# Remove references to the existing vagrant-built VM (if any).
# We want to build a new one from scratch, not start an existing one.
rm -rf .vagrant/

vagrant up

SSH_PORT=`vagrant port --guest 22`
VB_UUID=`cat .vagrant/machines/default/virtualbox/id`

sshpass -p 'rocks' \
    ssh -o UserKnownHostsFile=/dev/null -o StrictHostKeyChecking=no \
    -p ${SSH_PORT} sdn@127.0.0.1 "bash /vagrant/pre-ova-cleanup.sh"

sleep 5
vboxmanage controlvm ${VB_UUID} acpipowerbutton

# Wait for VM to power off
sleep 30

# Remove vagrant shared folder
vboxmanage sharedfolder remove ${VB_UUID} -name "vagrant"

rm -rf onos-p4-dev.ova
vboxmanage export ${VB_UUID} -o onos-p4-dev.ova
