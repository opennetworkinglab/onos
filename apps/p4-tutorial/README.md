# ONOS+P4 Tutorial

This directory contains the source code and instructions to run the ONOS+P4
tutorial exercises.

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

For more information on the content of the VM, and minimum system requirements,
[click here](/tools/dev/p4vm/README.md).

### VM credentials

The VM comes with one user with sudo privileges named `sdn` with password `rocks`.

## Overview

### mytunnel.p4

These exercises are based on a simple P4 program called
[mytunnel.p4](./pipeconf/src/main/resources/mytunnel.p4) designed for this
tutorial.

To start, have a look a the P4 source code. Even if this is the first time you
see P4 code, the program has been commented to provide an understanding of the
pipeline behavior to anyone with basic programming and networking background
and an high level knowledge of P4. While checking the P4 program, try answering
to the following questions:

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

### MyTunnel Pipeconf

The `mytunnel.p4` program is provided to ONOS as part of a "pipeconf", along
with the Java implementations of some ONOS driver behaviors necessary to
control this pipeline.

The following Java classes are provided:

* [PipeconfFactory.java](./pipeconf/src/main/java/org/onosproject/p4tutorial/pipeconf/PipeconfFactory.java):
This class is declared as an OSGi component which is "activated" once the
pipeconf application is loaded in ONOS. The main purpose of this class is to
instantiate the Pipeconf object and register that with the corresponding service
in ONOS. This is where we associate ONOS driver behaviors with the pipeconf, and
also define the necessary pipeconf extensions to be able to program and control
a BMv2 switch via P4Runtime, namelly the BMv2 JSON configuration and the P4Info
file.

* [PipelineInterpreterImple.java](./pipeconf/src/main/java/org/onosproject/p4tutorial/pipeconf/PipelineInterpreterImpl.java):
Implementation of the `PipelineInterpreter` ONOS driver behavior. The main
purpose of this class is to provide a mapping between ONOS constructs and P4
program-specific ones, for example methods to map ONOS well-known header fields
and actions to those defined in the P4 program.

* [PortStatisticsDiscoveryImpl.java](./pipeconf/src/main/java/org/onosproject/p4tutorial/pipeconf/PipelineInterpreterImpl.java):
Implementation of the `PortStatisticsDiscovery` ONOS driver behavior. As the
name suggests, this behavior is used to report statistics on the switch ports to
ONOS, e.g. number of packets/bytes received and transmitted for each port. This
implementation works by reading the value of two P4 counters defined in
`mytunnel.p4`, `tx_port_counter` and `rx_port_counter`.

### MyTunnel App

This application is used to provide connectivity between each pair of hosts via
the MyTunnel protocol. The implementation can be found
[here](./mytunnel/src/main/java/org/onosproject/p4tutorial/mytunnel/MyTunnelApp.java).

The application works by registering an host listener with the ONOS Host
Service. Every time a new host is discovered, the application creates two
unidirectional tunnels between that host and any other host previously
discovered.

## Tutorial exercises

### Exercise 1

[Click here to go to this exercise instructions](./exercise-1.md)

This exercise shows how to start ONOS and Mininet with BMv2, it also
demonstrates connectivity between hosts using the pipeline-agnostic application
Reactive Forwarding, in combination with other well known ONOS services such as
Proxy ARP, Host Location Provider, and LLDP Link Discovery.

### Exercise 2

[Click here to go to this exercise instructions](./exercise-2.md)

Similar to exercise 1, but here connectivity between hosts is demonstrated using
pipeline-specific application "MyTunnel".
