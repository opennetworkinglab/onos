#!/usr/bin/env python

"""
NOTES

To change onos log levels before start you can add something similar to
onos.py's ONOSNode.start method before starting onos service:
  # Change log levels
  self.ucmd( 'echo "log4j.logger.io.atomix= DEBUG" >> $ONOS_HOME/apache-karaf-*/etc/org.ops4j.pax.logging.cfg' )

"""

import argparse
from mininet.log import output, info, warn, error, debug, setLogLevel
from mininet.cli import CLI as origCLI
from mininet.net import Mininet
from mininet.topo import SingleSwitchTopo, Topo
from mininet.node import Host
from os.path import join
from glob import glob
import re
import json
from collections import deque
import hashlib
import onos  # onos.py

# Utility functions

def pause( net, msg, hint=False):
    """Reenter the CLI. Note that we use the mn base CLI class to allow
    extensibility and combination of custom files"""

    info( msg )

    if hint:
        help_msg = "Currently in the root Mininet net namespace...\n"
        help_msg += "To access control net functions use:\n"
        help_msg += "\tpx cnet=net.controllers[0].net\n"
        help_msg += "\tpy cnet.METHOD\n"
        help_msg += "To send commands to each onos node, use: onos_all CMD\n"
        help_msg += "\nBy default, ONOS nodes are running on the 192.168.123.X network\n"
        info( "%s\n" % help_msg )
    # NOTE: If we use onos.py as a custom file and as an imported module,
    #       we get two different sets of ONOS* classes. They don't play
    #       together and things don't work properly. Specifically the
    #       isinstance calls fail. This is due to import vs. exec calls
    onos.ONOSCLI( net )

def cprint( msg, color="default"):
    color=color.lower()
    colors = { 'cyan': '\033[96m', 'purple': '\033[95m',
               'blue': '\033[94m', 'green': '\033[92m',
               'yellow': '\033[93m', 'red': '\033[91m',
               'end': '\033[0m' }
    pre = colors.get( color, '' )
    output = pre + msg + colors['end']
    print( output )

def getNode( net, nodeId=0 ):
    "Helper function: return ONOS node, defaults to the first node"
    return net.controllers[ 0 ].nodes()[ nodeId ]

def onos_cli( net, line, nodeId=0 ):
    "Send command to ONOS CLI"
    c0 = net.controllers[ 0 ]
    # FIXME add this back after import onos.py works
    if isinstance( c0, onos.ONOSCluster ):
        # cmdLoop strips off command name 'onos'
        if line.startswith( ':' ):
            line = 'onos' + line
        node = getNode( net, nodeId )
        if line:
            line = '"%s"' % line
        cmd = 'client -h %s %s' % ( node.IP(), line )
        #node.cmdPrint( cmd )
        output  = node.cmd( cmd )
        info( line )
        # Remove verbose spam from output
        m = re.search( "unverified \{\} key: \{\}", output )
        if m:
            info( output[m.end():] )
        else:
            info( output )

def onos_all( net, line ):
    onosNodes = [ n for cluster in net.controllers for n in cluster.nodes() ]
    for node in range( len( onosNodes ) ):
        cprint( "*" * 53, "red" )
        cprint( "onos%s: %s" % ( str( node + 1 ), repr( onosNodes[ node ] ) ),
                "red" )
        cprint( "*" * 53, "red" )
        onos_cli( net, line, node )

# FIXME This needs a better name
def do_onos_all( self, line ):
    onos_all( self.mn, line)

# Add custom cli commands
# NOTE: This is so we can keep ONOSCLI and also add commands to it!
origCLI.do_onos_all = do_onos_all

# Test cases

