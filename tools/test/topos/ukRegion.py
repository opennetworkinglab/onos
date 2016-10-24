#!/usr/bin/env python

"""
  Sample network for demonstrating regions (see uk-region.json)
"""
from mininet.topo import Topo


class UkRegionTopo(Topo):
    """Switches projected onto a portion of the UK map"""

    def __init__(self):
        """Create a topology."""

        # Initialize Topology
        Topo.__init__(self)

        # add nodes, switches first...
        DOVER = self.addSwitch('s1', dpid='0000000000000010')
        BRGHTN_A = self.addSwitch('s2', dpid='0000000000000021')
        BRGHTN_B = self.addSwitch('s3', dpid='0000000000000022')
        BRGHTN_C = self.addSwitch('s4', dpid='0000000000000023')
        LONDON_A = self.addSwitch('s5', dpid='0000000000000031')
        LONDON_B = self.addSwitch('s6', dpid='0000000000000032')
        LONDON_C = self.addSwitch('s7', dpid='0000000000000033')
        LONDON_D = self.addSwitch('s8', dpid='0000000000000034')
        LONDON_E = self.addSwitch('s9', dpid='0000000000000035')

        # ... and now hosts
        DOVER_host = self.addHost('h1', mac='000000000010')
        BRGHTN_A_host = self.addHost('h2', mac='000000000021')
        BRGHTN_B_host = self.addHost('h3', mac='000000000022')
        BRGHTN_C_host = self.addHost('h4', mac='000000000023')
        LONDON_A_host = self.addHost('h5', mac='000000000031')
        LONDON_B_host = self.addHost('h6', mac='000000000032')
        LONDON_C_host = self.addHost('h7', mac='000000000033')
        LONDON_D_host = self.addHost('h8', mac='000000000034')
        LONDON_E_host = self.addHost('h9', mac='000000000035')

        # add edges between switch and corresponding host
        self.addLink(DOVER, DOVER_host)
        self.addLink(BRGHTN_A, BRGHTN_A_host)
        self.addLink(BRGHTN_B, BRGHTN_B_host)
        self.addLink(BRGHTN_C, BRGHTN_C_host)
        self.addLink(LONDON_A, LONDON_A_host)
        self.addLink(LONDON_B, LONDON_B_host)
        self.addLink(LONDON_C, LONDON_C_host)
        self.addLink(LONDON_D, LONDON_D_host)
        self.addLink(LONDON_E, LONDON_E_host)

        # add edges between switches
        self.addLink(DOVER, BRGHTN_A, bw=10, delay='1.0ms')
        self.addLink(BRGHTN_A, BRGHTN_B, bw=10, delay='1.0ms')
        self.addLink(BRGHTN_A, BRGHTN_C, bw=10, delay='1.0ms')
        self.addLink(BRGHTN_B, BRGHTN_C, bw=10, delay='1.0ms')
        self.addLink(BRGHTN_C, LONDON_A, bw=10, delay='1.0ms')
        self.addLink(LONDON_A, LONDON_B, bw=10, delay='1.0ms')
        self.addLink(LONDON_A, LONDON_C, bw=10, delay='1.0ms')
        self.addLink(LONDON_B, LONDON_D, bw=10, delay='1.0ms')
        self.addLink(LONDON_C, LONDON_D, bw=10, delay='1.0ms')
        self.addLink(LONDON_C, LONDON_E, bw=10, delay='1.0ms')
        self.addLink(LONDON_D, LONDON_E, bw=10, delay='1.0ms')


topos = {'uk': (lambda: UkRegionTopo())}

if __name__ == '__main__':
    from onosnet import run

    run(UkRegionTopo())
