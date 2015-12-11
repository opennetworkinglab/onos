#!/usr/bin/env python

''' file: custom/optical.py '''

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.cli import CLI
from mininet.log import setLogLevel, info
from mininet.link import Intf, Link
from mininet.node import RemoteController

class NullIntf( Intf ):
    "A dummy interface with a blank name that doesn't do any configuration"
    def __init__( self, name, **params ):
        self.name = ''

class NullLink( Link ):
    "A dummy link that doesn't touch either interface"
    def makeIntfPair( cls, intf1, intf2, *args, **kwargs ):
        pass
    def delete( self ):
        pass

class OpticalTopo(Topo):
    def addIntf( self, switch, intfName ):
        "Add intf intfName to switch"
        self.addLink( switch, switch, cls=NullLink,
                      intfName1=intfName, cls2=NullIntf )
    def __init__(self):

        # Initialize topology
        Topo.__init__(self)

        # Add hosts and switches
        h1 = self.addHost('h1')
        h2 = self.addHost('h2')
        h3 = self.addHost('h3')
        h4 = self.addHost('h4')
        h5 = self.addHost('h5')
        h6 = self.addHost('h6')

        s1 = self.addSwitch('s1', dpid="0000ffffffff0001")
        s2 = self.addSwitch('s2', dpid="0000ffffffff0002")
        s3 = self.addSwitch('s3', dpid="0000ffffffff0003")
        s4 = self.addSwitch('s4', dpid="0000ffffffff0004")
        s5 = self.addSwitch('s5', dpid="0000ffffffff0005")
        s6 = self.addSwitch('s6', dpid="0000ffffffff0006")


        # Add links from hosts to OVS
        self.addLink(s1, h1)
        self.addLink(s2, h2)
        self.addLink(s3, h3)
        self.addLink(s4, h4)
        self.addLink(s5, h5)
        self.addLink(s6, h6)

        # add links from ovs to linc-oe
        # sorry about the syntax :(
        self.addIntf(s1,'tap29')
        self.addIntf(s2,'tap30')
        self.addIntf(s3,'tap31')
        self.addIntf(s4,'tap32')
        self.addIntf(s5,'tap33')
        self.addIntf(s6,'tap34')

        # if you use, sudo mn --custom custom/optical.py, then register the topo:
topos = {'optical': ( lambda: OpticalTopo() )}


def run():
    c = RemoteController('c','127.0.0.1',6653)
    net = Mininet( topo=OpticalTopo(),controller=None,autoSetMacs=True)
    net.addController(c)
    net.start()

    #installStaticFlows( net )
    CLI( net )
    net.stop()

# if the script is run directly (sudo custom/optical.py):
if __name__ == '__main__':
    setLogLevel('info')
    run()
