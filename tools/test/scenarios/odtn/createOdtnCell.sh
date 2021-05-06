#!/bin/bash

# Create onos and atomix containers from related images.

# Initialize the environment
shopt -s expand_aliases
export PATH="$PATH:$HOME/bin:onos/bin"
export ONOS_ROOT=~/onos
source ${ONOS_ROOT}/tools/dev/bash_profile
KARAF_VERSION=`cat ${ONOS_ROOT}/tools/build/envDefaults | grep KARAF_VERSION= | awk -F '=' '{print $2}'`

# Start ONOS cluster through docker
cd ~/onos/tools/tutorials/vm
SSH_KEY=$(cut -d\  -f2 ~/.ssh/id_rsa.pub)
FULL_SSH_KEY=$(cat ~/.ssh/id_rsa.pub)
echo "The public key in local host is: $SSH_KEY"

# Create Atomix cluster using Atomix docker image
ATOMIX_IMAGE=atomix/atomix:3.1.12
for i in {1..3}; do
    echo "Setting up atomix-$i..."
    docker container run --detach --name atomix-$i --hostname atomix-$i \
        --restart=always -v /home/sdn/onos/tools/tutorials/vm/config:/atomix/config $ATOMIX_IMAGE \
        --config /atomix/config/atomix-$i.conf
done
wait

# Create and start  ONOS cluster using ONOS docker image
ONOS_IMAGE=onos:latest
for i in {1..3}; do
    echo "Setting up onos-$i..."
    docker container run --detach --name onos-$i --hostname onos-$i --restart=always $ONOS_IMAGE
    docker exec -i onos-$i /bin/bash -c "mkdir config; cat > config/cluster.json" < $ONOS_ROOT/tools/tutorials/vm/config/cluster-$i.json
    docker exec -i onos-$i /bin/bash -c "touch /root/onos/apache-karaf-${KARAF_VERSION}/etc/keys.properties"
    docker exec -i onos-$i /bin/bash -c "echo 'sdn=$SSH_KEY,_g_:admingroup' >> /root/onos/apache-karaf-${KARAF_VERSION}/etc/keys.properties"
    docker exec -i onos-$i /bin/bash -c "/root/onos/bin/onos-user-password onos rocks"
    docker exec -i onos-$i /bin/bash -c "ssh-keygen -f /root/.ssh/id_rsa -t rsa -N ''"
    docker exec -i onos-$i /bin/bash -c "/etc/init.d/ssh start"
    docker exec -i onos-$i /bin/bash -c "echo '$FULL_SSH_KEY' >> /root/.ssh/authorized_keys"
done

# Start onos and atomix docker containers
function waitForStart {
    sleep 5
    for i in {1..3}; do
        echo "Waiting for onos-$i startup..."
        ip=$(docker container inspect onos-$i | grep \"IPAddress | cut -d: -f2 | sort -u | tr -d '", ')
        echo "IP is: $ip"
    for t in {1..60}; do
        echo "$t-th times curl request"
            curl --fail -sS http://$ip:8181/onos/v1/applications --user "onos:rocks" 1>/dev/null 2>&1 && break;
            sleep 1;
        done
    echo
        onos $ip summary >/dev/null 2>&1
    done
}

# Extract the IP addresses of the ONOS nodes
export OC1=$(docker container inspect onos-1 | grep \"IPAddress | cut -d: -f2 | sort -u | tr -d '", ')
export OC2=$(docker container inspect onos-2 | grep \"IPAddress | cut -d: -f2 | sort -u | tr -d '", ')
export OC3=$(docker container inspect onos-3 | grep \"IPAddress | cut -d: -f2 | sort -u | tr -d '", ')
export ONOS_INSTANCES="\"$OC1 $OC2 $OC3\""

waitForStart

# remove known hosts
ssh-keygen -R $OC1
ssh-keygen -R $OC2
ssh-keygen -R $OC3
# add to known-hosts list
ssh-keyscan $OC1 >> ~/.ssh/known_hosts
ssh-keyscan $OC2 >> ~/.ssh/known_hosts
ssh-keyscan $OC3 >> ~/.ssh/known_hosts

echo "#!/bin/bash" > /tmp/odtn/OCvar.sh
echo "export OC1=$OC1" >> /tmp/odtn/OCvar.sh
echo "export OC2=$OC2" >> /tmp/odtn/OCvar.sh
echo "export OC3=$OC3" >> /tmp/odtn/OCvar.sh
echo "export OCI=$OC1" >> /tmp/odtn/OCvar.sh
echo "export ONOS_INSTANCES=$ONOS_INSTANCES" >> /tmp/odtn/OCvar.sh
echo "export ONOS_USER=root" >> /tmp/odtn/OCvar.sh
echo "export ONOS_INSTALL_DIR=/root/onos/apache-karaf-${KARAF_VERSION}/data" >> /tmp/odtn/OCvar.sh
# sleep to wait onos instances start up
sleep 20
