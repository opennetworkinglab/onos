Docker container which works with ONOS Scenario Test Coordinator (STC)

Before building docker image:
1. Check out the master branch and go to: $ONOS_ROOT/tools/dev/docker.
2. Copy $ONOS_ROOT/bazel-bin/onos.tar.gz here, assuming you have successfully built one.
3. Copy your public key ~/.ssh/id_rsa.pub here.


Build docker image as follows. You may tag your image as you like.
```
$ sudo docker build -t onos-sshd . -f Dockerfile-sshd
```

If you want to choose the version of Atomix, pleae build as follows:
```
$ ATOMIX_VERSION=3.1.4 sudo docker build --build-arg ATOMIX_VERSION -t onos-sshd . -f Dockerfile-sshd
```

Run 3 docker containers:
```
$ sudo docker run -t -d --name onos1 onos-sshd
$ sudo docker run -t -d --name onos2 onos-sshd
$ sudo docker run -t -d --name onos3 onos-sshd
```

Check ip of docker containers created:
```
$ sudo docker inspect --format '{{ .NetworkSettings.IPAddress }}' onos1
```

If not found, create cell definition file e.g. $ONOS_ROOT/tools/test/cells/3docker. Make sure ip's match your settings.
```
export ONOS_NIC="172.17.0.*"
export OCI="172.17.0.2"
export OC1="172.17.0.2"
export OC2="172.17.0.3"
export OC3="172.17.0.4"
export ONOS_APPS="drivers,openflow,fwd"
export ONOS_USER="sdn"
```

Set up cell definition:
```
$ cell 3docker
```

Execute STC setup: 
```
$ stc setup
```

Check status using ONOS CLI:
```
$ ssh -p 8101 karaf@$OC1    # password is karaf
```

