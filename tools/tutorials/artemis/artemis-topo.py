#!/usr/bin/python

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.cli import CLI
from mininet.log import setLogLevel, info, debug
from mininet.node import Host, RemoteController, OVSSwitch
import os

QUAGGA_DIR = '/usr/lib/quagga'
# Must exist and be owned by quagga user (quagga:quagga by default on Ubuntu)
QUAGGA_RUN_DIR = '/var/run/quagga'
EXABGP_RUN_EXE = '~/exabgp/sbin/exabgp'
CONFIG_DIR = 'configs/'

onos = RemoteController('onos', ip='192.168.0.1', port=6633)


class Onos(Host):

    def __init__(self, name, intfDict, *args, **kwargs):
        Host.__init__(self, name, *args, **kwargs)

        self.intfDict = intfDict

    def config(self, **kwargs):
        Host.config(self, **kwargs)

        for intf, attrs in self.intfDict.items():
            self.cmd('ip addr flush dev %s' % intf)
            if 'mac' in attrs:
                self.cmd('ip link set %s down' % intf)
                self.cmd('ip link set %s address %s' % (intf, attrs['mac']))
                self.cmd('ip link set %s up ' % intf)
            for addr in attrs['ipAddrs']:
                self.cmd('ip addr add %s dev %s' % (addr, intf))


class QuaggaRouter(Host):

    def __init__(self, name, quaggaConfFile, zebraConfFile, intfDict, *args, **kwargs):
        Host.__init__(self, name, *args, **kwargs)

        self.quaggaConfFile = quaggaConfFile
        self.zebraConfFile = zebraConfFile
        self.intfDict = intfDict

    def config(self, **kwargs):
        Host.config(self, **kwargs)
        self.cmd('sysctl net.ipv4.ip_forward=1')

        for intf, attrs in self.intfDict.items():
            self.cmd('ip addr flush dev %s' % intf)
            if 'mac' in attrs:
                self.cmd('ip link set %s down' % intf)
                self.cmd('ip link set %s address %s' % (intf, attrs['mac']))
                self.cmd('ip link set %s up ' % intf)
            for addr in attrs['ipAddrs']:
                self.cmd('ip addr add %s dev %s' % (addr, intf))

        self.cmd('/usr/lib/quagga/zebra -d -f %s -z %s/zebra%s.api -i %s/zebra%s.pid' %
                 (self.zebraConfFile, QUAGGA_RUN_DIR, self.name, QUAGGA_RUN_DIR, self.name))
        self.cmd('/usr/lib/quagga/bgpd -d -f %s -z %s/zebra%s.api -i %s/bgpd%s.pid' %
                 (self.quaggaConfFile, QUAGGA_RUN_DIR, self.name, QUAGGA_RUN_DIR, self.name))

    def terminate(self):
        self.cmd("ps ax | egrep 'bgpd%s.pid|zebra%s.pid' | awk '{print $1}' | xargs kill" % (
            self.name, self.name))

        Host.terminate(self)


class ExaBGPRouter(Host):

    def __init__(self, name, exaBGPconf, intfDict, *args, **kwargs):
        Host.__init__(self, name, *args, **kwargs)

        self.exaBGPconf = exaBGPconf
        self.intfDict = intfDict

    def config(self, **kwargs):
        Host.config(self, **kwargs)
        self.cmd('sysctl net.ipv4.ip_forward=1')

        for intf, attrs in self.intfDict.items():
            self.cmd('ip addr flush dev %s' % intf)
            if 'mac' in attrs:
                self.cmd('ip link set %s down' % intf)
                self.cmd('ip link set %s address %s' % (intf, attrs['mac']))
                self.cmd('ip link set %s up ' % intf)
            for addr in attrs['ipAddrs']:
                self.cmd('ip addr add %s dev %s' % (addr, intf))

        self.cmd('%s %s > /dev/null 2> exabgp.log &' % (EXABGP_RUN_EXE, self.exaBGPconf))

    def terminate(self):
        self.cmd(
            "ps ax | egrep 'lib/exabgp/application/bgp.py' | awk '{print $1}' | xargs kill")
        self.cmd(
            "ps ax | egrep 'server.py' | awk '{print $1}' | xargs kill")
        Host.terminate(self)


class ONOSSwitch(OVSSwitch):

    def start(self, controllers):
        return OVSSwitch.start(self, [onos])


class L2Switch(OVSSwitch):

    def start(self, controllers):
        return OVSSwitch.start(self, [])


