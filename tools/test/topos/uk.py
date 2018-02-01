#!/usr/bin/env python

"""
"""
from mininet.topo import Topo

class UkTopo( Topo ):
    """Switches projected onto the UK map"""

    def addSwitch( self, name, **opts ):
        kwargs = { 'protocols' : 'OpenFlow13' }
        kwargs.update( opts )
        return super(UkTopo, self).addSwitch( name, **kwargs )

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
        self.addLink( BIRMHM, LIVRPL )
        self.addLink( BIRMHM, YORK )
        self.addLink( BRISTL, ABYSTW )
        self.addLink( BRISTL, BIRMHM )
        self.addLink( BRISTL, PLYMTH )
        self.addLink( DOVER,  BRGHTN )
        self.addLink( DOVER,  NRWICH )
        self.addLink( LIVRPL, ABYSTW )
        self.addLink( LIVRPL, EDBUGH )
        self.addLink( LONDON, BIRMHM )
        self.addLink( LONDON, BRGHTN )
        self.addLink( LONDON, BRISTL )
        self.addLink( LONDON, BRISTL )
        self.addLink( LONDON, DOVER )
        self.addLink( LONDON, NRWICH )
        self.addLink( LONDON, PLYMTH )
        self.addLink( LONDON, YORK )
        self.addLink( LONDON, YORK )
        self.addLink( NWCSTL, EDBUGH )
        self.addLink( NWCSTL, LIVRPL )
        self.addLink( NWCSTL, YORK )
        self.addLink( YORK,   LIVRPL )
        self.addLink( YORK,   NRWICH )

topos = { 'uk': ( lambda: UkTopo() ) }

if __name__ == '__main__':
    from onosnet import run
    run( UkTopo() )
