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

    def config( self, vlan=100, **params ):
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

        v100h1 = self.addHost('v100h1', cls=VLANHost, vlan=100, mac='00:00:00:00:00:01')
        v200h1 = self.addHost('v200h1', cls=VLANHost, vlan=200, mac='00:00:00:00:00:02')
        v300h1 = self.addHost('v300h1', cls=VLANHost, vlan=300, mac='00:00:00:00:00:03')
        v400h1 = self.addHost('v400h1', cls=VLANHost, vlan=400, mac='00:00:00:00:00:04')

        self.addLink(s1, v100h1, port1=1, port2=0)
        self.addLink(s2, v200h1, port1=1, port2=0)
        self.addLink(s3, v300h1, port1=1, port2=0)
        self.addLink(s4, v400h1, port1=1, port2=0)

        self.addLink(s1, s4)
        self.addLink(s1, s2)
        self.addLink(s2, s4)
        self.addLink(s2, s3)
        self.addLink(s3, s4)

topos = { 'vpls': ( lambda: VplsTopo() ) }

if __name__ == '__main__':
    from onosnet import run
    run(VplsTopo())
