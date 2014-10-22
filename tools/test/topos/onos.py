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

class ONOS( Controller ):
    #def __init__( self, name, command='/opt/onos/bin/onos-service', **kwargs ):
    #    Controller.__init__( self, name, command=command, inNamespace=True,  **kwargs )
    #def __init__( self, name, inNamespace=False, command='controller',
    #          cargs='-v ptcp:%d', cdir=None, ip="127.0.0.1",
    #          port=6633, protocol='tcp', **params ):
    #self.command = command
    #self.cargs = cargs
    #self.cdir = cdir
    #self.ip = ip
    #self.port = port
    #self.protocol = protocol
    #Node.__init__( self, name, inNamespace=inNamespace,
    #               ip=ip, **params  )
    #self.checkListening()
    
    ONOS_DIR = '/opt/onos/'
    KARAF_DIR = ONOS_DIR + 'apache-karaf-3.0.1/'
    reactive = True
   
    def start( self ):
        # switch to the non-root user because karaf gets upset otherwise
        # TODO we should look into why.... 
        self.sendCmd( 'sudo su - %s' % self.findUser() )
        self.waiting = False

        if self.inNamespace:
            self.cmd( self.KARAF_DIR + 'bin/instance create %s' % self.name )
            src  = self.KARAF_DIR + 'etc/org.apache.karaf.features.cfg'
            dst = self.KARAF_DIR + 'instances/%s/etc/org.apache.karaf.features.cfg' % self.name
            self.cmd( 'cp %s %s' % (src, dst) )
            self.updateProperties( dst )
            self.cmd( self.KARAF_DIR + 'bin/instance start %s' % self.name )
        else:
            # we are running in the root namespace, so let's use the root instance
            self.cmd( 'rm -rf '+ self.KARAF_DIR + 'data/' )
            filename = self.KARAF_DIR + 'etc/org.apache.karaf.features.cfg'
            self.updateProperties( filename )
            self.cmd( self.KARAF_DIR + 'bin/start' )

        #TODO we should wait for startup...

    def stop( self ):
        if self.inNamespace:
            self.cmd( '/opt/onos/apache-karaf-3.0.1/bin/instance stop %s' % self.name )
            self.cmd( '/opt/onos/apache-karaf-3.0.1/bin/instance destroy %s' % self.name )
        else:
            self.cmd( self.ONOS_DIR + 'apache-karaf-3.0.1/bin/stop' )
        self.terminate()

    def updateProperties( self, filename ):
        with open( filename, 'r+' ) as f:
            lines = f.readlines()
            f.seek(0)
            f.truncate()
            for line in lines:
                #print '?', line,
                if 'featuresBoot=' in line:
                    line = line.rstrip()
                    #print ord(line[-1]), ord(line[-2]), ord(line[-3])
                    if self.reactive:
                        line += ',onos-app-fwd'
                    line += '\n'
                    #print '!', line,
                f.write( line )

    @classmethod
    def isAvailable( self ):
        return quietRun( 'ls /opt/onos' )

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
        for i in range( 0, n ):
            c = self.addHost( 'c%s' % i, cls=dataController,
                              inNamespace=True )
            self.addLink( c, cs0 )
        # Connect switch to root namespace so that data network
        # switches will be able to talk to us
        root = self.addHost( 'root', inNamespace=False )
        self.addLink( root, cs0 )

class ONOSCluster( Controller ):
    # TODO
    n = 4
   
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
    setLogLevel( 'info' )
    size = 2 if len( argv ) != 2 else int( argv[ 1 ] )
    net = Mininet( topo=LinearTopo( size ),
                   controller=partial( ONOSCluster, n=4 ),
                   switch=OVSSwitchONOS )
    net.start()
    #waitConnected( net.switches )
    CLI( net )
    net.stop()
