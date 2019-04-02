#!/bin/bash

# Get first IPv4 Address on local machine, even the machine has lots of NICs.
localIP=`ifconfig -a|grep inet|grep -v 127.0.0.1|grep -v inet6|awk '{print $2}'|tr -d "addr:" | head -n1`
mkdir /tmp/odtn
# Replace IP address in openconfig json files with real ones.
cat ${ONOS_ROOT}/apps/odtn/api/src/test/resources/openconfig-devices.json | sed "s/127.0.0.1/${localIP}/g" > /tmp/odtn/openconfig-devices.json
cat ${ONOS_ROOT}/apps/odtn/api/src/test/resources/openconfig-device-link.json | sed "s/127.0.0.1/${localIP}/g" > /tmp/odtn/openconfig-device-link.json
sleep 30
