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
        SWA = self.addSwitch( 's1' )
        SWB = self.addSwitch( 's2' )

        # ... and now hosts
        HOSTX = self.addHost( 'h1' )

        # add edges between switch and corresponding host
        self.addLink( SWA, HOSTX )
        self.addLink( SWB, HOSTX )

        # add edges between switches
        self.addLink( SWA, SWB, bw=10, delay='1.0ms' )


topos = { 'dual': ( lambda: DualTopo() ) }

if __name__ == '__main__':
    from onosnet import run
    run( DualTopo() )
