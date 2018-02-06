#!/usr/bin/env python

"""
"""
from mininet.topo import Topo

class Tower( Topo ):
    "Internet Topology Zoo Specimen."

    def addSwitch( self, name, **opts ):
        kwargs = { 'protocols' : 'OpenFlow13' }
        kwargs.update( opts )
        return super(Tower, self).addSwitch( name, **kwargs )

    def __init__( self ):
        "Create a topology."

        # Initialize Topology
        Topo.__init__( self )

        spines = []

        # Create the two spine switches
        spines.append(self.addSwitch( 's1' ))
        spines.append(self.addSwitch( 's2' ))

        # Now create the leaf switches, their hosts and connect them together
        for i in range(4):
            sn = i + 1
            leaf = self.addSwitch( 's1%d' % sn )
            for spine in spines:
                self.addLink(leaf, spine)

            for j in range(5):
                host = self.addHost( 'h%d%d' % (sn, j + 1) )
                self.addLink( host, leaf )

topos = { 'tower': ( lambda: Tower() ) }

if __name__ == '__main__':
    from onosnet import run
    run( Tower() )
