#!/usr/bin/python

"""
onos.py: ONOS cluster and control network in Mininet

With onos.py, you can use Mininet to create a complete
ONOS network, including an ONOS cluster with a modeled
control network as well as the usual data nework.

This is intended to be useful for distributed ONOS
development and testing in the case that you require
a modeled control network.

Invocation (using OVS as default switch):

mn --custom onos.py --controller onos,3 --topo torus,4,4

Or with the user switch (or CPqD if installed):

mn --custom onos.py --controller onos,3 \
   --switch onosuser --topo torus,4,4

Currently you meed to use a custom switch class
because Mininet's Switch() class does't (yet?) handle
controllers with multiple IP addresses directly.

The classes may also be imported and used via Mininet's
python API.

Bugs/Gripes:
- We need --switch onosuser for the user switch because
  Switch() doesn't currently handle Controller objects
  with multiple IP addresses.
- ONOS startup and configuration is painful/undocumented.
- Too many ONOS environment vars - do we need them all?
- ONOS cluster startup is very, very slow. If Linux can
  boot in 4 seconds, why can't ONOS?
- It's a pain to mess with the control network from the
  CLI
- Setting a default controller for Mininet should be easier
"""

from mininet.node import Controller, OVSSwitch, UserSwitch
from mininet.nodelib import LinuxBridge
from mininet.net import Mininet
from mininet.topo import SingleSwitchTopo, Topo
from mininet.log import setLogLevel, info, warn, error, debug
from mininet.cli import CLI
from mininet.util import quietRun, specialClass
from mininet.examples.controlnet import MininetFacade

from os import environ
from os.path import dirname, join, isfile
from sys import argv
from glob import glob
import time
from functools import partial
from re import search


### ONOS Environment

KarafPort = 8101	# ssh port indicating karaf is running
GUIPort = 8181		# GUI/REST port
OpenFlowPort = 6653 	# OpenFlow port

def defaultUser():
    "Return a reasonable default user"
    if 'SUDO_USER' in environ:
        return environ[ 'SUDO_USER' ]
    try:
        user = quietRun( 'who am i' ).split()[ 0 ]
    except:
        user = 'nobody'
    return user

# Module vars, initialized below
HOME = ONOS_ROOT = ONOS_USER = None
ONOS_APPS = ONOS_WEB_USER = ONOS_WEB_PASS = ONOS_TAR = None

def initONOSEnv():
    """Initialize ONOS environment (and module) variables
       This is ugly and painful, but they have to be set correctly
       in order for the onos-setup-karaf script to work.
       nodes: list of ONOS nodes
       returns: ONOS environment variable dict"""
    # pylint: disable=global-statement
    global HOME, ONOS_ROOT, ONOS_USER
    global ONOS_APPS, ONOS_WEB_USER, ONOS_WEB_PASS
    env = {}
    def sd( var, val ):
        "Set default value for environment variable"
        env[ var ] = environ.setdefault( var, val )
        return env[ var ]
    assert environ[ 'HOME' ]
    HOME = sd( 'HOME', environ[ 'HOME' ] )
    ONOS_ROOT = sd( 'ONOS_ROOT',  join( HOME, 'onos' ) )
    environ[ 'ONOS_USER' ] = defaultUser()
    ONOS_USER = sd( 'ONOS_USER', defaultUser() )
    ONOS_APPS = sd( 'ONOS_APPS',
                     'drivers,openflow,fwd,proxyarp,mobility' )
    # ONOS_WEB_{USER,PASS} isn't respected by onos-karaf:
    environ.update( ONOS_WEB_USER='karaf', ONOS_WEB_PASS='karaf' )
    ONOS_WEB_USER = sd( 'ONOS_WEB_USER', 'karaf' )
    ONOS_WEB_PASS = sd( 'ONOS_WEB_PASS', 'karaf' )
    return env


def updateNodeIPs( env, nodes ):
    "Update env dict and environ with node IPs"
    # Get rid of stale junk
    for var in 'ONOS_NIC', 'ONOS_CELL', 'ONOS_INSTANCES':
        env[ var ] = ''
    for var in environ.keys():
        if var.startswith( 'OC' ):
            env[ var ] = ''
    for index, node in enumerate( nodes, 1 ):
        var = 'OC%d' % index
        env[ var ] = node.IP()
    env[ 'OCI' ] = env[ 'OCN' ] = env[ 'OC1' ]
    env[ 'ONOS_INSTANCES' ] = '\n'.join(
        node.IP() for node in nodes )
    environ.update( env )
    return env


