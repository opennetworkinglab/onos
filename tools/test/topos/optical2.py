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
    def makeIntfPair( cls, intf1, intf2 ):
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

        s1 = self.addSwitch('s1',dpid="0000ffffffff0001")
        s2 = self.addSwitch('s2',dpid="0000ffffffff0002")
        s3 = self.addSwitch('s3',dpid="0000ffffffff0003")
        s4 = self.addSwitch('s4',dpid="0000ffffffff0004")
        s5 = self.addSwitch('s5',dpid="0000ffffffff0005")
        s6 = self.addSwitch('s6',dpid="0000ffffffff0006")


        # Add links from hosts to OVS
        self.addLink(s1, h1)
        self.addLink(s2, h2)
        self.addLink(s3, h3)
        self.addLink(s4, h4)
        self.addLink(s5, h5)
        self.addLink(s6, h6)

        # temporary packet link from s1 to s2 for testing
        # self.addLink( s1, s2 )

        # add links from ovs to linc-oe
        # sorry about the syntax :(
        self.addLink(s1, s1, intfName1='s1-eth0', intfName2='tap29')
        self.addLink(s2, s2, intfName1='s2-eth0', intfName2='tap30')
        self.addLink(s3, s3, intfName1='s3-eth0', intfName2='tap31')
        self.addLink(s4, s4, intfName1='s4-eth0', intfName2='tap32')
        self.addLink(s5, s5, intfName1='s5-eth0', intfName2='tap33')
        self.addLink(s6, s6, intfName1='s6-eth0', intfName2='tap34')

        #self.addLink(s1, s2, s3, s4, s5, s6)
        #intfName1 = 'tap3', intfName\2 = 'tap4', intfName2 = 'tap5',
        # intfName2 = 'tap6', intfName2 = 'tap7', intfName2 = 'tap8'

    # if you use, sudo mn --custom custom/optical.py, then register the topo:
topos = {'optical': ( lambda: OpticalTopo() )}

def installStaticFlows(net):
    for swName in ['s1', 's2', 's3', 's4', 's5', 's6']:
        info('Adding flows to %s...' % swName)
        sw = net[swName]
        sw.dpctl('add-flow', 'in_port=1,actions=output=2')
        sw.dpctl('add-flow', 'in_port=2,actions=output=1')
        info(sw.dpctl('dump-flows'))


def run():
    c = RemoteController('c','10.1.8.147',6633)
    net = Mininet( topo=OpticalTopo(),controller=None)
    net.addController(c)
    net.start()

    # intf1 = Intf( 'tap3', node=net.nameToNode['s1'] )
    # intf2 = Intf( 'tap4', node=net.nameToNode['s2'] )
    # net.nameToNode['s1'].attach( intf1 )
    # net.nameToNode['s2'].attach( intf2 )

    #installStaticFlows( net )
    CLI( net )
    net.stop()

# if the script is run directly (sudo custom/optical.py):
if __name__ == '__main__':
    setLogLevel('info')
    run()
