#!/usr/bin/env python

"""
      [1] ----- [3] ----- [5]
       |   ____/ | \       |
       |  /      |  \____  |
       | /       |       \ |
      [2] ----- [4] ----- [6]
"""
from mininet.topo import Topo

class RegionABC( Topo ):
    """Simple 6 switch example"""

    def __init__( self ):
        """Create a topology."""

        # Initialize Topology
        Topo.__init__( self )

        # add nodes, switches first...
        S1 = self.addSwitch( 's1' )
        S2 = self.addSwitch( 's2' )
        S3 = self.addSwitch( 's3' )
        S4 = self.addSwitch( 's4' )
        S5 = self.addSwitch( 's5' )
        S6 = self.addSwitch( 's6' )

        # ... and now hosts
        S1_host = self.addHost( 'h1' )
        S2_host = self.addHost( 'h2' )
        S3_host = self.addHost( 'h3' )
        S4_host = self.addHost( 'h4' )
        S5_host = self.addHost( 'h5' )
        S6_host = self.addHost( 'h6' )

        # add edges between switch and corresponding host
        self.addLink( S1, S1_host )
        self.addLink( S2, S2_host )
        self.addLink( S3, S3_host )
        self.addLink( S4, S4_host )
        self.addLink( S5, S5_host )
        self.addLink( S6, S6_host )

        # add edges between switches as diagrammed above
        self.addLink( S1, S2, bw=10, delay='1.0ms')
        self.addLink( S1, S3, bw=10, delay='1.0ms')
        self.addLink( S2, S3, bw=10, delay='1.0ms')
        self.addLink( S2, S4, bw=10, delay='1.0ms')
        self.addLink( S3, S4, bw=10, delay='1.0ms')
        self.addLink( S3, S5, bw=10, delay='1.0ms')
        self.addLink( S3, S6, bw=10, delay='1.0ms')
        self.addLink( S4, S6, bw=10, delay='1.0ms')
        self.addLink( S5, S6, bw=10, delay='1.0ms')

topos = { 'regionabc': ( lambda: RegionABC() ) }

if __name__ == '__main__':
    from onosnet import run
    run( RegionABC() )
