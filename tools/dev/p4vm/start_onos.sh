#!/usr/bin/env bash

ONOS_TAR=~/onos.tar.gz

[ -f $ONOS_TAR ] || (echo "$ONOS_TAR not found" && exit 1)

ONOS_DIR=/tmp/$(tar tf $ONOS_TAR | head -n 1 | cut -d/ -f1)

# Kill any running instances
ps -ef | grep apache.karaf.main.Main | grep -v grep | awk '{print $2}' | xargs kill -9 &>/dev/null

# Do not tolerate any errors from this point onward
set -e

echo "Running clean installation..."
# Blitz previously unrolled onos- directory
rm -fr $ONOS_DIR
# Unroll new image from the specified tar file
[ -f $ONOS_TAR ] && tar zxf $ONOS_TAR -C /tmp

echo "Configuring password-less CLI access..."
# Run using the secure SSH client
[ ! -f ~/.ssh/id_rsa.pub ] && (echo "Missing SSH public key (~/.ssh/id_rsa.pub), please generate one using ssh-keygen"; exit 1)
$ONOS_DIR/bin/onos-user-key $(id -un) "$(cut -d\  -f2 ~/.ssh/id_rsa.pub)"
$ONOS_DIR/bin/onos-user-password onos rocks

# Create config/cluster.json (cluster metadata)
IP=${ONOS_IP:-127.0.0.1}
echo "Creating local cluster configs for IP $IP..."
[ -d $ONOS_DIR/config ] || mkdir -p $ONOS_DIR/config
cat > $ONOS_DIR/config/cluster.json <<-EOF
{
  "name": "default-$RANDOM",
  "node": {
    "id": "$IP",
    "ip": "$IP",
    "port": 9876
  },
  "clusterSecret": "$RANDOM"
}
EOF

# Change into the ONOS home directory
cd $ONOS_DIR
export ONOS_HOME=$PWD

# Start ONOS as a server, but include any specified options
./bin/onos-service server "$@"
