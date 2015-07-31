#!/usr/bin/env python

from mininet.topo import Topo

class FractalTopo( Topo ):
    def build( self, n=3, h=2 ):

        clusters = []
        for i in range( 1, n+1 ):
            clusterSws = []
            # create switches in cluster
            for j in range( 1, n+1 ):
                id = i * 1000 + j
                sw = self.addSwitch('s%d' % id, dpid=str(id).zfill(16))
                [ self.addLink(s, sw) for s in clusterSws ]
                clusterSws.append(sw)
            clusters.append(clusterSws)

        for i in range( 1, n+1 ):
            # create the edge switch
            id = i * 10000
            sw = self.addSwitch('s%d' % id, dpid=str(id).zfill(16))
            self.addLink(clusters[i-1].pop(0), sw)
            for j in range( 1, h+1 ):
                id = i * 1000 + j
                host = self.addHost( 'h%d' % id )
                self.addLink( host, sw )

        for i in range( 1, n+1 ):
            # connect the clusters
            if i == n:
                id = n * 1000000 + 10000
                sw = self.addSwitch('s%d' % id, dpid=str(id).zfill(16))
                self.addLink(clusters[i-1].pop(0), sw)
                self.addLink(clusters[0].pop(0), sw)

            else:
                id = (i+1) * 1000000 + i * 10000
                sw = self.addSwitch('s%d' % id, dpid=str(id).zfill(16))
                self.addLink(clusters[i-1].pop(0), sw)
                self.addLink(clusters[i].pop(0), sw)


topos = { 'fractal': FractalTopo }

def run():
    topo = FractalTopo()
    net = Mininet( topo=topo, controller=RemoteController, autoSetMacs=True )
    net.start()
    CLI( net )
    net.stop()

if __name__ == '__main__':
    setLogLevel( 'info' )
    run()

