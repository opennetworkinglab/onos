#!/usr/bin/env python

# TODO add onos-app-fwd to features
# TODO check if service is running... i think this might already be done by mn

from mininet.node import Controller, OVSSwitch, CPULimitedHost, RemoteController
from mininet.net import Mininet
from mininet.cli import CLI
from mininet.topo import LinearTopo, Topo
from mininet.log import setLogLevel, info, warn
from mininet.util import quietRun, numCores

from shutil import copyfile
from os import environ, path
from functools import partial
import time
from sys import argv
from time import sleep
from sets import Set

class ONOS( Controller ):
    "TODO"

    onosDir = '/opt/onos/'

    def __init__( self, name, onosDir=onosDir,
                  reactive=True, features=[ 'onos-app-tvue' ],
                  **kwargs ):
        '''TODO'''

        Controller.__init__( self, name, **kwargs )
        # the following have been done for us:
        #self.ip = ip ('127.0.0.1')
        #self.port = port (6653)
        #self.protocol = protocol ('tcp')
        #self.checkListening()

        self.onosDir = onosDir
        self.karafDir = onosDir + 'apache-karaf-3.0.3/'
        self.instanceDir = self.karafDir

        # add default modules
        # TODO: consider an ordered set
        self.features = Set([ 'webconsole',
                              'onos-rest',
                              'onos-api',
                              'onos-cli',
                              'onos-openflow' ])
        self.features.update( features )
        # add reactive forwarding modules
        if reactive:
            self.features.update( ['onos-app-fwd', 
                                   'onos-app-proxyarp',
                                   'onos-app-mobility' ] )
        # add the distributed core if we are in a namespace with no trivial core
        if self.inNamespace and 'onos-core-trivial' not in self.features:
            self.features.add( 'onos-core' )
        # if there is no core, add the trivial one
        if 'onos-core' not in self.features:
            self.features.add( 'onos-core-trivial' )
        print self.features  
   
    def start( self ):
        if self.inNamespace:
            instanceOpts = ( '-furl mvn:org.onosproject/onos-features/1.4.1-SNAPSHOT/xml/features '
                             '-s 8101' )
            if self.ip is not None:
                instanceOpts += (' -a %s' % self.IP() )
            self.userCmd( self.karafDir + 'bin/instance create %s %s' % ( instanceOpts, self.name ) )
            self.instanceDir = self.karafDir + 'instances/%s/' % self.name
        else:
            # we are running in the root namespace, so let's use the root instance
            # clean up the data directory
            #self.userCmd( 'rm -rf '+ self.karafDir + 'data/' )
            pass

        self.userCmd( 'rm -rf '+ self.instanceDir + 'data/' )

        # Update etc/org.apache.karaf.features.cfg
        self.updateFeatures()

        # TODO 2. Update etc/hazelcast.xml : interface lines
        #cp etc/hazelcast.xml instances/c1/etc/
        self.updateHazelcast()

        # TODO 3. Update etc/system.properties : onos.ip
        # TODO 4. Update config/cluster.json : with all nodes

        # start onos
        self.userCmd( '%sbin/instance start -d %s' % ( self.karafDir, self.name ) )
        #TODO we should wait for startup...

    def stop( self ):
        self.userCmd( self.instanceDir + 'bin/stop' )
        #if self.inNamespace:
        #    self.userCmd( self.karafDir + 'bin/instance destroy %s' % self.name )
        self.terminate()

    def updateHazelcast( self ):
        hz = '192.168.123.*'
        if self.ip is not None:
            hz = '.'.join(self.ip.split('.')[:-1]) + '.*'

        readfile = self.karafDir + 'etc/hazelcast.xml'
        writefile = self.instanceDir + 'etc/hazelcast.xml'
        with open( readfile, 'r' ) as r:
            with open( writefile, 'w' ) as w:
                for line in r.readlines():
                    if '<interface>' in line:
                        line = '<interface>' + hz + '</interface>\n'
                    w.write( line )

    def updateFeatures( self ):
        filename = self.instanceDir + 'etc/org.apache.karaf.features.cfg'
        with open( filename, 'r+' ) as f:
            lines = f.readlines()
            f.seek(0)
            f.truncate()
            for line in lines:
                #print '?', line,
                if 'featuresBoot=' in line:
                    # parse the features from the line
                    features = line.rstrip().split('=')[1].split(',')
                    # add the features to our features set
                    self.features.update( features )
                    # generate the new features line
                    line = 'featuresBoot=' + ','.join( self.features ) + '\n'
                    #print '!', line,
                f.write( line )


    @classmethod
    def isAvailable( self ):
        return quietRun( 'ls %s' % self.onosDir )

    def userCmd( self, cmd ):
        # switch to the non-root user because karaf gets upset otherwise
        # because the .m2repo is not stored with root
        cmd = 'sudo -u %s %s' % ( self.findUser(), cmd )
        return self.cmd( cmd )

    @staticmethod
    def findUser():
        "Try to return logged-in (usually non-root) user"
        try:
            # If we're running sudo
            return os.environ[ 'SUDO_USER' ]
        except:
            try:
                # Logged-in user (if we have a tty)
                return quietRun( 'who am i' ).split()[ 0 ]
            except:
                # Give up and return effective user
                return quietRun( 'whoami' )
 

