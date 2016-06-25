#!/usr/bin/python

import sys
import itertools
import signal
from time import sleep
from threading import Thread

from mininet.net import Mininet
from mininet.log import setLogLevel
from mininet.node import RemoteController, Node
from mininet.log import info, debug, output, error
from mininet.link import TCLink
from mininet.cli import CLI

# This is the program that each host will call
import gratuitousArp
ARP_PATH = gratuitousArp.__file__.replace('.pyc', '.py')

class ONOSMininet( Mininet ):

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
            self.waitConnected( timeout=5 )
            info ( '*** Sending a gratuitious ARP from each host\n' )
            self.gratuitousArp()

    def verifyHosts( self, hosts ):
        for i in range( len( hosts ) ):
            if isinstance( hosts[i], str):
                if hosts[i] in self:
                    hosts[i] = self[ hosts[i] ]
                else:
                    info( '*** ERROR: %s is not a host\n' % hosts[i] )
                    del hosts[i]
            elif not isinstance( hosts[i], Node):
                del hosts[i]

    def gratuitousArp( self, hosts=[] ):
        "Send an ARP from each host to aid controller's host discovery; fallback to ping if necessary"
        if not hosts:
            hosts = self.hosts
        self.verifyHosts( hosts )

        for host in hosts:
            info( '%s ' % host.name )
            info( host.cmd( ARP_PATH ) )
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
        self.verifyHosts( hosts )
        servers = [ host.popen("iperf -s") for host in hosts ]

        clients = []
        for s, d in itertools.combinations(hosts, 2):
            info ( '%s <--> %s\n' % ( s.name, d.name ))
            cmd = 'iperf -c %s -t %s -y csv' % (d.IP(), seconds)
            p = s.popen(cmd)
            p.s = s.name
            p.d = d.name
            clients.append(p)

        def handler (_signum, _frame):
            raise BackgroundException()
        oldSignal = signal.getsignal(signal.SIGTSTP)
        signal.signal(signal.SIGTSTP, handler)

        def finish( verbose=True ):
            for c in clients:
                out, err = c.communicate()
                if verbose:
                    if err:
                        info( err )
                    else:
                        bw = out.split( ',' )[8]
                        info( '%s <--> %s: %s\n' % ( c.s, c.d, formatBw(bw) ) )
            for s in servers:
                s.terminate()

        try:
            info ( 'Press ^Z to continue in background or ^C to abort\n')
            progress( seconds )
            finish()
        except KeyboardInterrupt:
            for c in clients:
                c.terminate()
            for s in servers:
                s.terminate()
        except BackgroundException:
            info( '\n*** Continuing in background...\n' )
            t = Thread( target=finish, args=[ False ] )
            t.start()
        finally:
            #Disable custom background signal
            signal.signal(signal.SIGTSTP, oldSignal)

def progress(t):
    while t > 0:
        sys.stdout.write( '.' )
        t -= 1
        sys.stdout.flush()
        sleep(1)
    print

def formatBw( bw ):
    bw = float(bw)
    if bw > 1000:
        bw /= 1000
        if bw > 1000:
            bw /= 1000
            if bw > 1000:
                bw /= 1000
                return '%.2f Gbps' % bw
            return '%.2f Mbps' % bw
        return '%.2f Kbps' % bw
    return '%.2f bps' % bw

class BackgroundException( Exception ):
    pass

def do_bgIperf( self, line ):
    args = line.split()
    if not args:
        output( 'Provide a list of hosts.\n' )

    #Try to parse the '-t' argument as the number of seconds
    seconds = 10
    for i, arg in enumerate(args):
        if arg == '-t':
            if i + 1 < len(args):
                try:
                    seconds = int(args[i + 1])
                except ValueError:
                    error( 'Could not parse number of seconds: %s', args[i+1] )
                del(args[i+1])
            del args[i]

    hosts = []
    err = False
    for arg in args:
        if arg not in self.mn:
            err = True
            error( "node '%s' not in network\n" % arg )
        else:
            hosts.append( self.mn[ arg ] )
    if "bgIperf" in dir(self.mn) and not err:
        self.mn.bgIperf( hosts, seconds=seconds )

def do_gratuitousArp( self, line ):
    args = line.split()
    if "gratuitousArp" in dir( self.mn ):
        self.mn.gratuitousArp( args )
    else:
        output( 'Gratuitous ARP is not supported.\n' )

CLI.do_bgIperf = do_bgIperf
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
