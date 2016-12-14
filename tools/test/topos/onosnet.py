#!/usr/bin/python
import itertools
import os
import signal
import sys
from argparse import ArgumentParser
from subprocess import call
from threading import Thread
from time import sleep

import gratuitousArp
from mininet.cli import CLI
from mininet.examples.controlnet import MininetFacade
from mininet.link import TCLink
from mininet.log import info, output, error
from mininet.log import setLogLevel
from mininet.net import Mininet
from mininet.node import RemoteController, Node

ARP_PATH = gratuitousArp.__file__.replace('.pyc', '.py')

class ONOSMininet( Mininet ):

    def __init__( self, controllers=[], gratuitousArp=True, build=True, *args, **kwargs ):
        """Create Mininet object for ONOS.
        controllers: List of controller IP addresses
        gratuitousArp: Send an ARP from each host to aid controller's host discovery"""

        # delay building for a second
        kwargs[ 'build' ] = False

        Mininet.__init__(self, *args, **kwargs )

        self.gratArp = gratuitousArp

        # If a controller is not provided, use list of remote controller IPs instead.
        if 'controller' not in kwargs or not kwargs['controller']:
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


def get_mn(mn):
    if isinstance(mn, ONOSMininet):
        return mn
    elif isinstance(mn, MininetFacade):
        # There's more Mininet objects instantiated (e.g. one for the control network in onos.py).
        for net in mn.nets:
            if isinstance(net, ONOSMininet):
                return net
    return None


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
    mn = get_mn( self.mn )
    if "bgIperf" in dir( mn ) and not err:
        mn.bgIperf( hosts, seconds=seconds )
    else:
        output('Background Iperf is not supported.\n')

def do_gratuitousArp( self, line ):
    args = line.split()
    mn = get_mn(self.mn)
    if "gratuitousArp" in dir( mn ):
        mn.gratuitousArp( args )
    else:
        output( 'Gratuitous ARP is not supported.\n' )

CLI.do_bgIperf = do_bgIperf
CLI.do_gratuitousArp = do_gratuitousArp

def parse_args():
    parser = ArgumentParser(description='ONOS Mininet')
    parser.add_argument('--cluster-size', help='Starts an ONOS cluster with the given number of instances',
                        type=int, action='store', dest='clusterSize', required=False, default=0)
    parser.add_argument('--netcfg', help='Relative path of the JSON file to be used with netcfg',
                        type=str, action='store', dest='netcfgJson', required=False, default='')
    parser.add_argument('ipAddrs', metavar='IP', type=str, nargs='*',
                        help='List of controller IP addresses', default=[])
    return parser.parse_args()

def run( topo, controllers=None, link=TCLink, autoSetMacs=True):
    if not topo:
        print 'Need to provide a topology'
        exit(1)

    args = parse_args()

    if not controllers and len(args.ipAddrs) > 0:
        controllers = args.ipAddrs

    if not controllers and args.clusterSize < 1:
        print 'Need to provide a list of controller IPs, or define a cluster size.'
        exit( 1 )

    setLogLevel( 'info' )

    if args.clusterSize > 0:
        if 'ONOS_ROOT' not in os.environ:
            print "Environment var $ONOS_ROOT not set (needed to import onos.py)"
            exit( 1 )
        sys.path.append(os.environ["ONOS_ROOT"] + "/tools/dev/mininet")
        from onos import ONOSCluster, ONOSOVSSwitch, ONOSCLI
        controller = ONOSCluster('c0', args.clusterSize)
        onosAddr = controller.nodes()[0].IP()
        net = ONOSMininet( topo=topo, controller=controller, switch=ONOSOVSSwitch, link=link,
                           autoSetMacs=autoSetMacs )
        cli = ONOSCLI
    else:
        onosAddr = controllers[0]
        net = ONOSMininet(topo=topo, controllers=controllers, link=link, autoSetMacs=autoSetMacs)
        cli = CLI

    net.start()

    if len(args.netcfgJson) > 0:
        if not os.path.isfile(args.netcfgJson):
            error('*** WARNING no such netcfg file: %s\n' % args.netcfgJson)
        else:
            info('*** Setting netcfg: %s\n' % args.netcfgJson)
            call(("onos-netcfg", onosAddr, args.netcfgJson))

    cli( net )
    net.stop()
