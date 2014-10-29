#!/usr/bin/env python

''' file: custom/optical.py '''

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.cli import CLI
from mininet.log import setLogLevel, info
from mininet.util import irange

class OpticalTopo( Topo ):

    def build( self, n=3, tapStart=3 ):

        # Add hosts and switches
        hosts = []
        switches = []
        for i in irange( 1, n ):
            h = self.addHost( 'h%d' % i )
            s = self.addSwitch( 's%d' % i )
            self.addLink( h, s )
            hosts.append( h )
            switches.append( s )

        # Add optical tap interfaces
        tapNum = tapStart
        for sw in switches:
            self.addLink( sw, sw, intfName1='%s-eth0' % sw, intfName2='tap%d' % tapNum )
            tapNum += 1

# if you use, sudo mn --custom custom/optical.py, then register the topo:
#sudo mn --custom optical-topo.py --topo optical,5
topos = { 'optical': OpticalTopo }

def installStaticFlows( net ):
    for swName in [ 's1', 's2', 's3', 's4', 's5', 's6' ]:
      info( 'Adding flows to %s...' % swName )
      sw = net[ swName ]
      sw.dpctl( 'add-flow', 'in_port=1,actions=output=2' )
      sw.dpctl( 'add-flow', 'in_port=2,actions=output=1' )
      info( sw.dpctl( 'dump-flows' ) )

def run():
    net = Mininet( topo=OpticalTopo() )
    net.start()
    #installStaticFlows( net )
    CLI( net )
    net.stop()

# if the script is run directly (sudo custom/optical.py):
if __name__ == '__main__':
    setLogLevel( 'info' )
    run()