tarDefaultPath = 'buck-out/gen/tools/package/onos-package/onos.tar.gz'

def unpackONOS( destDir='/tmp', run=quietRun ):
    "Unpack ONOS and return its location"
    global ONOS_TAR
    environ.setdefault( 'ONOS_TAR', join( ONOS_ROOT, tarDefaultPath ) )
    ONOS_TAR = environ[ 'ONOS_TAR' ]
    tarPath = ONOS_TAR
    if not isfile( tarPath ):
        raise Exception( 'Missing ONOS tarball %s - run buck build onos?'
                         % tarPath )
    info( '(unpacking %s)' % destDir)
    cmds = ( 'mkdir -p "%s" && cd "%s" && tar xzf "%s"'
             % ( destDir, destDir, tarPath) )
    run( cmds, shell=True, verbose=True )
    # We can use quietRun for this usually
    tarOutput = quietRun( 'tar tzf "%s" | head -1' % tarPath, shell=True)
    tarOutput = tarOutput.split()[ 0 ].strip()
    assert '/' in tarOutput
    onosDir = join( destDir, dirname( tarOutput ) )
    # Add symlink to log file
    run( 'cd %s; ln -s onos*/apache* karaf;'
         'ln -s karaf/data/log/karaf.log log' % destDir,
         shell=True )
    return onosDir


def waitListening( server, port=80, callback=None, sleepSecs=.5,
                   proc='java' ):
    "Simplified netstat version of waitListening"
    while True:
        lines = server.cmd( 'netstat -natp' ).strip().split( '\n' )
        entries = [ line.split() for line in lines ]
        portstr = ':%s' % port
        listening = [ entry for entry in entries
                      if len( entry ) > 6 and portstr in entry[ 3 ]
                      and proc in entry[ 6 ] ]
        if listening:
            break
        info( '.' )
        if callback:
            callback()
        time.sleep( sleepSecs )


### Mininet classes

def RenamedTopo( topo, *args, **kwargs ):
    """Return specialized topo with renamed hosts
       topo: topo class/class name to specialize
       args, kwargs: topo args
       sold: old switch name prefix (default 's')
       snew: new switch name prefix
       hold: old host name prefix (default 'h')
       hnew: new host name prefix
       This may be used from the mn command, e.g.
       mn --topo renamed,single,spref=sw,hpref=host"""
    sold = kwargs.pop( 'sold', 's' )
    hold = kwargs.pop( 'hold', 'h' )
    snew = kwargs.pop( 'snew', 'cs' )
    hnew = kwargs.pop( 'hnew' ,'ch' )
    topos = {}  # TODO: use global TOPOS dict
    if isinstance( topo, str ):
        # Look up in topo directory - this allows us to
        # use RenamedTopo from the command line!
        if topo in topos:
            topo = topos.get( topo )
        else:
            raise Exception( 'Unknown topo name: %s' % topo )
    # pylint: disable=no-init
    class RenamedTopoCls( topo ):
        "Topo subclass with renamed nodes"
        def addNode( self, name, *args, **kwargs ):
            "Add a node, renaming if necessary"
            if name.startswith( sold ):
                name = snew + name[ len( sold ): ]
            elif name.startswith( hold ):
                name = hnew + name[ len( hold ): ]
            return topo.addNode( self, name, *args, **kwargs )
    return RenamedTopoCls( *args, **kwargs )