def Partition( net ):
    # Controller net instance
    cnet = net.controllers[0].net

    info( "ONOS control network partition test\n")
    net.pingAll()
    if args.interactive:
        pause( net,  "~~~ Dropping into cli... Exit cli to continue test\n", True )

    onos_all( net, "nodes;partitions;partitions -c")
    info( "~~~ Right before the partitioned\n" )
    if args.interactive:
        pause( net, "Dropping into cli... Exit cli to continue test\n" )

    cs1 = cnet.switches[0]
    cs2 = cnet.switches[1]

    # PARTITION sub-clusters

    # we need to use names here
    cnet.configLinkStatus( cs1.name, cs2.name, "down" )
    onos_all( net, "nodes;partitions;partitions -c")
    info( "~~~ Right after cluster is partitioned. Next step is to heal the partition\n" )
    if args.interactive:
        pause( net, "Dropping into cli... Exit cli to continue test\n" )

    cnet.configLinkStatus( cs1.name, cs2.name, "up" )
    onos_all( net, "nodes;partitions;partitions -c")
    info( "~~~ Right after the partition is healed \n" )
    if args.interactive:
        pause( net, "Test is finished! Exit cli to exit test.\n" )

def Scaling( net ):

    def startNodes( net, nodes ):
        "start multiple ONOS nodes"
        cluster = net.controllers[0]
        cluster.activeNodes.extend( nodes )
        cluster.activeNodes = sorted( set( cluster.activeNodes ) )
        for node in nodes:
            node.shouldStart = True
            node.start( cluster.env, cluster.activeNodes )
        for node in nodes:
            node.waitStarted()

    # control net objects
    cluster = net.controllers[0]
    cnet = cluster.net
    cs1 = cnet.switches[0]

    info( "ONOS dynamic clustering scaling test\n")
    # Start the first node
    cluster.activeNodes.append( cnet.hosts[0] )
    cluster.activeNodes = sorted( set( cluster.activeNodes ) )
    startNodes( net, cluster.activeNodes )

    onos_all( net, "nodes;partitions;partitions -c")
    if args.interactive:
        pause( net, "Dropping into cli... Exit cli to continue test\n" )

    # Scale up by two
    while True:
        new = [ n for c in net.controllers for n in c.net.hosts if isinstance( n, DynamicONOSNode) and not n.started ][:2]
        if not new:
            break
        startNodes( net, new )
        onos_all( net, "nodes;partitions;partitions -c")
        if args.interactive:
            pause( net, "Dropping into cli... Exit cli to continue test\n" )

    # Scale down
    for i in range( len( cluster.activeNodes ) - 1 ):
        node = cluster.activeNodes.pop()
        node.genPartitions( cluster.activeNodes, node.metadata )
        onos_all( net, "nodes;partitions;partitions -c")
        if args.interactive:
            pause( net, "Dropping into cli... Exit cli to continue test\n" )
    if args.interactive:
        pause( net, "Test is finished! Exit cli to exit test.\n" )



# Mininet object subclasses

class HTTP( Host ):
    def __init__( self, *args, **kwargs ):
        super( HTTP, self).__init__( *args, **kwargs )
        self.dir = '/tmp/%s' % self.name
        self.cmd( 'rm -rf', self.dir )
        self.cmd( 'mkdir', self.dir )
        self.cmd( 'cd', self.dir )

    def start( self ):
        output( "(starting HTTP Server)" )
        # start python web server as a bg process
        self.cmd( 'python -m SimpleHTTPServer &> web.log &' )

    def stop( self ):
        # XXX is this ever called?
        print "Stopping HTTP Server..."
        print self.cmd( 'fg' )
        print self.cmd( '\x03' )  # ctrl-c


