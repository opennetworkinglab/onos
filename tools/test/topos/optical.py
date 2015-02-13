#!/usr/bin/env python

''' file: optical.py '''

from mininet.topo import Topo
from mininet.node import RemoteController
from mininet.net import Mininet
from mininet.cli import CLI
from mininet.log import setLogLevel, info
from mininet.link import Intf, Link
from mininet.util import irange

class NullIntf( Intf ):
    "A dummy interface with a blank name that doesn't do any configuration"
    def __init__( self, name, **params ):
        self.name = ''

class NullLink( Link ):
    "A dummy link that doesn't touch either interface"
    def makeIntfPair( cls, intf1, intf2, addr1=None, addr2=None ):
        pass
    def delete( self ):
        pass

class OpticalTopo( Topo ):

    def addIntf( self, switch, intfName ):
        "Add intf intfName to switch"
        self.addLink( switch, switch, cls=NullLink,
                      intfName1=intfName, cls2=NullIntf, intfName2=intfName )

    def build( self, n=2, tapStart=3 ):

        # Add hosts and switches
        hosts = []
        switches = []
        for i in irange( 1, n ):
            h = self.addHost( 'h%d' % i )
            s = self.addSwitch( 's%d' % i, dpid="0000ffffffff%04d" % i )
            self.addLink( h, s )
            hosts.append( h )
            switches.append( s )

        # Add optical tap interfaces
        tapNum = tapStart
        for sw in switches:
            self.addIntf( sw, 'tap%d' % tapNum )
            tapNum += 1

# if you use, sudo mn --custom custom/optical.py, then register the topo:
#sudo mn --custom optical.py --topo optical,5
topos = { 'optical': OpticalTopo }

def installStaticFlows( net ):
    for sw in net.switches:
      info( 'Adding flows to %s...' % sw.name )
      sw.dpctl( 'add-flow', 'in_port=1,actions=output=2' )
      sw.dpctl( 'add-flow', 'in_port=2,actions=output=1' )
      info( sw.dpctl( 'dump-flows' ) )

def run( n ):
    topo = OpticalTopo( n )
    net = Mininet( topo=topo, controller=RemoteController, autoSetMacs=True )
    net.start()
    #installStaticFlows( net )
    CLI( net )
    net.stop()

# if the script is run directly (sudo custom/optical.py):
if __name__ == '__main__':
    import sys
    try:
        n = int( sys.argv[1] )
    except:
        print ( 'Usage: ./optical.py n    # n is number of switches\n'
                'Starting with default of 2 switches...\n' )
        n = 2
    setLogLevel( 'info' )
    run( n )
