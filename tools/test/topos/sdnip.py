#!/usr/bin/env python

from mininet.cli import CLI
from mininet.log import setLogLevel
from mininet.node import Link, Host
from mininet.net import Mininet
from mininet.node import RemoteController, OVSSwitch, OVSBridge
from mininet.term import makeTerm
from mininet.topo import Topo
from functools import partial
from routinglib import RoutingCli as CLI
from routinglib import AutonomousSystem, BasicAutonomousSystem, SdnAutonomousSystem
from routinglib import generateRoutes

class SdnIpTopo( Topo ):

    "SDN-IP topology"

    def __init__( self, onoses, **kwargs ):
        Topo.__init__( self, **kwargs )
        coreMesh = []

        # Create first 4 switches
        for i in range( 1, 5 ):
            coreMesh.append( self.addSwitch( 's%s' %i ) )

        # create full mesh between middle 4 switches
        remaining = list( coreMesh )
        while True:
            first = remaining[ 0 ]
            for switch in tuple( remaining ):
                if switch is not first:
                    self.addLink( switch, first )
            remaining.remove( first )
            if not remaining:
                break

        # Add more switches
        s5 = self.addSwitch( 's5', dpid='00:00:00:00:00:00:00:05' )
        s6 = self.addSwitch( 's6', dpid='00:00:00:00:00:00:00:06' )
        s7 = self.addSwitch( 's7', dpid='00:00:00:00:00:00:00:07' )
        s8 = self.addSwitch( 's8', dpid='00:00:00:00:00:00:00:08' )
        s9 = self.addSwitch( 's9', dpid='00:00:00:00:00:00:00:09' )
        s10 = self.addSwitch( 's10', dpid='00:00:00:00:00:00:00:0A' )

        # Add more links
        self.addLink( s5, s6 )
        self.addLink( s5, s8 )
        self.addLink( s6, s7 )
        self.addLink( s8, s9 )
        self.addLink( s9, s10 )
        self.addLink( coreMesh[ 0 ], s5 )
        self.addLink( coreMesh[ 0 ], s6 )
        self.addLink( coreMesh[ 0 ], s7 )
        self.addLink( coreMesh[ 1 ], s8 )
        self.addLink( coreMesh[ 1 ], s9 )
        self.addLink( coreMesh[ 1 ], s10 )
        self.addLink( coreMesh[ 2 ], s7 )
        self.addLink( coreMesh[ 3 ], s10 )
        
        # SDN AS        
        sdnAs = SdnAutonomousSystem(onoses, numBgpSpeakers=3, asNum=65000, externalOnos=True)
        cs0 = self.addSwitch('cs0', cls=OVSBridge)

        numRoutesPerAs = 32

        # Add external ASes
        as1 = BasicAutonomousSystem(1, generateRoutes(u'192.168.1.0/24', numRoutesPerAs))
        AutonomousSystem.addPeering(as1, sdnAs)
        AutonomousSystem.addPeering(as1, sdnAs, router2=3, intf1=2)
        as1.addLink(s5)
        as1.addLink(s6)
        as1.build(self)
        
        as2 = BasicAutonomousSystem(2, generateRoutes(u'192.168.2.0/24', numRoutesPerAs))
        AutonomousSystem.addPeering(as2, sdnAs)
        AutonomousSystem.addPeering(as2, sdnAs, router2=2)
        as2.addLink(s7)
        as2.build(self)
        
        as3 = BasicAutonomousSystem(3, generateRoutes(u'192.168.3.0/24', numRoutesPerAs))
        AutonomousSystem.addPeering(as3, sdnAs, router2=2)
        AutonomousSystem.addPeering(as3, sdnAs, router2=3)
        as3.addLink(s8)
        as3.build(self)
        
        as4 = BasicAutonomousSystem(4, generateRoutes(u'192.168.4.0/24', numRoutesPerAs), numRouters=2)
        AutonomousSystem.addPeering(as4, sdnAs)
        AutonomousSystem.addPeering(as4, sdnAs, router1=2, router2=3)
        as4.addLink(s9)
        as4.addLink(s10, router=2)
        as4.build(self)

        # add links between nets
        #self.addLink( BGP1, coreMesh[ 0 ], port2=10 )
        #self.addLink( BGP2, coreMesh[ 1 ], port2=10 )
        #self.addLink( BGP3, coreMesh[ 2 ], port2=10 )
        
        sdnAs.build(self, coreMesh[ 0 ], cs0)
        # TODO multihome the BGP speakers to different switches

topos = { 'sdnip': ( lambda: SdnIpTopo() ) }

if __name__ == '__main__':
    setLogLevel( 'debug' )
    from onosnet import run, parse_args
    run(SdnIpTopo(onoses=parse_args().ipAddrs))