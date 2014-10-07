#!/usr/bin/env python
from mininet.cli import CLI
from mininet.net import Mininet
from mininet.node import RemoteController, OVSKernelSwitch

MAC = 12
DPID = 16

def string_to_hex(s, length):
    """ Convert a string like 00:00 in to hex 0x0000 format"""
    tmp = '{0:#x}'.format(int(s.replace(':', '').lstrip('0'),length))
    return tmp

def hex_to_string(h, length):
    """Convert a hex number from 0x0000 to 00:00 format"""
    tmp = h.lstrip('0x').zfill(length)
    tmp = ':'.join(a+b for a,b in zip(tmp[::2], tmp[1::2]))
    return tmp

class Tower(object):
    """ Create a tower topology from semi-scratch in Mininet """

    def __init__(self, cname='flare', cip='15.255.126.183', k=4, h=6, 
                 proto=None):
        """Create tower topology for mininet
            cname: controller name
            cip: controller ip
            k: number of leaf switches
            h: number of hosts perl leaf switch
        """

        # We are creating the controller with local-loopback on purpose to avoid
        # having the switches connect immediately. Instead, we'll set controller
        # explicitly for each switch after configuring it as we want.
        self.flare = RemoteController(cname, '127.0.0.1', 6633)
        self.net = Mininet(controller=self.flare, switch = OVSKernelSwitch, 
                           build=False)

        self.cip = cip
        self.spines = []
        self.leaves = []
        self.hosts = []
        self.proto = proto

        # Create the two spine switches
        self.spines.append(self.net.addSwitch('s1'))
        self.spines.append(self.net.addSwitch('s2'))

        # Create two links between the spine switches
        self.net.addLink(self.spines[0], self.spines[1])
        self.net.addLink(self.spines[1], self.spines[0])
        
        # Now create the leaf switches, their hosts and connect them together
        i = 1
        c = 0
        while i <= k:
            self.leaves.append(self.net.addSwitch('s1%d' % i))
            for spine in self.spines:
                self.net.addLink(self.leaves[i-1], spine)

            j = 1
            while j <= h:
                self.hosts.append(self.net.addHost('h%d%d' % (i, j)))
                self.net.addLink(self.hosts[c], self.leaves[i-1])
                j+=1
                c+=1

            i+=1

    def run(self):
        """ Runs the created network topology and launches mininet cli"""
        self.run_silent()
        CLI(self.net)
        self.net.stop()

    def run_silent(self):
        """ Runs silently - for unit testing """
        self.net.build()

        # Start the switches, configure them with desired protocols and only 
        # then set the controller
        for sw in self.spines:
            sw.start([self.flare])
            if self.proto:
                sw.cmd('ovs-vsctl set bridge %(sw)s protocols=%(proto)s' % \
                           { 'sw': sw.name, 'proto': self.proto})
            sw.cmdPrint('ovs-vsctl set-controller %(sw)s tcp:%(ctl)s:6633' % \
                            {'sw': sw.name, 'ctl': self.cip})

        for sw in self.leaves:
            sw.start([self.flare])
            sw.cmdPrint('ovs-vsctl set-controller %(sw)s tcp:%(ctl)s:6633' % \
                            {'sw': sw.name, 'ctl': self.cip})

    def pingAll(self):
        """ PingAll to create flows - for unit testing """
        self.net.pingAll()

    def stop(self):
        "Stops the topology. You should call this after run_silent"
        self.net.stop()
