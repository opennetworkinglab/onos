# ONOS : Open Network Operating System


## What is ONOS?
ONOS is the only SDN controller platform that supports the transition from legacy “brown field” networks to SDN “green field” networks.
This enables exciting new capabilities, and disruptive deployment and operational cost points for network operators.

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


## Getting started

### Dependencies

The following packages are reuqired:

* git
* zip
* curl
* unzip
* python2.7
* Oracle JDK8

To install Oracle JDK8, use following commands (Ubuntu):
```bash
$ sudo apt-get install software-properties-common -y && \
  sudo add-apt-repository ppa:webupd8team/java -y && \
  sudo apt-get update && \
  echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true" | sudo debconf-set-selections && \
  sudo apt-get install oracle-java8-installer oracle-java8-set-default -y
```

### Build ONOS from source

1. Clone the code from ONOS gerrit repository
```bash
$ git clone https://gerrit.onosproject.org/onos
```

2. Add ONOS developer environment to your bash profile, no need to do this step again if you had done this before
```bash
$ cd onos
$ cat << EOF >> ~/.bash_profile
export ONOS_ROOT="`pwd`"
source $ONOS_ROOT/tools/dev/bash_profile
EOF
$ . ~/.bash_profile
```

3. Build ONOS with Buck
```bash
$ cd $ONOS_ROOT
$ onos-buck build onos [--show-output]
```

ONOS currently uses a modified version of Buck (`onos-buck`), which has been packaged with ONOS. Please use this version until our changes have been upstreamed and released as part of an official Buck release. 

This will compile all source code assemble the installable onos.tar.gz, which is located in the buck-out directory. Note the --show-output option, which can be omitted, will display the path to this file.


### Start ONOS on local machine

To run ONOS locally on the development machine, simply run the following command:

```bash
$ onos-buck run onos-local [-- [clean] [debug]]
```

or simplier one:

```bash
$ ok [clean] [debug]
```

The above command will create a local installation from the onos.tar.gz file (re-building it if necessary) and will start the ONOS server in the background.
In the foreground, it will display a continuous view of the ONOS (Apache Karaf) log file.
Options following the double-dash (–) are passed through to the ONOS Apache Karaf and can be omitted.
Here, the `clean` option forces a clean installation of ONOS and the `debug` option means that the default debug port 5005 will be available for attaching a remote debugger.

### Interacting with ONOS

To access ONOS UI, use browser to open [http://localhost:8181/onos/ui](http://localhost:8181/onos/ui) or use `onos-gui localhost` command

The default username and password is **onos/rocks**

To attach to the ONOS CLI console, run:

```bash
$ onos localhost
```

### Unit Tests

To run ONOS unit tests, including code Checkstyle validation, run the following command:

```bash
$ onos-buck test
```

Or more specific tests:

```bash
$ onos-buck test [buck-test-rule]
```

## Contributing

ONOS code is hosted and maintained using [Gerrit](https://gerrit.onosproject.org/).

Code on GitHub is only a mirror. The ONOS project does **NOT** accept code through pull requests on GitHub. 

To contribute to ONOS, please refer to [Sample Gerrit Workflow](https://wiki.onosproject.org/display/ONOS/Sample+Gerrit+Workflow). It should includes most of the things you'll need to get your contribution started!


## More information

For more information, please check out our wiki page or mailing lists:

* [Wiki](https://wiki.onosproject.org/)
* [Google group](https://groups.google.com/a/onosproject.org/forum/#!forum/onos-dev)
* [Slack](https://onosproject.slack.com)

## License

ONOS (Open Network Operating System) is published under [Apache License 2.0](https://github.com/opennetworkinglab/onos/blob/master/LICENSE.txt)