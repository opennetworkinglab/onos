#!/usr/bin/python

"""
Custom topology for Mininet
"""
from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import Host, RemoteController
from mininet.node import Node
from mininet.node import CPULimitedHost
from mininet.link import TCLink
from mininet.cli import CLI
from mininet.log import setLogLevel
from mininet.util import dumpNodeConnections
from mininet.node import ( UserSwitch, OVSSwitch, IVSSwitch )

class VLANHost( Host ):
    def config( self, vlan=100, **params ):
        r = super( Host, self ).config( **params )
        intf = self.defaultIntf()
        self.cmd( 'ifconfig %s inet 0' % intf )
        self.cmd( 'vconfig add %s %d' % ( intf, vlan ) )
        self.cmd( 'ifconfig %s.%d inet %s' % ( intf, vlan, params['ip'] ) )
        newName = '%s.%d' % ( intf, vlan )
        intf.name = newName
        self.nameToIntf[ newName ] = intf
        return r

class IPv6Host( Host ):
    def config( self, v6Addr='1000:1/64', **params ):
        r = super( Host, self ).config( **params )
        intf = self.defaultIntf()
        self.cmd( 'ifconfig %s inet 0' % intf )
        self.cmd( 'ip -6 addr add %s dev %s' % ( v6Addr, intf ) )
        return r

class dualStackHost( Host ):
    def config( self, v6Addr='2000:1/64', **params ):
        r = super( Host, self ).config( **params )
        intf = self.defaultIntf()
        self.cmd( 'ip -6 addr add %s dev %s' % ( v6Addr, intf ) )
        return r

class MyTopo( Topo ):

    def __init__( self ):
        # Initialize topology
        Topo.__init__( self )
        # Switch S5 Hosts
        host1=self.addHost( 'h1', ip='10.1.0.2/24' )
        host2=self.addHost( 'h2', cls=IPv6Host, v6Addr='1000::2/64' )
        host3=self.addHost( 'h3', ip='10.1.0.3/24', cls=dualStackHost, v6Addr='2000::2/64' )
        #VLAN hosts
        host4=self.addHost( 'h4', ip='100.1.0.2/24', cls=VLANHost, vlan=100 )
        host5=self.addHost( 'h5', ip='200.1.0.2/24', cls=VLANHost, vlan=200 )
        #VPN-1 and VPN-2 Hosts
        host6=self.addHost( 'h6', ip='11.1.0.2/24' )
        host7=self.addHost( 'h7', ip='12.1.0.2/24' )
        #Multicast Sender
        host8=self.addHost( 'h8', ip='10.1.0.4/24' )

        # Switch S6 Hosts
        host9=self.addHost( 'h9', ip='10.1.0.5/24' )
        host10=self.addHost( 'h10', cls=IPv6Host, v6Addr='1000::3/64' )
        host11=self.addHost( 'h11', ip='10.1.0.6/24', cls=dualStackHost, v6Addr='2000::3/64' )
        #VLAN hosts
        host12=self.addHost( 'h12', ip='100.1.0.3/24', cls=VLANHost, vlan=100 )
        host13=self.addHost( 'h13', ip='200.1.0.3/24', cls=VLANHost, vlan=200 )
        #VPN-1 and VPN-2 Hosts
        host14=self.addHost( 'h14', ip='11.1.0.3/24' )
        host15=self.addHost( 'h15', ip='12.1.0.3/24' )
        #Multicast Receiver
        host16=self.addHost( 'h16', ip='10.1.0.7/24' )

        # Switch S7 Hosts
        host17=self.addHost( 'h17', ip='10.1.0.8/24' )
        host18=self.addHost( 'h18', cls=IPv6Host, v6Addr='1000::4/64' )
        host19=self.addHost( 'h19', ip='10.1.0.9/24', cls=dualStackHost, v6Addr='2000::4/64' )
        #VLAN hosts
        host20=self.addHost( 'h20', ip='100.1.0.4/24', cls=VLANHost, vlan=100 )
        host21=self.addHost( 'h21', ip='200.1.0.4/24', cls=VLANHost, vlan=200 )
        #VPN-1 and VPN-2 Hosts
        host22=self.addHost( 'h22', ip='11.1.0.4/24' )
        host23=self.addHost( 'h23', ip='12.1.0.4/24' )
        #Multicast Receiver
        host24=self.addHost( 'h24', ip='10.1.0.10/24' )

        s1 = self.addSwitch( 's1' )
        s2 = self.addSwitch( 's2' )
        s3 = self.addSwitch( 's3' )
        s4 = self.addSwitch( 's4' )
        s5 = self.addSwitch( 's5' )
        s6 = self.addSwitch( 's6' )
        s7 = self.addSwitch( 's7' )

        self.addLink(s5,host1)
        self.addLink(s5,host2)
        self.addLink(s5,host3)
        self.addLink(s5,host4)
        self.addLink(s5,host5)
        self.addLink(s5,host6)
        self.addLink(s5,host7)
        self.addLink(s5,host8)

        self.addLink(s6,host9)
        self.addLink(s6,host10)
        self.addLink(s6,host11)
        self.addLink(s6,host12)
        self.addLink(s6,host13)
        self.addLink(s6,host14)
        self.addLink(s6,host15)
        self.addLink(s6,host16)

        self.addLink(s7,host17)
        self.addLink(s7,host18)
        self.addLink(s7,host19)
        self.addLink(s7,host20)
        self.addLink(s7,host21)
        self.addLink(s7,host22)
        self.addLink(s7,host23)
        self.addLink(s7,host24)

        self.addLink(s1,s2)
        self.addLink(s1,s3)
        self.addLink(s1,s4)
        self.addLink(s1,s5)
        self.addLink(s2,s3)
        self.addLink(s2,s5)
        self.addLink(s2,s6)
        self.addLink(s3,s4)
        self.addLink(s3,s6)
        self.addLink(s4,s7)
        topos = { 'mytopo': ( lambda: MyTopo() ) }

# HERE THE CODE DEFINITION OF THE TOPOLOGY ENDS

def setupNetwork():
    "Create network"
    topo = MyTopo()
    network = Mininet(topo=topo, autoSetMacs=True, controller=None)
    network.start()
    CLI( network )
    network.stop()

if __name__ == '__main__':
    setLogLevel('info')
    #setLogLevel('debug')
    setupNetwork()
