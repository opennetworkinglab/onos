#!/usr/bin/env python

"""
"""
from mininet.topo import Topo

class DualTopo( Topo ):
    """Switches and Dual-homed host"""

    def __init__( self ):
        """Create a topology."""

        # Initialize Topology
        Topo.__init__( self )

        # add nodes, switches first...
        LONDON = self.addSwitch( 's1' )
        BRISTL = self.addSwitch( 's2' )

        # ... and now hosts
        LONDON_host = self.addHost( 'h1' )

        # add edges between switch and corresponding host
        self.addLink( LONDON, LONDON_host )
        self.addLink( BRISTL, LONDON_host )

        # add edges between switches
        self.addLink( LONDON, BRISTL, bw=10, delay='1.0ms')


topos = { 'dual': ( lambda: DualTopo() ) }

if __name__ == '__main__':
    from onosnet import run
    run( DualTopo() )
