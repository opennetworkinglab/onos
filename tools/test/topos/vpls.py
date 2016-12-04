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

        vpls1h1 = self.addHost('vpls1h1', cls=VLANHost, vlan=10, mac='00:00:00:00:00:01')
        vpls1h2 = self.addHost('vpls1h2', cls=VLANHost, vlan=10, mac='00:00:00:00:00:02')
        vpls1h3 = self.addHost('vpls1h3', cls=VLANHost, vlan=20, mac='00:00:00:00:00:03')
        vpls2h1 = self.addHost('vpls2h1', cls=VLANHost, vlan=30, mac='00:00:00:00:00:04')
        vpls2h2 = self.addHost('vpls2h2', cls=VLANHost, vlan=40, mac='00:00:00:00:00:05')
        vpls2h3 = self.addHost('vpls2h3', mac='00:00:00:00:00:06')
        vpls3h1 = self.addHost('vpls3h1', mac='00:00:00:00:00:07')
        vpls3h2 = self.addHost('vpls3h2', mac='00:00:00:00:00:08')

        self.addLink(s1, vpls1h1, port1=1, port2=0)
        self.addLink(s2, vpls2h2, port1=1, port2=0)
        self.addLink(s3, vpls1h3, port1=1, port2=0)
        self.addLink(s3, vpls3h1, port1=2, port2=0)
        self.addLink(s4, vpls1h2, port1=1, port2=0)
        self.addLink(s4, vpls2h1, port1=2, port2=0)
        self.addLink(s4, vpls2h3, port1=3, port2=0)
        self.addLink(s5, vpls3h2, port1=1, port2=0)

        self.addLink(s1, s4)
        self.addLink(s1, s2)
        self.addLink(s2, s4)
        self.addLink(s2, s3)
        self.addLink(s3, s4)
        self.addLink(s3, s5)

topos = { 'vpls': ( lambda: VplsTopo() ) }

if __name__ == '__main__':
    from onosnet import run
    run(VplsTopo())