class ONOSNode( Controller ):
    "ONOS cluster node"

    def __init__( self, name, **kwargs ):
        "alertAction: exception|ignore|warn|exit (exception)"
        kwargs.update( inNamespace=True )
        self.alertAction = kwargs.pop( 'alertAction', 'exception' )
        Controller.__init__( self, name, **kwargs )
        self.dir = '/tmp/%s' % self.name
        self.client = self.dir + '/karaf/bin/client'
        self.ONOS_HOME = '/tmp'
        self.cmd( 'rm -rf', self.dir )
        self.ONOS_HOME = unpackONOS( self.dir, run=self.ucmd )

    # pylint: disable=arguments-differ

    def start( self, env, nodes=() ):
        """Start ONOS on node
           env: environment var dict
           nodes: all nodes in cluster"""
        env = dict( env )
        env.update( ONOS_HOME=self.ONOS_HOME )
        self.updateEnv( env )
        karafbin = glob( '%s/apache*/bin' % self.ONOS_HOME )[ 0 ]
        onosbin = join( ONOS_ROOT, 'tools/test/bin' )
        self.cmd( 'export PATH=%s:%s:$PATH' % ( onosbin, karafbin ) )
        self.cmd( 'cd', self.ONOS_HOME )
        self.ucmd( 'mkdir -p config && '
                   'onos-gen-partitions config/cluster.json',
                   ' '.join( node.IP() for node in nodes ) )
        info( '(starting %s)' % self )
        service = join( self.ONOS_HOME, 'bin/onos-service' )
        self.ucmd( service, 'server 1>../onos.log 2>../onos.log'
                   ' & echo $! > onos.pid; ln -s `pwd`/onos.pid ..' )
        self.onosPid = int( self.cmd( 'cat onos.pid' ).strip() )
        self.warningCount = 0

    # pylint: enable=arguments-differ

    def stop( self ):
        # XXX This will kill all karafs - too bad!
        self.cmd( 'pkill -HUP -f karaf.jar && wait' )
        self.cmd( 'rm -rf', self.dir )

    def sanityAlert( self, *args ):
        "Alert to raise on sanityCheck failure"
        info( '\n' )
        if self.alertAction == 'exception':
            raise Exception( *args )
        if self.alertAction == 'warn':
            warn( *args + ( '\n', ) )
        elif self.alertAction == 'exit':
            error( '***',  *args +
                   ( '\nExiting. Run "sudo mn -c" to clean up.\n', ) )
            exit( 1 )

    def isRunning( self ):
        "Is our ONOS process still running?"
        cmd = ( 'ps -p %d  >/dev/null 2>&1 && echo "running" ||'
                'echo "not running"' )
        return self.cmd( cmd % self.onosPid ).strip() == 'running'

    def checkLog( self ):
        "Return log file errors and warnings"
        log = join( self.dir, 'log' )
        errors, warnings = [], []
        if isfile( log ):
            lines = open( log ).read().split( '\n' )
            errors = [ line for line in lines if 'ERROR' in line ]
            warnings = [ line for line in lines if 'WARN'in line ]
        return errors, warnings

    def memAvailable( self ):
        "Return available memory in KB (or -1 if we can't tell)"
        lines = open( '/proc/meminfo' ).read().strip().split( '\n' )
        entries = map( str.split, lines )
        index = { entry[ 0 ]: entry for entry in entries }
        # Check MemAvailable if present
        default = ( None, '-1', 'kB' )
        _name, count, unit = index.get( 'MemAvailable:', default )
        if unit.lower() == 'kb':
            return int( count )
        return -1

    def sanityCheck( self, lowMem=100000 ):
        """Check whether we've quit or are running out of memory
           lowMem: low memory threshold in KB (100000)"""
        # Are we still running?
        if not self.isRunning():
            self.sanityAlert( 'ONOS node %s has died' % self.name )
        # Are there errors in the log file?
        errors, warnings  = self.checkLog()
        if errors:
            self.sanityAlert( 'ONOS startup errors:\n<<%s>>' %
                              '\n'.join( errors ) )
        warningCount = len( warnings )
        if warnings and warningCount > self.warningCount:
            warn( '(%d warnings)' % len( warnings ) )
            self.warningCount = warningCount
        # Are we running out of memory?
        mem = self.memAvailable()
        if mem > 0 and mem < lowMem:
            self.sanityAlert( 'Running out of memory (only %d KB available)'
                              % mem )

    def waitStarted( self ):
        "Wait until we've really started"
        info( '(checking: karaf' )
        while True:
            status = self.ucmd( 'karaf status' ).lower()
            if 'running' in status and 'not running' not in status:
                break
            info( '.' )
            self.sanityCheck()
            time.sleep( 1 )
        info( ' ssh-port' )
        waitListening( server=self, port=KarafPort, callback=self.sanityCheck )
        info( ' openflow-port' )
        waitListening( server=self, port=OpenFlowPort,
                       callback=self.sanityCheck )
        info( ' client' )
        while True:
            result = quietRun( '%s -h %s "apps -a"' %
                               ( self.client, self.IP() ), shell=True )
            if 'openflow' in result:
                break
            info( '.' )
            self.sanityCheck()
            time.sleep( 1 )
        info( ')\n' )

    def updateEnv( self, envDict ):
        "Update environment variables"
        cmd = ';'.join( ( 'export %s="%s"' % ( var, val )
                          if val else 'unset %s' % var )
                        for var, val in envDict.iteritems() )
        self.cmd( cmd )

    def ucmd( self, *args, **_kwargs ):
        "Run command as $ONOS_USER using sudo -E -u"
        if ONOS_USER != 'root':  # don't bother with sudo
            args = [ "sudo -E -u $ONOS_USER PATH=$PATH "
                     "bash -c '%s'" % ' '.join( args ) ]
        return self.cmd( *args )


