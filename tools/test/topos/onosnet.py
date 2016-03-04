#!/usr/bin/python

import sys
import itertools
from time import sleep

from mininet.net import Mininet
from mininet.log import setLogLevel
from mininet.node import RemoteController
from mininet.log import info, debug, output
from mininet.util import quietRun
from mininet.link import TCLink
from mininet.cli import CLI

class ONOSMininet( Mininet ):

    @classmethod
    def setup( cls ):
        cls.useArping = True if quietRun( 'which arping' ) else False

    def __init__( self, controllers=[], gratuitousArp=True, build=True, *args, **kwargs ):
        """Create Mininet object for ONOS.
        controllers: List of controller IP addresses
        gratuitousArp: Send an ARP from each host to aid controller's host discovery"""
        # discarding provided controller (if any),
        # using list of remote controller IPs instead
        kwargs[ 'controller' ] = None

        # delay building for a second
        kwargs[ 'build' ] = False

        Mininet.__init__(self, *args, **kwargs )

        self.gratArp = gratuitousArp
        self.useArping = ONOSMininet.useArping

        info ( '*** Adding controllers\n' )
        ctrl_count = 0
        for controllerIP in controllers:
            self.addController( 'c%d' % ctrl_count, RemoteController, ip=controllerIP )
            info( '   c%d (%s)\n' % ( ctrl_count, controllerIP ) )
            ctrl_count = ctrl_count + 1

        if self.topo and build:
            self.build()

    def start( self ):
        Mininet.start( self )
        if self.gratArp:
            self.waitConnected()
            info ( '*** Sending a gratuitious ARP from each host\n' )
            self.gratuitousArp()


    def gratuitousArp( self ):
        "Send an ARP from each host to aid controller's host discovery; fallback to ping if necessary"
        if self.useArping:
            for host in self.hosts:
                info( '%s ' % host.name )
                debug( host.cmd( 'arping -U -c 1 ' + host.IP() ) )
            info ( '\n' )
        else:
            info( '\nWARNING: arping is not found, using ping instead.\n'
                  'For higher performance, install arping: sudo apt-get install iputils-arping\n\n' )

            procs = [ s.popen( 'ping -w 0.1 -W 0.1 -c1 %s > /dev/null; printf "%s "'
                                % ( d.IP(), s.name ), shell=True )
                      for (s, d) in zip( self.hosts, self.hosts[1:] + self.hosts[0:1] ) ]
            for t in procs:
                out, err = t.communicate()
                if err:
                    info ( err )
                else:
                    info ( out )
        info ( '\n' )

    def pingloop( self ):
        "Loop forever pinging the full mesh of hosts"
        setLogLevel( 'error' )
        try:
            while True:
                self.ping()
        finally:
            setLogLevel( 'info' )

    def bgIperf( self, hosts=[], seconds=10 ):
        #TODO check if the hosts are strings or objects
        #    h1 = net.getNodeByName('h1')
        servers = [ host.popen("iperf -s") for host in hosts ]

        clients = []
        for pair in itertools.combinations(hosts, 2):
            info ( '%s <--> %s\n' % ( pair[0].name, pair[1].name ))
            cmd = "iperf -c %s -t %s" % (pair[1].IP(), seconds)
            clients.append(pair[0].popen(cmd))

        progress( seconds )

        for c in clients:
            out, err = c.communicate()
            if err:
                info( err )
            else:
                debug( out )
                #TODO parse output and print summary

        for s in servers:
            s.terminate()

def progress(t):
    while t > 0:
        sys.stdout.write( '.' )
        t -= 1
        sys.stdout.flush()
        sleep(1)
    print

# Initialize ONOSMininet the first time that the class is loaded
ONOSMininet.setup()

def do_iperf( self, line ):
    args = line.split()
    if not args:
        output( 'Provide a list of hosts.\n' )
    hosts = []
    err = False
    for arg in args:
        if arg not in self.mn:
            err = True
            error( "node '%s' not in network\n" % arg )
        else:
            hosts.append( self.mn[ arg ] )
    if "bgIperf" in dir(self.mn) and not err:
        self.mn.bgIperf( hosts )

def do_gratuitousArp( self, _line ):
    if "gratuitousArp" in dir(self.mn):
        self.mn.gratuitousArp()
    else:
        output( 'Gratuitous ARP is not support.\n' )

CLI.do_bgIperf = do_iperf
CLI.do_gratuitousArp = do_gratuitousArp

def run( topo, controllers=None, link=TCLink, autoSetMacs=True ):
    if not controllers and len( sys.argv ) > 1:
        controllers = sys.argv[ 1: ]
    else:
        print 'Need to provide a topology and list of controllers'
        exit( 1 )

    setLogLevel( 'info' )

    net = ONOSMininet( topo=topo, controllers=controllers, link=link, autoSetMacs=autoSetMacs )
    net.start()
    CLI( net )
    net.stop()
