#!/usr/bin/python

import sys
from threading import Thread

from mininet.net import Mininet
from mininet.log import setLogLevel
from mininet.node import RemoteController
from mininet.log import info, debug
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

            threads = [ self.threadPing(s, d) for (s, d) in zip( self.hosts, self.hosts[1:] + self.hosts[0:1] ) ]
            for t in threads:
                t.join()
            info ( '\n' )

    def threadPing( self, src, dst ):
        "Ping from src to dst in a thread"
        def p():
            src.cmd( 'ping -w 0.1 -W 0.1 -c1 ' + dst.IP() )
        t = Thread( target=p )
        info ( '%s ' % src.name )
        t.start()
        return t

    def pingloop( self ):
        "Loop forever pinging the full mesh of hosts"
        setLogLevel( 'error' )
        try:
            while True:
                self.ping()
        finally:
            setLogLevel( 'info' )

# Initialize ONOSMininet the first time that the class is loaded
ONOSMininet.setup()

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
