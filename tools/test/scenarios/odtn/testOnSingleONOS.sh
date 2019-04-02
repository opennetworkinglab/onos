#!/bin/bash

# This scrip is used to test ODTN on single ONOS instance in branch 2.0
# Before this script:
#   1. Make sure the default Python version is 2.x
#   2. Run ONOS locally (bazel run onos-local -- clean)
#   3. Start sshd service, and make sure "ssh $USER@localhost" operation doesn't need passwd
#   4. Emulator configuration could be found under directory $HOME/emulator

# env configuration
ONOS_ROOT="${ONOS_ROOT:-~/onos}"
source ${ONOS_ROOT}/tools/dev/bash_profile
source ${ONOS_ROOT}/tools/build/envDefaults
unset OC2
unset OC3
export OC1="127.0.0.1"
export OCI="$OC1"
export ONOS_INSTANCES="$OC1"
export ONOS_USER=$USER

OV=`echo $ONOS_VERSION | sed "s/\.$USER/-SNAPSHOT/g" `
export ONOS_INSTLL_DIR=/tmp/onos-${OV}/apache-karaf-${KARAF_VERSION}/data
export EMULATOR_ROOT="${EMULATOR_ROOT:-$HOME/emulator}"

for t in {1..60}; do
    echo "$t-th times curl request"
    curl --fail -sS http://localhost:8181/onos/v1/applications --user "onos:rocks" 1>/dev/null 2>&1 && break;
    sleep 2
done

# activate odtn-service
# run emulator, and push topo into local onos instance
cd ${EMULATOR_ROOT}
docker-compose up -d
stc net-setup-odtn
if [[ $? == 0 ]]; then
    stc net-odtn-restconf
fi
