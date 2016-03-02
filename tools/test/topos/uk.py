#!/usr/bin/env python

"""
"""
from mininet.topo import Topo

class UkTopo( Topo ):
    """Switches projected onto the UK map"""

    def __init__( self ):
        """Create a topology."""

        # Initialize Topology
        Topo.__init__( self )

        # add nodes, switches first...
        LONDON = self.addSwitch( 's1' )
        BRISTL = self.addSwitch( 's2' )
        BIRMHM = self.addSwitch( 's3' )
        PLYMTH = self.addSwitch( 's4' )
        DOVER  = self.addSwitch( 's5' )
        BRGHTN = self.addSwitch( 's6' )
        LIVRPL = self.addSwitch( 's7' )
        YORK   = self.addSwitch( 's8' )
        NWCSTL = self.addSwitch( 's9' )
        NRWICH = self.addSwitch( 's10' )
        EDBUGH = self.addSwitch( 's11' )
        ABYSTW = self.addSwitch( 's12' )

        # ... and now hosts
        LONDON_host = self.addHost( 'h1' )
        BRISTL_host = self.addHost( 'h2' )
        BIRMHM_host = self.addHost( 'h3' )
        PLYMTH_host = self.addHost( 'h4' )
        DOVER_host  = self.addHost( 'h5' )
        BRGHTN_host = self.addHost( 'h6' )
        LIVRPL_host = self.addHost( 'h7' )
        YORK_host   = self.addHost( 'h8' )
        NWCSTL_host = self.addHost( 'h9' )
        NRWICH_host = self.addHost( 'h10' )
        EDBUGH_host = self.addHost( 'h11' )
        ABYSTW_host = self.addHost( 'h12' )

        # add edges between switch and corresponding host
        self.addLink( LONDON, LONDON_host )
        self.addLink( BRISTL, BRISTL_host )
        self.addLink( BIRMHM, BIRMHM_host )
        self.addLink( PLYMTH, PLYMTH_host )
        self.addLink( DOVER,  DOVER_host )
        self.addLink( BRGHTN, BRGHTN_host )
        self.addLink( LIVRPL, LIVRPL_host )
        self.addLink( YORK,   YORK_host )
        self.addLink( NWCSTL, NWCSTL_host )
        self.addLink( NRWICH, NRWICH_host )
        self.addLink( EDBUGH, EDBUGH_host )
        self.addLink( ABYSTW, ABYSTW_host )

        # add edges between switches
        self.addLink( BIRMHM, LIVRPL, bw=10, delay='1.0ms')
        self.addLink( BIRMHM, YORK,   bw=10, delay='1.0ms')
        self.addLink( BRISTL, ABYSTW, bw=10, delay='1.0ms')
        self.addLink( BRISTL, BIRMHM, bw=10, delay='1.0ms')
        self.addLink( BRISTL, PLYMTH, bw=10, delay='1.0ms')
        self.addLink( DOVER,  BRGHTN, bw=10, delay='1.0ms')
        self.addLink( DOVER,  NRWICH, bw=10, delay='1.0ms')
        self.addLink( LIVRPL, ABYSTW, bw=10, delay='1.0ms')
        self.addLink( LIVRPL, EDBUGH, bw=10, delay='1.0ms')
        self.addLink( LONDON, BIRMHM, bw=10, delay='1.0ms')
        self.addLink( LONDON, BRGHTN, bw=10, delay='1.0ms')
        self.addLink( LONDON, BRISTL, bw=10, delay='1.0ms')
        self.addLink( LONDON, BRISTL, bw=10, delay='1.0ms')
        self.addLink( LONDON, DOVER,  bw=10, delay='1.0ms')
        self.addLink( LONDON, NRWICH, bw=10, delay='1.0ms')
        self.addLink( LONDON, PLYMTH, bw=10, delay='1.0ms')
        self.addLink( LONDON, YORK,   bw=10, delay='1.0ms')
        self.addLink( LONDON, YORK,   bw=10, delay='1.0ms')
        self.addLink( NWCSTL, EDBUGH, bw=10, delay='1.0ms')
        self.addLink( NWCSTL, LIVRPL, bw=10, delay='1.0ms')
        self.addLink( NWCSTL, YORK,   bw=10, delay='1.0ms')
        self.addLink( YORK,   LIVRPL, bw=10, delay='1.0ms')
        self.addLink( YORK,   NRWICH, bw=10, delay='1.0ms')

topos = { 'uk': ( lambda: UkTopo() ) }

if __name__ == '__main__':
    from onosnet import run
    run( UkTopo() )
