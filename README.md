# ONOS : Open Network Operating System


## What is ONOS?
ONOS is a new SDN network operating system designed for high availability,
performance, scale-out.

## Top-Level Features

* High availability through clustering and distributed state management.
* Scalability through clustering and sharding of network device control.
* Performance that is good for a first release, and which has an architecture
  that will continue to support improvements.
* Northbound abstractions for a global network view, network graph, and
  application intents.
* Pluggable southbound for support of OpenFlow and new or legacy protocols.
* Graphical user interface to view multi-layer topologies and inspect elements
  of the topology.
* REST API for access to Northbound abstractions as well as CLI commands.
* CLI for debugging.
* Support for both proactive and reactive flow setup.
* SDN-IP application to support interworking with traditional IP networks
  controlled by distributed routing protocols such as BGP.
* IP-Optical use case demonstration.

Checkout our [website](http://www.onosproject.org) and our
[tools](http://www.onosproject.org/software/#tools)

## [Developer Quickstart](https://wiki.onosproject.org/display/ONOS/Developer+Quick+Start)

Code is hosted and maintained using [gerrit](https://gerrit.onosproject.org/).

The [GitHub](https://github.com/opennetworkinglab/onos) code is only a mirror. The ONOS project does not accept code through pull requests on GitHub, please do not submit them.

```bash
git clone https://gerrit.onosproject.org/onos
```

On Ubuntu/Debian, you can do the following.

### Requirements

* git
* zip
* curl
* unzip # CentOS installations only
* python # Version 2.7 is required

```bash
sudo apt-get install software-properties-common -y && \
sudo add-apt-repository ppa:webupd8team/java -y && \
sudo apt-get update && \
echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | sudo debconf-set-selections && \
sudo apt-get install oracle-java8-installer oracle-java8-set-default -y
```

ONOS is built with Buck, an open-source build tool created by Facebook and inspired by Google. It is also in use by number of well-known projects, including all Facebook’s mobile apps, Gerrit, etc. By relying on explicit dependencies between targets and SHA hashes of files (rather than on timestamps), Buck avoids unnecessary work by recognizing whether or not a target artifact requires a rebuild. This also helps to increase reproducibility of builds.

> ONOS currently uses a modified version of Buck, which has been packaged with ONOS. Please use this version until our changes have been upstreamed and released as part of an official Buck release. 

### Build

This will compile all source code assemble the installable onos.tar.gz, which is located in the buck-out directory. Note the --show-output option, which can be omitted, will display the path to this file.

```bash
export ONOS_ROOT=$(pwd)
tools/build/onos-buck build onos --show-output
```

### Run

To run ONOS locally on the development machine, simply run the following command:

```bash
tools/build/onos-buck run onos-local -- clean debug
```

The above command will create a local installation from the onos.tar.gz file (re-building it if necessary) and will start the ONOS server in the background. In the foreground, it will display a continuous view of the ONOS (Apache Karaf) log file. Options following the double-dash (–) are passed through to the ONOS Apache Karaf and can be omitted. Here, the clean option forces a clean installation of ONOS and the debug option means that the default debug port 5005 will be available for attaching a remote debugger.

### Attach

[GUI](http://localhost:8181/onos/ui) or `tools/test/bin/onos-gui localhost`

To attach to the ONOS CLI console, run:

```bash
tools/test/bin/onos localhost
```

### Mininet

To start up a Mininet network controlled by an ONOS instance that is already running on your development machine, you can use a command like:

```bash
sudo mn --controller remote,ip=<ONOS IP address> --topo torus,3,3
```

Note that you should replace <ONOS IP address> with the IP address of your development machine where ONOS is running.

### Test

To execute ONOS unit tests, including code Checkstyle validation, run the following command:

```bash
tools/build/onos-buck test
```

or more specific tests:

```bash
# All
tools/build/onos-buck test //drivers/ciena/waveserver:onos-drivers-ciena-waveserver-tests
# Only check style
tools/build/onos-buck test //drivers/ciena/waveserver:onos-drivers-ciena-waveserver-checkstyle
```

### Commit

When you are ready to commit, use [this](https://wiki.onosproject.org/display/ONOS/Sample+Gerrit+Workflow) guide

### Help

Check out our:

* [Google group](https://groups.google.com/a/onosproject.org/forum/#!forum/onos-dev)
* [Slack](https://onosproject.slack.com)
* [Wiki](https://wiki.onosproject.org/)
