# ONOS-P4 Developer Virtual Machine

This directory contains files necessary to build and provision a VM to test and
develop ONOS support for P4 Runtime.

For more information on P4 support in ONOS please visit the following web page:
<https://wiki.onosproject.org/x/FYnV>

This document contains also instructions on how to download a pre-built VM.

## Contents

The VM is based on Ubuntu 16.04 (server) and contains the following software:

- ONOS
- BMv2 (P4 software switch with P4Runtime support)
- p4c (P4 compiler)
- Mininet (network emulator)

### Tutorial VM

It is possible to generate a variant of the VM to be used during tutorials. This
version of the VM comes with a Lubuntu desktop environment and various code
editors with P4 syntax highlighting (vim, Sublime Text, and Atom).

## Recommended system requirements

The VM is configured with 4 GB of RAM and 2 CPU cores (4 cores for the tutorial
variant). The disk has size of approx. 4 GB but expect to grow up to 8 GB when
building ONOS. For a flawless experience we recommend running the VM on a host
system that has at least the double of resources.

These are the recommended minimum requirements to be able to run a Mininet
network with 1-10 BMv2 devices controlled by 1 ONOS instance. To emulate larger
networks with multiple instances of ONOS (for example using
[`onos.py`](https://wiki.onosproject.org/x/GAOW)), we recommend configuring the
VM to use at least 4 CPU cores.

To modify the VM configuration you can either modify the
[Vagrantfile](./Vagrantfile) (look for `vb.cpus`) before starting the build
process, or use the VirtualBox VM settings after you have imported the
pre-built VM.

## Download a pre-built VM

Building the VM takes around 30-50 minutes, depending on your Internet
connection speed. If you would rather not wait, you can use the following link
to download an Open Virtual Appliance (OVA) package to be imported using
VirtualBox or any other x86 virtualization system that supports this format.

Pre-built OVA package (approx. 1.5 GB):
<http://onlab.vicci.org/onos/onos-p4-dev.ova>

The tutorial variant of the OVA package can be found here (approx 2.3 GB):
<http://onlab.vicci.org/onos/onos-p4-tutorial.ova>

### Login credentials

The VM comes with one user with sudo privileges named `sdn` with password `rocks`.
Use these credentials to log in the guest Ubuntu system.

## Build the VM

### Requirements

To build the VM you will need the following software installed in your host
machine:

- [Vagrant](https://www.vagrantup.com/) (tested v2.1.1)
- [VirtualBox](https://www.virtualbox.org/wiki/Downloads) (tested with v5.2.10)

Optionally, to export the VM as an OVA package you will also need
[sshpass](https://gist.github.com/arunoda/7790979).

### Build using Vagrant

The VM can be generated locally using Vagrant. In a terminal window type:

```bash
cd $ONOS_ROOT/tools/dev/p4vm
vagrant up
```

Once Vagrant has provisioned the VM, you can access to it using the `vagrant
ssh` command. However, this command will log in to the guest Ubuntu shell with
the default `vagrant` user. To use ONOS and the other P4 tools, we suggest using
the `sdn` user. Once you are able to access the VM using `vagrant ssh`, use the
following command to switch to the `sdn` user:

```bash
sudo su sdn
```

### Export as OVA package 

It is possible to generate an OVA package to distribute a pre-built VM.
To generate the OVA file, in a terminal window type the following commands:

```bash
cd $ONOS_ROOT/tools/dev/p4vm
./export-ova.sh
```

This script will:

1. provision the VM using Vagrant;
2. remove the `vagrant` user;
3. reduce VM disk size (by removing build artifacts);
4. generate a file named `onos-p4-dev.ova`.

### Building the tutorial VM

To build the tutorial VM, simply set the environment variable `P4_VM_TYPE` to `tutorial` before building.

For example:

```bash
P4_VM_TYPE=tutorial vagrant up
```

In alternative, to generate the OVA package:

```bash
P4_VM_TYPE=tutorial ./export-ova.sh
```