class DynamicONOSNode( onos.ONOSNode ):
    def __init__( self, *args, **kwargs ):
        self.shouldStart = False
        self.started = False
        self.metadata = '/tmp/cluster.json'
        super( DynamicONOSNode, self ).__init__( *args, **kwargs )
        # XXX HACK, need to get this passed in correctly
        self.alertAction = 'warn'

    def start( self, env, nodes=()):
        if not self.shouldStart:
            return
        elif self.started:
            return
        else:
            ##### Modified from base class
            env = dict( env )
            env.update( ONOS_HOME=self.ONOS_HOME )
            if self.remote:
                # Point onos to rewmote cluster metadata file
                ip = self.remote.get( 'ip', '127.0.0.1' )
                port = self.remote.get( 'port', '8000' )
                filename = self.remote.get( 'filename', 'cluster.json' )
                remote = 'http://%s:%s/%s' % ( ip, port, filename )
                uri = '-Donos.cluster.metadata.uri=%s' % remote
                prev = env.get( 'JAVA_OPTS', False )
                if prev:
                    jarg = ':'.join( [prev, uri] )
                else:
                    jarg = uri
                env.update( JAVA_OPTS=jarg )
            self.updateEnv( env )
            karafbin = glob( '%s/apache*/bin' % self.ONOS_HOME )[ 0 ]
            onosbin = join( self.ONOS_ROOT, 'tools/test/bin' )
            self.cmd( 'export PATH=%s:%s:$PATH' % ( onosbin, karafbin ) )
            self.cmd( 'cd', self.ONOS_HOME )
            self.ucmd( 'mkdir -p config ' )
            self.genPartitions( nodes, self.metadata )
            info( '(starting %s)' % self )
            service = join( self.ONOS_HOME, 'bin/onos-service' )
            self.ucmd( service, 'server 1>../onos.log 2>../onos.log'
                       ' & echo $! > onos.pid; ln -s `pwd`/onos.pid ..' )
            self.onosPid = int( self.cmd( 'cat onos.pid' ).strip() )
            self.warningCount = 0
            ####
            self.started=True

    def sanityCheck( self, lowMem=100000 ):
        if self.started:
            super( DynamicONOSNode, self ).sanityCheck( lowMem )

    def waitStarted( self ):
        if self.started:
            super( DynamicONOSNode, self ).waitStarted()

    def genPartitions( self, nodes, location='/tmp/cluster.json' ):
        """
        Generate a cluster metadata file for dynamic clustering.
        Note: name should be the same in different versions of the file as
              well as the number of partitions.
        """
        def genParts( nodes, k, parts=3):
            l = deque( nodes )
            perms = []
            for i in range( 1, parts + 1 ):
                part = {
                           'id': i,
                           'members': list(l)[:k]
                       }
                perms.append( part )
                l.rotate( -1 )
            return perms

        print "Generating %s with %s" % ( location, str(nodes) )
        port = 9876
        ips = [ node.IP() for node in nodes ]
        node = lambda k: { 'id': k, 'ip': k, 'port': port }
        m = hashlib.sha256( "Mininet based ONOS test" )
        name = int(m.hexdigest()[:8], base=16 )
        partitions = genParts( ips, 3 )
        data = {
                'name': name,
                'nodes': [ node(v) for v in ips ],
                'partitions': partitions
               }
        output = json.dumps( data, indent=4 )
        with open( location, 'w' ) as f:
            f.write( output )
        cprint( output, "yellow" )


class DynamicONOSCluster( onos.ONOSCluster ):
    def __init__( self, *args, **kwargs ):
        self.activeNodes = []
        # TODO: can we get super to use super's nodes()?
        super( DynamicONOSCluster, self ).__init__( *args, **kwargs )
        self.activeNodes = [ h for h in self.net.hosts if onos.isONOSNode( h ) ]
        onos.updateNodeIPs( self.env, self.nodes() )
        self.activeNodes = []

    def start( self ):
        "Start up ONOS control network"
        info( '*** ONOS_APPS = %s\n' % onos.ONOS_APPS )
        self.net.start()
        for node in self.net.hosts:
            if onos.isONOSNode( node ):
                node.start( self.env, self.nodes() )
            else:
                try:
                    node.start()
                except AttributeError:
                    # NAT doesn't have start?
                    pass
        info( '\n' )
        self.configPortForwarding( ports=self.forward, action='A' )
        self.waitStarted()
        return

    def nodes( self ):
        "Return list of ONOS nodes that should be running"
        return self.activeNodes