class ArtemisTopo(Topo):
    "Artemis tutorial topology"

    def build(self):
        zebraConf = '%szebra.conf' % CONFIG_DIR

        quaggaConf = '%sR1-quagga.conf' % CONFIG_DIR
        name = 'R1'
        eth0 = {
            'ipAddrs': ['150.1.1.2/30']
        }
        eth1 = {
            'ipAddrs': ['10.0.0.1/8']
        }
        eth2 = {
            'ipAddrs': ['150.1.2.1/30']
        }
        intfs = {
            '%s-eth0' % name: eth0,
            '%s-eth1' % name: eth1,
            '%s-eth2' % name: eth2
        }
        r1 = self.addHost(name, cls=QuaggaRouter, quaggaConfFile=quaggaConf,
                          zebraConfFile=zebraConf, intfDict=intfs)

        quaggaConf = '%sR2-quagga.conf' % CONFIG_DIR
        name = 'R2'
        eth0 = {
            'ipAddrs': ['150.1.3.1/30']
        }
        eth1 = {
            'ipAddrs': ['150.1.2.2/30']
        }
        intfs = {
            '%s-eth0' % name: eth0,
            '%s-eth1' % name: eth1
        }
        r2 = self.addHost(name, cls=QuaggaRouter, quaggaConfFile=quaggaConf,
                          zebraConfFile=zebraConf, intfDict=intfs)

        quaggaConf = '%sR3-quagga.conf' % CONFIG_DIR
        name = 'R3'
        eth0 = {
            'ipAddrs': ['40.0.0.1/8']
        }
        eth1 = {
            'ipAddrs': ['150.1.1.1/30']
        }
        intfs = {
            '%s-eth0' % name: eth0,
            '%s-eth1' % name: eth1
        }
        r3 = self.addHost(name, cls=QuaggaRouter, quaggaConfFile=quaggaConf,
                          zebraConfFile=zebraConf, intfDict=intfs)

        quaggaConf = '%sR4-quagga.conf' % CONFIG_DIR
        name = 'R4'
        eth0 = {
            'ipAddrs': ['150.1.3.2/30'],
            'mac': 'e2:f5:32:16:9a:46'
        }
        eth1 = {
            'ipAddrs': ['10.10.10.1/24']
        }
        intfs = {
            '%s-eth0' % name: eth0,
            '%s-eth1' % name: eth1
        }
        r4 = self.addHost(name, cls=QuaggaRouter, quaggaConfFile=quaggaConf,
                          zebraConfFile=zebraConf, intfDict=intfs)

        ovs = self.addSwitch('ovs', dpid='00002a45d713e141', cls=ONOSSwitch)

        l2_switch = self.addSwitch(
            'l2_switch', dpid='0000000000000001', failMode='standalone', cls=L2Switch)

        h1 = self.addHost('h1', ip='10.0.0.100/8', defaultRoute='via 10.0.0.1')
        h4 = self.addHost('h4', ip='40.0.0.100/8', defaultRoute='via 40.0.0.1')

        # Set up the internal BGP speaker

        name = 'exabgp'
        eth0 = {
            'ipAddrs': ['10.0.0.3/8']
        }
        eth1 = {
            'ipAddrs': ['192.168.1.2/24']
        }
        intfs = {
            '%s-eth0' % name: eth0,
            '%s-eth1' % name: eth1
        }
        exabgp = self.addHost(name, cls=ExaBGPRouter,
                              exaBGPconf='%sexabgp.conf' % CONFIG_DIR,
                              intfDict=intfs)

        self.addLink(r1, r3, port1=0, port2=1)
        self.addLink(r1, l2_switch, port1=1, port2=2)
        self.addLink(r1, r2, port1=2, port2=1)

        self.addLink(ovs, r2, port1=2, port2=0)
        self.addLink(ovs, h4, port1=3, port2=0)
        self.addLink(ovs, r4, port1=4, port2=0)

        self.addLink(l2_switch, h1, port1=1, port2=0)
        self.addLink(l2_switch, exabgp, port1=3, port2=0)

        name = 'onos'
        eth0 = {
            'ipAddrs': ['192.168.0.1/24']
        }
        eth1 = {
            'ipAddrs': ['10.10.10.2/24']
        }
        eth2 = {
            'ipAddrs': ['192.168.1.1/24']
        }
        intfs = {
            '%s-eth0' % name: eth0,
            '%s-eth1' % name: eth1,
            '%s-eth2' % name: eth2
        }
        onos = self.addHost(name, inNamespace=False, cls=Onos, intfDict=intfs)

        self.addLink(onos, ovs, port1=0, port2=1)
        self.addLink(onos, r4, port1=1, port2=1)
        self.addLink(onos, exabgp, port1=2, port2=1)

topos = {'artemis': ArtemisTopo}

if __name__ == '__main__':
    setLogLevel('debug')
    topo = ArtemisTopo()

    net = Mininet(topo=topo, build=False)
    net.addController(onos)
    net.build()
    net.start()

    CLI(net)

    net.stop()

    info("done\n")
