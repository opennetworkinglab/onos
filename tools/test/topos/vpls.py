#!/usr/bin/env python

from mininet.cli import CLI
from mininet.node import Link, Host
from mininet.net import Mininet
from mininet.node import RemoteController
from mininet.term import makeTerm
from mininet.topo import Topo
from functools import partial

class VLANHost( Host ):
    "Host connected to VLAN interface"

    def config( self, vlan=10, **params ):
        """Configure VLANHost according to (optional) parameters:
           vlan: VLAN ID for default interface"""

        r = super( VLANHost, self ).config( **params )

        intf = self.defaultIntf()
        # remove IP from default, "physical" interface
        self.cmd( 'ifconfig %s inet 0' % intf )
        # create VLAN interface
        self.cmd( 'vconfig add %s %d' % ( intf, vlan ) )
        # assign the host's IP to the VLAN interface
        self.cmd( 'ifconfig %s.%d inet %s' % ( intf, vlan, params['ip'] ) )
        # update the intf name and host's intf map
        newName = '%s.%d' % ( intf, vlan )
        # update the (Mininet) interface to refer to VLAN interface name
        intf.name = newName
        # add VLAN interface to host's name to intf map
        self.nameToIntf[ newName ] = intf

        return r

class VplsTopo(Topo):
    ''' VPLS demo Topology '''

    def __init__(self):
        Topo.__init__(self)

        s1 = self.addSwitch('s1')
        s2 = self.addSwitch('s2')
        s3 = self.addSwitch('s3')
        s4 = self.addSwitch('s4')
        s5 = self.addSwitch('s5')
        s6 = self.addSwitch('s6')

        h1 = self.addHost('h1', cls=VLANHost, vlan=100, mac='00:00:00:00:00:01')
        h2 = self.addHost('h2', cls=VLANHost, vlan=200, mac='00:00:00:00:00:02')
        h3 = self.addHost('h3', cls=VLANHost, vlan=300, mac='00:00:00:00:00:03')
        h4 = self.addHost('h4', cls=VLANHost, vlan=400, mac='00:00:00:00:00:04')
        h5 = self.addHost('h5', mac='00:00:00:00:00:05')
        h6 = self.addHost('h6', mac='00:00:00:00:00:06')

        self.addLink(s1, h1, port1=1, port2=0)
        self.addLink(s2, h2, port1=1, port2=0)
        self.addLink(s3, h3, port1=1, port2=0)
        self.addLink(s4, h4, port1=1, port2=0)
        self.addLink(s5, h5, port1=1, port2=0)
        self.addLink(s6, h6, port1=1, port2=0)

        self.addLink(s1, s2)
        self.addLink(s2, s3)
        self.addLink(s3, s4)
        self.addLink(s4, s1)
        self.addLink(s4, s2)
        self.addLink(s1, s5)
        self.addLink(s4, s5)
        self.addLink(s2, s6)
        self.addLink(s3, s6)

topos = { 'vpls': ( lambda: VplsTopo() ) }

if __name__ == '__main__':
    from onosnet import run
    run(VplsTopo())
