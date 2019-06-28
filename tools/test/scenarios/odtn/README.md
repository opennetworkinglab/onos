## Description

This directory contains several Shell scripts for ODTN project testing in STC environment.
The normal steps of ODTN testing are listed (or see the wiki page: https://wiki.onosproject.org/display/ODTN/Cassini+with+STC):

#### 1. `stc net-odtn-presmoke`
This command completes preparation for testing, including latest onos image build, and onos/atomix cluster containers startup. Also, because of dynamic IP Address for containers, some related environment variables are stored in /tmp/odtn/OCvar.sh. All subsequent stc command should source this file as default environment. An example of this file is:

```shell
#!/bin/bash
export OC1=172.17.0.5
export OC2=172.17.0.6
export OC3=172.17.0.7
export OCI=172.17.0.5
export ONOS_INSTANCES="172.17.0.5 172.17.0.6 172.17.0.7"
export ONOS_USER=root
export ONOS_INSTALL_DIR=/root/onos/apache-karaf-4.2.6/data
```

Besides, the access for each onos container are very easy (`source /tmp/odtn/OCvar` firstly):
* If you want to login ONOS CLI directly, please type `onos $OC1/2/3`.
* If you want to login ONOS container, please type `ssh root@OC1/2/3`.

#### 2. `stc -ENV_DEFAULT=/tmp/odtn/OCvar.sh net-odtn-smoke`

This command contains thress substeps in order, you can use these steps one by one:
* `stc -ENV_DEFAULT=/tmp/odtn/OCvar.sh net-setup-odtn`
In this step, `odtn-service` and related multiple apps are installed and checked. Then emulator containers are started, whose topology is pushed into onos/atomix cluster via `onos-netcfg`.

This command invokes script `CheckNetInit.sh`, which need file "~/emulator/net-summary.json" to load number of device/port/link.

* `stc -ENV_DEFAULT=/tmp/odtn/OCvar.sh net-odtn-restconf`
In this step, line-side and client-side connectivity creation and deletion are tested.

* `stc -ENV_DEFAULT=/tmp/odtn/OCvar.sh net-teardown-odtn`
In this step, all onos/atomix/emulator containers are stopped and removed, and onos images are removed.
