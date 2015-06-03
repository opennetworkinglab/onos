#!/usr/bin/env python

from mininet.topo import Topo

class ObeliskTopo( Topo ):
    def __init__( self ):
        Topo.__init__( self )
        topSwitch = self.addSwitch('s1',dpid='1000'.zfill(16))
        leftTopSwitch = self.addSwitch('s2',dpid='2000'.zfill(16))
        rightTopSwitch = self.addSwitch('s5',dpid='5000'.zfill(16))
        leftBotSwitch = self.addSwitch('s3',dpid='3000'.zfill(16))
        rightBotSwitch = self.addSwitch('s6',dpid='6000'.zfill(16))	
        midBotSwitch = self.addSwitch('s28',dpid='2800'.zfill(16))
        
        topHost = self.addHost( 'h1' )
        leftTopHost = self.addHost('h2')
        rightTopHost = self.addHost('h5')
        leftBotHost = self.addHost('h3')
        rightBotHost = self.addHost('h6')
        midBotHost = self.addHost('h28')
        self.addLink(topSwitch,topHost)
        self.addLink(leftTopSwitch,leftTopHost)
        self.addLink(rightTopSwitch,rightTopHost)
        self.addLink(leftBotSwitch,leftBotHost)
        self.addLink(rightBotSwitch,rightBotHost)
        self.addLink(midBotSwitch,midBotHost)
        self.addLink(leftTopSwitch,rightTopSwitch)
        self.addLink(topSwitch,leftTopSwitch)
        self.addLink(topSwitch,rightTopSwitch)
        self.addLink(leftTopSwitch,leftBotSwitch)
        self.addLink(rightTopSwitch,rightBotSwitch)
        self.addLink(leftBotSwitch,midBotSwitch)
        self.addLink(midBotSwitch,rightBotSwitch)

        agg1Switch = self.addSwitch('s4',dpid = '3004'.zfill(16))
        agg2Switch = self.addSwitch('s7',dpid = '6007'.zfill(16))
        agg1Host = self.addHost('h4')
        agg2Host = self.addHost('h7')
        self.addLink(agg1Switch,agg1Host)
        self.addLink(agg2Switch,agg2Host)
        self.addLink(agg1Switch, leftBotSwitch)
        self.addLink(agg2Switch, rightBotSwitch)

        for i in range(10):
            num = str(i+8)
            switch = self.addSwitch('s'+num,dpid = ('30'+num.zfill(2)).zfill(16))
            host = self.addHost('h'+num)
            self.addLink(switch, host)
            self.addLink(switch, agg1Switch)

        for i in range(10):
            num = str(i+18)
            switch = self.addSwitch('s'+num,dpid = ('60'+num.zfill(2)).zfill(16))
            host = self.addHost('h'+num)
            self.addLink(switch, host)
            self.addLink(switch, agg2Switch)

topos = { 'obelisk': (lambda: ObeliskTopo() ) }

def run():
    topo = ObeliskTopo()
    net = Mininet( topo=topo, controller=RemoteController, autoSetMacs=True )
    net.start()
    CLI( net )
    net.stop()

if __name__ == '__main__':
    setLogLevel( 'info' )
    run()