class HATopo( Topo ):
    def build( self, partitions=[], serverCount=1, dynamic=False, **kwargs ):
        """
        partitions  = a list of strings specifing the assignment of onos nodes
                      to regions. ['1', '2,3'] designates two regions, with
                      ONOS 1 in the first and ONOS 2 and 3 in the second.
        serverCount = If partitions is not given, then the number of ONOS
                      nodes to create
        dynamic     = A boolean indicating dynamic ONOS clustering
        """
        self.switchNum = 1
        if dynamic:
            cls = DynamicONOSNode
        else:
            cls = onos.ONOSNode
        if partitions:
            prev = None
            for partition in partitions:
                # Create a region of ONOS nodes connected to a switch
                # FIXME Check for nodes that are not assigned to a partition?
                cur = self.addRegion( partition, cls )

                # Connect switch to previous switch
                if prev:
                    self.addLink( prev, cur )
                prev = cur
        else:
            partition = ','.join( [ str( x ) for x in range( 1, serverCount + 1 ) ] )
            cs1 = self.addRegion( partition, cls )
        if dynamic:
            # TODO Pass these in
            scale = 2
            new = ','.join( [ str( x + 1 ) for x in range( serverCount , serverCount + scale ) ] )
            cs2 = self.addRegion( new, cls )
            self.addLink( cs1, cs2 )
            server = self.addHost( "server", cls=HTTP )
            for switch in self.switches():
                self.addLink( server, switch )

    def addRegion( self, partition, cls=onos.ONOSNode ):
        switch = self.addSwitch( 'cs%s' % self.switchNum )
        self.switchNum += 1
        for n in partition.split( ',' ):
            node = self.addHost( "onos" + str( n ), cls=cls )
            self.addLink( switch, node )
        return switch


CLI = onos.ONOSCLI

# The main runner
def runTest( args ):
    test = None
    if args.test == "partition":
        test=Partition
        serverCount = args.nodes
        # NOTE we are ignoring serverCount for this test, using partition assignment instead.
        topo = HATopo( partitions=args.partition )
        # FIXME Configurable dataplane topology
        net = Mininet( topo=SingleSwitchTopo( 3 ),
                       controller=[ onos.ONOSCluster( 'c0', topo=topo, alertAction='warn' ) ],
                       switch=onos.ONOSOVSSwitch )
    elif args.test == "scaling":
        test=Scaling
        serverCount = args.nodes
        topo = HATopo( serverCount=serverCount, dynamic=True )
        net = Mininet( topo=SingleSwitchTopo( 3 ),
                       controller=[ DynamicONOSCluster( 'c0', topo=topo, alertAction='warn' ) ],
                       switch=onos.ONOSOVSSwitch )
        cluster = net.controllers[0]
        cnet = cluster.net
        server = cnet.get( 'server' )
        remote = { 'ip': server.IP(),
                   'port': '8000',
                   'filename':'cluster.json' }
        for node in cnet.hosts:
            if isinstance( node, DynamicONOSNode ):
                node.metadata = '%s/cluster.json' % server.dir
                node.remote = remote
        ips = []
        cluster.activeNodes = [ cnet.get( "onos%s" % ( i + 1 ) ) for i in range( serverCount ) ]
        for node in cluster.activeNodes:
            node.shouldStart = True
    else:
        print "Incorrect test"
        return
    net.start()
    if args.interactive:
        CLI( net )
    test(net)
    CLI( net )
    net.stop()


if __name__ == '__main__':
    setLogLevel( 'info' )
    # Base parser
    parser= argparse.ArgumentParser(
            description='Mininet based HA tests for ONOS. For more detailed help on a test include the test option' )
    parser.add_argument(
            '-n', '--nodes', metavar="NODES", type=int, default=1,
            help="Number of nodes in the ONOS cluster" )
    parser.add_argument(
            '-i', '--interactive',# type=bool,
            default=False, action="store_true",
            help="Pause the test in between steps" )
    test_parsers=parser.add_subparsers( title="Tests", help="Types of HA tests", dest="test" )

    # Partition test parser
    partition_help = 'Network partition test. Each set of ONOS nodes is connected to their own switch in the control network. Partitions are introduced by removing links between control network switches.'
    partition_parser = test_parsers.add_parser(
            "partition", description=partition_help )
    partition_parser.add_argument(
            '-p', '--partition', metavar='Partition', required=True,
            type=str, nargs=2,
            help='Specify the membership for two partitions by node id. Nodes are comma separated and node count begins at 1. E.g. "1,3 2" will create a network with 3 ONOS nodes and two connected switches. Switch 1 will be connected to ONOS1 and ONOS3 while switch 2 will be connected to ONOS2. A partition will be created by disconnecting the two switches. All ONOS nodes will still be connected to the dataplane.' )

    # Dynamic scaling test parser
    # FIXME Replace with real values
    scaling_parser = test_parsers.add_parser( "scaling" )

    args = parser.parse_args()
    runTest( args )
