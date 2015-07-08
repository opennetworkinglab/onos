#!/usr/bin/env python

"""
"""
from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import RemoteController
from mininet.node import Node
from mininet.node import CPULimitedHost
from mininet.link import TCLink
from mininet.cli import CLI
from mininet.log import setLogLevel
from mininet.util import dumpNodeConnections

class ReactiveForwardingTestTopo( Topo ):
    "Internet Topology Zoo Specimen."

    def __init__( self ):
        "Create a topology."

        # Initialize Topology
        Topo.__init__( self )

        # add nodes, switches first...
        s1 = self.addSwitch( 's1' )
        s2 = self.addSwitch( 's2' )
        s3 = self.addSwitch( 's3' )
        s4 = self.addSwitch( 's4' )
        s5 = self.addSwitch( 's5' )
        s6 = self.addSwitch( 's6' )
        s7 = self.addSwitch( 's7' )
        s8 = self.addSwitch( 's8' )
        s9 = self.addSwitch( 's9' )

        # ... and now hosts
        h1 = self.addHost( 'h1' )
        h2 = self.addHost( 'h2' )
        h3 = self.addHost( 'h3' )
        h4 = self.addHost( 'h4' )

        # add edges between switch and corresponding host
        self.addLink( s1 , h1 )
        self.addLink( s2 , h2 )
        self.addLink( s3 , h3 )
        self.addLink( s4 , h4 )

        # add edges between switches
        self.addLink( s1 , s5 )
        self.addLink( s2 , s5 )
        self.addLink( s2 , s8 )
        self.addLink( s3 , s4 )
        self.addLink( s3 , s7 )
        self.addLink( s4 , s5 )
        self.addLink( s6 , s8 )
        self.addLink( s6 , s7 )
        self.addLink( s5 , s9 )
        self.addLink( s6 , s9 )

topos = { 'att': ( lambda: ReactiveForwardingTestTopo() ) }
