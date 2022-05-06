# ONOS : Open Network Operating System


## What is ONOS?
ONOS is the only SDN controller platform that supports the transition from
legacy “brown field” networks to SDN “green field” networks. This enables
exciting new capabilities, and disruptive deployment and operational cost points
for network operators.

## Top-Level Features

* High availability through clustering and distributed state management.
* Scalability through clustering and sharding of network device control.
* Performance that is good for a first release, and which has an architecture
  that will continue to support improvements.
* Northbound abstractions for a global network view, network graph, and
  application intents.
* Pluggable southbound for support of OpenFlow, P4Runtime, and new or legacy
  protocols.
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

The following packages are required:

* git
* zip
* curl
* unzip
* python3 (needed by Bazel)

### Build ONOS from source

ONOS is built with [Bazel](https://bazel.build/), an open-source build tool
developed by Google. We suggest downloading and installing Bazel using the
[official instructions](https://docs.bazel.build/versions/master/install.html).

The minimum required Bazel version is 1.0.0

1. Clone the code from the ONOS Gerrit repository
```bash
$ git clone https://gerrit.onosproject.org/onos
```

2. Optionally, you can add the ONOS developer environment to your bash profile.
   This will provide access to a number of handy commands to run, test and debug
   ONOS. No need to do this step again if you had done this before:
```bash
$ cd onos
$ cat << EOF >> ~/.bash_profile
export ONOS_ROOT="`pwd`"
source $ONOS_ROOT/tools/dev/bash_profile
EOF
$ . ~/.bash_profile
```

3. Build ONOS with Bazel
```bash
$ cd $ONOS_ROOT
$ bazel build onos
```

### Start ONOS on local machine

To run ONOS locally on the development machine, simply run the following command:

```bash
$ bazel run onos-local [-- [clean] [debug]]
```

Or simpler one, if you have added the ONOS developer environment to your bash
profile:

```bash
$ ok [clean] [debug]
```

The above command will create a local installation from the ONOS tarbal
(re-building if necessary) and will start the ONOS server in the background. In
the foreground, it will display a continuous view of the ONOS (Apache Karaf) log
file. Options following the double-dash (–) are passed through to the ONOS
Apache Karaf and can be omitted. Here, the `clean` option forces a clean
installation, removing any state from previous executions. The `debug` option
means that the default debug port 5005 will be available for attaching a remote
debugger.

### Interacting with ONOS

To access ONOS UI, use a browser to open:

[http://localhost:8181/onos/ui](http://localhost:8181/onos/ui)

Or simpler, use the `onos-gui localhost` command.

The default username and password is `onos`/`rocks`.

To attach to the ONOS CLI console, run:

```bash
$ onos localhost
```

### Unit Tests

To run ONOS unit tests, including code Checkstyle validation, run the following
command:

```bash
$ bazel query 'tests(//...)' | xargs bazel test
```

Or better yet, to run code Checkstyle and all unit tests use the following
convenience alias:

```bash
$ ot
```

## Contributing

ONOS code is hosted and maintained using [Gerrit](https://gerrit.onosproject.org/).

Code on GitHub is only a mirror. The ONOS project does **NOT** accept code
through pull requests on GitHub.

To contribute to ONOS, please refer to [Sample Gerrit
Workflow](https://wiki.onosproject.org/display/ONOS/Sample+Gerrit+Workflow). It
should include most of the things you'll need to get your contribution started!

## More information

For more information, please check out our wiki page or mailing lists:

* [Wiki](https://wiki.onosproject.org/)
* [Google group](https://groups.google.com/a/onosproject.org/forum/#!forum/onos-dev)
* [Slack](https://onosproject.slack.com)

## License

ONOS (Open Network Operating System) is published under [Apache License
2.0](https://github.com/opennetworkinglab/onos/blob/master/LICENSE.txt)

## Acknowledgements
YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of [YourKit Java Profiler](https://www.yourkit.com/java/profiler/), [YourKit .NET Profiler](https://www.yourkit.com/.net/profiler/) and [YourKit YouMonitor](https://www.yourkit.com/youmonitor/).

![YourKit](https://www.yourkit.com/images/yklogo.png)