class ONOSCluster( Controller ):
    "ONOS Cluster"
    def __init__( self, *args, **kwargs ):
        """name: (first parameter)
           *args: topology class parameters
           ipBase: IP range for ONOS nodes
           forward: default port forwarding list,
           topo: topology class or instance
           nodeOpts: ONOSNode options
           **kwargs: additional topology parameters"""
        args = list( args )
        name = args.pop( 0 )
        topo = kwargs.pop( 'topo', None )
        nat = kwargs.pop( 'nat', 'nat0' )
        nodeOpts = kwargs.pop( 'nodeOpts', {} )
        # Default: single switch with 1 ONOS node
        if not topo:
            topo = SingleSwitchTopo
            if not args:
                args = ( 1, )
        if not isinstance( topo, Topo ):
            topo = RenamedTopo( topo, *args, hnew='onos', **kwargs )
        self.ipBase = kwargs.pop( 'ipBase', '192.168.123.0/24' )
        self.forward = kwargs.pop( 'forward',
                                   [ KarafPort, GUIPort, OpenFlowPort ] )
        super( ONOSCluster, self ).__init__( name, inNamespace=False )
        fixIPTables()
        self.env = initONOSEnv()
        self.net = Mininet( topo=topo, ipBase=self.ipBase,
                            host=partial( ONOSNode, **nodeOpts ),
                            switch=LinuxBridge,
                            controller=None )
        if nat:
            self.net.addNAT( nat ).configDefault()
        updateNodeIPs( self.env, self.nodes() )
        self._remoteControllers = []

    def start( self ):
        "Start up ONOS cluster"
        info( '*** ONOS_APPS = %s\n' % ONOS_APPS )
        self.net.start()
        for node in self.nodes():
            node.start( self.env, self.nodes() )
        info( '\n' )
        self.configPortForwarding( ports=self.forward, action='A' )
        self.waitStarted()
        return

    def waitStarted( self ):
        "Wait until all nodes have started"
        startTime = time.time()
        for node in self.nodes():
            info( node )
            node.waitStarted()
        info( '*** Waited %.2f seconds for ONOS startup' %
              ( time.time() - startTime ) )

    def stop( self ):
        "Shut down ONOS cluster"
        self.configPortForwarding( ports=self.forward, action='D' )
        for node in self.nodes():
            node.stop()
        self.net.stop()

    def nodes( self ):
        "Return list of ONOS nodes"
        return [ h for h in self.net.hosts if isinstance( h, ONOSNode ) ]

    def defaultIntf( self ):
        "Call ip route to determine default interface"
        result = quietRun( 'ip route | grep default', shell=True ).strip()
        match = search( r'dev\s+([^\s]+)', result )
        if match:
            intf = match.group( 1 )
        else:
            warn( "Can't find default network interface - using eth0\n" )
            intf = 'eth0'
        return intf

    def configPortForwarding( self, ports=[], intf='', action='A' ):
        """Start or stop forwarding on intf to all nodes
           action: A=add/start, D=delete/stop (default: A)"""
        if not intf:
            intf = self.defaultIntf()
        for port in ports:
            for index, node in enumerate( self.nodes() ):
                ip, inport = node.IP(), port + index
                # Configure a destination NAT rule
                cmd = ( 'iptables -t nat -{action} PREROUTING -t nat '
                        '-i {intf} -p tcp --dport {inport} '
                        '-j DNAT --to-destination {ip}:{port}' )
                self.cmd( cmd.format( **locals() ) )


class ONOSSwitchMixin( object ):
    "Mixin for switches that connect to an ONOSCluster"
    def start( self, controllers ):
        "Connect to ONOSCluster"
        self.controllers = controllers
        assert ( len( controllers ) is 1 and
                 isinstance( controllers[ 0 ], ONOSCluster ) )
        clist = controllers[ 0 ].nodes()
        return super( ONOSSwitchMixin, self ).start( clist )

class ONOSOVSSwitch( ONOSSwitchMixin, OVSSwitch ):
    "OVSSwitch that can connect to an ONOSCluster"
    pass

class ONOSUserSwitch( ONOSSwitchMixin, UserSwitch):
    "UserSwitch that can connect to an ONOSCluster"
    pass