class ControlNetwork( Topo ):
    "Control Network Topology"
    def __init__( self, n, dataController=ONOS, **kwargs ):
        """n: number of data network controller nodes
           dataController: class for data network controllers"""
        Topo.__init__( self, **kwargs )
        # Connect everything to a single switch
        cs0 = self.addSwitch( 'cs0' )
        # Add hosts which will serve as data network controllers
        for i in range( 1, n+1 ):
            c = self.addHost( 'c%s' % i, cls=dataController,
                              inNamespace=True )
            self.addLink( c, cs0 )
        # Connect switch to root namespace so that data network
        # switches will be able to talk to us
        root = self.addHost( 'root', inNamespace=False )
        self.addLink( root, cs0 )

class ONOSCluster( Controller ):
    # TODO
    n = 3
   
    def start( self ):
        ctopo = ControlNetwork( n=self.n, dataController=ONOS )
        self.cnet = Mininet( topo=ctopo, ipBase='192.168.123.0/24', controller=None )
        self.cnet.addController( 'cc0', controller=Controller )
        self.cnet.start()

        self.ctrls = []
        for host in self.cnet.hosts:
            if isinstance( host, Controller ):
                self.ctrls.append( host )
                host.start()

    def stop( self ):
        for host in self.cnet.hosts:
            if isinstance( host, Controller ):
                host.stop()
        self.cnet.stop()
        
    def clist( self ):
        "Return list of Controller proxies for this ONOS cluster"
        print 'controllers:', self.ctrls
        return self.ctrls

class OVSSwitchONOS( OVSSwitch ):
    "OVS switch which connects to multiple controllers"
    def start( self, controllers ):
        assert len( controllers ) == 1
        c0 = controllers[ 0 ]
        assert type( c0 ) == ONOSCluster
        controllers = c0.clist()
        OVSSwitch.start( self, controllers )

controllers = { 'onos': ONOS }
switches = { 'ovso': OVSSwitchONOS }

if __name__ == '__main__':
    # Simple test for ONOS() controller class
    setLogLevel( 'info' ) #TODO info
    size = 2 if len( argv ) != 2 else int( argv[ 1 ] )
    net = Mininet( topo=LinearTopo( size ),
                   #controller=ONOS,
                   controller=partial( ONOSCluster, n=3 ), #TODO
                   switch=OVSSwitchONOS )
    net.start()
    #waitConnected( net.switches )
    CLI( net )
    net.stop()
