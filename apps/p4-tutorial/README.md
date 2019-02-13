# ONOS+P4 Tutorial

Welcome to the ONOS+P4 tutorial! The goal of this tutorial is to learn how to
use ONOS to control P4-capable devices via P4Runtime, and how to write ONOS apps
to control custom data plane capabilities implemented in P4.

For help, please write to the mailing list
[brigade-p4@onosproject.org](mailto:brigade-p4@onosproject.org) or check the
[mailing list archives](https://groups.google.com/a/onosproject.org/forum/#!forum/brigade-p4).

## Tutorial VM

To complete the exercises, you will need to download and run the following VM
(in .ova format):

<http://onlab.vicci.org/onos/onos-p4-tutorial.ova>

To run the VM you can use any modern virtualization system, although we
recommend using VirtualBox. To download VirtualBox and import the VM use the
following links:

* <https://www.virtualbox.org/wiki/Downloads>
* <https://docs.oracle.com/cd/E26217_01/E26796/html/qs-import-vm.html>

For more information on the content of the VM and minimum system requirements,
[click here](../../tools/dev/p4vm/README.md).

### VM credentials

The VM comes with one user with sudo privileges. Use these credentials to log in:

* Username: `sdn`
* Password: `rocks`

## Overview

Before starting, we suggest to open the `$ONOS_ROOT/apps/p4-tutorial` directory
in your editor of choice for easier access to the different files used in the
exercise. For example, if using the Atom editor:

```
$ atom $ONOS_ROOT/apps/p4-tutorial/
```

### mytunnel.p4

These exercises are based on a simple P4 program called
[mytunnel.p4](pipeconf/src/main/resources/mytunnel.p4) designed for this
tutorial.

```
$ atom $ONOS_ROOT/apps/p4-tutorial/pipeconf/src/main/resources/mytunnel.p4
```

To start, have a look a the P4 program. Even if this is the first time you see
P4 code, the program has been commented to provide an understanding of the
pipeline behavior to anyone with basic programming and networking background.
While checking the P4 program, try answering the following questions:

* Which protocol headers are being extracted from each packet?
* How can the parser distinguish a packet with MyTunnel encapsulation from one
    without?
* How many match+action tables are defined in the P4 program?
* What is the first table in the pipeline applied to every packet?
* Which headers can be matched on table `t_l2_fwd`?
* Which type of match is applied to `t_l2_fwd`? E.g. exact match, ternary, or
    longest-prefix match?
* Which actions can be executed on matched packets?
* Which action can be used to send a packet to the controller?
* What happens if a matching entry is not found in table `t_l2_fwd`? What's the
    next table applied to the packet?

The answer to these questions is provided in the `questions.md` file in this
directory.

### MyTunnel Pipeconf

The `mytunnel.p4` program is provided to ONOS as part of a "pipeconf".

The main class used to implement the pipeconf is
[PipeconfFactory.java](pipeconf/src/main/java/org/onosproject/p4tutorial/pipeconf/PipeconfFactory.java).

```
$ atom $ONOS_ROOT/apps/p4-tutorial/pipeconf/src/main/java/org/onosproject/p4tutorial/pipeconf/PipeconfFactory.java
```

This class is declared as an OSGi runtime component which is "activated" once
the pipeconf app is loaded in ONOS. The main purpose of this class is to
instantiate the Pipeconf object and register that with the corresponding service
in ONOS. This is where we associate ONOS driver behaviors with the pipeconf
(look for the `buildPipeconf()` method), and also define the necessary pipeconf
extensions to be able to deploy the P4 program to a device.

This pipeconf contains:

* [mytunnel.json](pipeconf/src/main/resources/mytunnel.json):
The JSON configuration used to execute the P4 program on BMv2. This is an output
of the P4 compiler for BMv2.

* [mytunnel_p4info.txt](pipeconf/src/main/resources/mytunnel_p4info.txt):
P4Info file obtained from the P4 compiler.

* [PipelineInterpreterImpl.java](pipeconf/src/main/java/org/onosproject/p4tutorial/pipeconf/PipelineInterpreterImpl.java):
Implementation of the `PipelineInterpreter` ONOS driver behavior. The main
purpose of this class is to provide a mapping between ONOS constructs and P4
program-specific ones, for example methods to map ONOS well-known header fields
and packet forwarding/manipulation actions to those defined in the P4 program.
For a more detailed explanation of each method, check the
[PipelineInterpreter interface](../../core/api/src/main/java/org/onosproject/net/pi/model/PiPipelineInterpreter.java).

* [PortStatisticsDiscoveryImpl.java](pipeconf/src/main/java/org/onosproject/p4tutorial/pipeconf/PipelineInterpreterImpl.java):
Implementation of the `PortStatisticsDiscovery` ONOS driver behavior. As the
name suggests, this behavior is used to report statistics on the switch ports to
ONOS, e.g. number of packets/bytes received and transmitted for each port. This
implementation works by reading the value of two P4 counters defined in
`mytunnel.p4`, `tx_port_counter` and `rx_port_counter`.

### MyTunnel App

This app is used to provide connectivity between each pair of hosts via the
MyTunnel protocol, a non-standard tunneling protocol created for this exercise.
The implementation of this app can be found
[here](mytunnel/src/main/java/org/onosproject/p4tutorial/mytunnel/MyTunnelApp.java),
and it will be discussed in more details on Exercise 2.

## Tutorial exercises

### Exercise 1

[Click here to go to this exercise instructions](exercise-1.md)

This exercise shows how to start ONOS and Mininet with BMv2, it also
demonstrates connectivity between hosts using the pipeline-agnostic app
Reactive Forwarding, in combination with other well known ONOS services such as
Proxy ARP, Host Location Provider, and LLDP Link Discovery.

### Exercise 2

[Click here to go to this exercise instructions](exercise-2.md)

Similar to exercise 1, but here connectivity between hosts is demonstrated using
a pipeline-specific app "MyTunnel".
