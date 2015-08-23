#!/usr/bin/env python
from mininet.cli import CLI
from mininet.net import Mininet
from mininet.node import RemoteController, OVSKernelSwitch

MAC = 12
DPID = 16

class CustomCLI(CLI):
    """Custom CLI to allow us to add our own commands."""

    def __init__ (self, net):
        """Init method for our custom CLI."""
        self.net = net
        CLI.__init__(self, net)

class Solar(object):
    """ Create a tiered topology from semi-scratch in Mininet """

    def __init__(self, cname='onos', cips=['192.168.56.1'], islands=3, edges=2, hosts=2):
        """Create tower topology for mininet"""

        # We are creating the controller with local-loopback on purpose to avoid
        # having the switches connect immediately. Instead, we'll set controller
        # explicitly for each switch after configuring it as we want.
        self.ctrls = [ RemoteController(cname, cip, 6653) for cip in cips ]
        self.net = Mininet(controller=RemoteController, switch = OVSKernelSwitch,
                           build=False)

        self.cips = cips
        self.spines = []
        self.leaves = []
        self.hosts = []
        for ctrl in self.ctrls:
            self.net.addController(ctrl)

        # Create the two core switches and links between them
        c1 = self.net.addSwitch('c1',dpid='1111000000000000')
        c2 = self.net.addSwitch('c2',dpid='2222000000000000')
        self.spines.append(c1)
        self.spines.append(c2)

        self.net.addLink(c1, c2)
        self.net.addLink(c2, c1)

        for i in range(1, islands + 1):
            sc = self.createSpineClump(i, edges, hosts)
            self.net.addLink(c1, sc[0])
            self.net.addLink(c2, sc[0])
            self.net.addLink(c1, sc[1])
            self.net.addLink(c2, sc[1])

    def createSpineClump(self, island, edges, hosts):
        """ Creates a clump of spine and edge switches with hosts"""
        s1 = self.net.addSwitch('s%1d1' % island,dpid='00000%1d0100000000' % island)
        s2 = self.net.addSwitch('s%1d2' % island,dpid='00000%1d0200000000' % island)
        self.net.addLink(s1, s2)
        self.net.addLink(s2, s1)

        for i in range(1, edges + 1):
            es = self.createEdgeSwitch(island, i, hosts)
            self.net.addLink(es, s1)
            self.net.addLink(es, s2)

        self.spines.append(s1)
        self.spines.append(s2)

        clump = []
        clump.append(s1)
        clump.append(s2)
        return clump

    def createEdgeSwitch(self, island, index, hosts):
        """ Creates an edge switch in an island and ads hosts to it"""
        sw = self.net.addSwitch('e%1d%1d' % (island, index),dpid='0000000%1d0000000%1d' % (island, index))
        self.leaves.append(sw)

        for j in range(1, hosts + 1):
            host = self.net.addHost('h%d%d%d' % (island, index, j),ip='10.%d.%d.%d' % (island, index, j))
            self.net.addLink(host, sw)
            self.hosts.append(host)
        return sw

    def run(self):
        """ Runs the created network topology and launches mininet cli"""
        self.net.build()
        self.net.start()
        CustomCLI(self.net)
        self.net.stop()

    def pingAll(self):
        """ PingAll to create flows - for unit testing """
        self.net.pingAll()

    def stop(self):
        "Stops the topology. You should call this after run_silent"
        self.net.stop()
