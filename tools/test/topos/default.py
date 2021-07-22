#!/usr/bin/env python

"""
"""
from mininet.topo import Topo

class AttMplsTopo( Topo ):
    "Internet Topology Zoo Specimen."

    def addSwitch( self, name, **opts ):
        kwargs = { 'protocols' : 'OpenFlow13' }
        kwargs.update( opts )
        return super(AttMplsTopo, self).addSwitch( name, **kwargs )

    def __init__( self ):
        "Create a topology."

        # Initialize Topology
        Topo.__init__( self )

        # add nodes, switches first...
        C1 = self.addSwitch( 's1' )
        C2 = self.addSwitch( 's2' )
        C3 = self.addSwitch( 's3' )

        # ... and now hosts
        CH1 = self.addHost( 'h1' )
        CH2 = self.addHost( 'h3' )
        CH3 = self.addHost( 'h3' )
        
        # add links between switch and corresponding host
        self.addLink( C3 , CH3 )
        self.addLink( C1 , CH1 )
        self.addLink( C2 , CH2 )

        # add links between hosts
        self.addLink( C2 , C1 )
        self.addLink( C3 , C1 )
        self.addLink( C3 , C2 )

topos = { 'att': ( lambda: AttMplsTopo() ) }

if __name__ == '__main__':
    from onosnet import run
    run( AttMplsTopo() )
