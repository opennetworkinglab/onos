#!/usr/bin/python

"""
multicluster.py: multiple ONOS clusters example

We create two ONOSClusters, "east" and "west", and
a LinearTopo data network where the first and second halves
of the network are connected to each ONOSCluster,
respectively.

The size of the ONOSCluster is determined by its
topology. In this example the topology is a
SingleSwitchTopo of size 1, so the "Cluster" is
actually a single node (for performance and
resource usage reasons.) However, it is possible
to use larger cluster sizes in a large (!) Mininet VM,
(e.g. 12 GB of RAM for two 3-node ONOS clusters.)

The MultiSwitch class is a customized version of
ONOSOVSSwitch that has a "controller" instance variable
(and parameter)
"""

from mininet.net import Mininet
from mininet.topo import LinearTopo, SingleSwitchTopo
from mininet.log import setLogLevel
from mininet.topolib import TreeTopo
from mininet.clean import cleanup

from onos import ONOSCluster, ONOSOVSSwitch, ONOSCLI, RenamedTopo


class MultiSwitch( ONOSOVSSwitch ):
    "Custom OVSSwitch() subclass that connects to different clusters"

    def __init__( self, *args, **kwargs ):
        "controller: controller/ONOSCluster to connect to"
        self.controller = kwargs.pop( 'controller', None )
        ONOSOVSSwitch.__init__( self, *args, **kwargs )

    def start( self, controllers ):
        "Start and connect to our previously specified controller"
        return ONOSOVSSwitch.start( self, [ self.controller ] )


def run():
    "Test a multiple ONOS cluster network"
    setLogLevel( 'info' )
    # East and west control network topologies (using RenamedTopo)
    # We specify switch and host prefixes to avoid name collisions
    # East control switch prefix: 'east_cs', ONOS node prefix: 'east_onos'
    # Each network is a renamed SingleSwitchTopo of size clusterSize
    # It's also possible to specify your own control network topology
    clusterSize = 1
    etopo = RenamedTopo( SingleSwitchTopo, clusterSize,
                         snew='east_cs', hnew='east_onos' )
    wtopo = RenamedTopo( SingleSwitchTopo, clusterSize,
                         snew='west_cs', hnew='west_onos' )
    # east and west ONOS clusters
    # Note that we specify the NAT node names to avoid name collisions
    east = ONOSCluster( 'east', topo=etopo, ipBase='192.168.123.0/24',
                        nat='enat0' )
    west = ONOSCluster( 'west', topo=wtopo, ipBase='192.168.124.0/24',
                        nat='wnat0' )
    # Data network topology
    topo = LinearTopo( 10 )
    # Create network
    net = Mininet( topo=topo, switch=MultiSwitch, controller=[ east, west ] )
    # Assign switches to controllers
    count = len( net.switches )
    for i, switch in enumerate( net.switches ):
        switch.controller = east if i < count/2 else west
    # Start up network
    net.start()
    ONOSCLI( net )  # run our special unified Mininet/ONOS CLI
    net.stop()

# Add a "controllers" command to ONOSCLI

def do_controllers( self, line ):
    "List controllers assigned to switches"
    cmap = {}
    for s in self.mn.switches:
        c = getattr( s, 'controller', None ).name
        cmap.setdefault( c, [] ).append( s.name )
    for c in sorted( cmap.keys() ):
        switches = ' '.join( cmap[ c ] )
        print '%s: %s' % ( c, switches )

ONOSCLI.do_controllers = do_controllers


if __name__ == '__main__':
    run()