### Ugly utility routines

def fixIPTables():
    "Fix LinuxBridge warning"
    for s in 'arp', 'ip', 'ip6':
        quietRun( 'sysctl net.bridge.bridge-nf-call-%stables=0' % s )


### Test code

def test( serverCount ):
    "Test this setup"
    setLogLevel( 'info' )
    net = Mininet( topo=SingleSwitchTopo( 3 ),
                   controller=[ ONOSCluster( 'c0', serverCount ) ],
                   switch=ONOSOVSSwitch )
    net.start()
    net.waitConnected()
    CLI( net )
    net.stop()


### CLI Extensions

OldCLI = CLI

class ONOSCLI( OldCLI ):
    "CLI Extensions for ONOS"

    prompt = 'mininet-onos> '

    def __init__( self, net, **kwargs ):
        c0 = net.controllers[ 0 ]
        if isinstance( c0, ONOSCluster ):
            net = MininetFacade( net, cnet=c0.net )
        OldCLI.__init__( self, net, **kwargs )

    def onos1( self ):
        "Helper function: return default ONOS node"
        return self.mn.controllers[ 0 ].net.hosts[ 0 ]

    def do_onos( self, line ):
        "Send command to ONOS CLI"
        c0 = self.mn.controllers[ 0 ]
        if isinstance( c0, ONOSCluster ):
            # cmdLoop strips off command name 'onos'
            if line.startswith( ':' ):
                line = 'onos' + line
            onos1 = self.onos1().name
            if line:
                line = '"%s"' % line
            cmd = '%s client -h %s %s' % ( onos1, onos1, line )
            quietRun( 'stty -echo' )
            self.default( cmd )
            quietRun( 'stty echo' )

    def do_wait( self, line ):
        "Wait for switches to connect"
        self.mn.waitConnected()

    def do_balance( self, line ):
        "Balance switch mastership"
        self.do_onos( ':balance-masters' )

    def do_log( self, line ):
        "Run tail -f /tmp/onos1/log; press control-C to stop"
        self.default( '%s tail -f /tmp/%s/log' %
                      ( self.onos1(), self.onos1() ) )

    def do_status( self, line ):
        "Return status of ONOS cluster(s)"
        for c in self.mn.controllers:
            if isinstance( c, ONOSCluster ):
                for node in c.net.hosts:
                    if isinstance( node, ONOSNode ):
                        errors, warnings = node.checkLog()
                        running = ( 'Running' if node.isRunning()
                                   else 'Exited' )
                        status = ''
                        if errors:
                            status += '%d ERRORS ' % len( errors )
                        if warnings:
                            status += '%d warnings' % len( warnings )
                        status = status if status else 'OK'
                        info( node, '\t', running, '\t', status, '\n' )

    def do_arp( self, line ):
        "Send gratuitous arps from all data network hosts"
        startTime = time.time()
        try:
            count = int( line )
        except:
            count = 1
        # Technically this check should be on the host
        if '-U' not in quietRun( 'arping -h', shell=True ):
            warn( 'Please install iputils-arping.\n' )
            return
        # This is much faster if we do it in parallel
        for host in self.mn.net.hosts:
            intf = host.defaultIntf()
            # -b: keep using broadcasts; -f: quit after 1 reply
            # -U: gratuitous ARP update
            host.sendCmd( 'arping -bf -c', count, '-U -I',
                           intf.name, intf.IP() )
        for host in self.mn.net.hosts:
            # We could check the output here if desired
            host.waitOutput()
            info( '.' )
        info( '\n' )
        elapsed = time.time() - startTime
        debug( 'Completed in %.2f seconds\n' % elapsed )


# For interactive use, exit on error
exitOnError = dict( nodeOpts={ 'alertAction': 'exit' } )
ONOSClusterInteractive = specialClass( ONOSCluster, defaults=exitOnError )


### Exports for bin/mn

CLI = ONOSCLI
controllers = { 'onos': ONOSClusterInteractive,
                'default': ONOSClusterInteractive }

# XXX Hack to change default controller as above doesn't work
findController = lambda: controllers[ 'default' ]

switches = { 'onos': ONOSOVSSwitch,
             'onosovs': ONOSOVSSwitch,
             'onosuser': ONOSUserSwitch,
             'default': ONOSOVSSwitch }

# Null topology so we can control an external/hardware network
topos = { 'none': Topo }

if __name__ == '__main__':
    if len( argv ) != 2:
        test( 3 )
    else:
        test( int( argv[ 1 ] ) )
