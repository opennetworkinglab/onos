#!/usr/bin/env python

"""
"""
from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import RemoteController
from mininet.node import Node
from mininet.node import CPULimitedHost
from mininet.link import TCLink
from mininet.cli import CLI
from mininet.log import setLogLevel
from mininet.util import dumpNodeConnections

class AttMplsTopoExt( Topo ):
    "Internet Topology Zoo Specimen."

    def __init__( self ):
        "Create a topology."

        # Initialize Topology
        Topo.__init__( self )

        # add nodes, switches first...
        MINE = self.addSwitch( 's31', dpid='0000001000000001')  # 44.977862, -93.265427
        BISM = self.addSwitch( 's32', dpid='0000001000000002')  # 46.817887, -100.786109
        BOIS = self.addSwitch( 's33', dpid='0000001000000003')  # 43.617834, -116.216903
        RENO = self.addSwitch( 's34', dpid='0000001000000004')  # 39.533310, -119.796940
        ALBU = self.addSwitch( 's35', dpid='0000001000000005')  # 35.109657, -106.626698

        # ... and now hosts
        MINE_host = self.addHost( 'h31', mac='00:10:00:00:00:01' )
        BISM_host = self.addHost( 'h32', mac='00:10:00:00:00:02'  )
        BOIS_host = self.addHost( 'h33', mac='00:10:00:00:00:03'  )
        RENO_host = self.addHost( 'h34', mac='00:10:00:00:00:04'  )
        ALBU_host = self.addHost( 'h35', mac='00:10:00:00:00:05'  )

        # add edges between switch and corresponding host
        self.addLink( MINE , MINE_host )
        self.addLink( BISM , BISM_host )
        self.addLink( BOIS , BOIS_host )
        self.addLink( RENO , RENO_host )
        self.addLink( ALBU , ALBU_host )

        # add edges between switches
        self.addLink( MINE , BISM, bw=10, delay='0.979030824185ms')
        self.addLink( BISM , BOIS, bw=10, delay='0.806374975652ms')
        self.addLink( BOIS , RENO, bw=10, delay='0.686192970166ms')
        self.addLink( BOIS , ALBU, bw=10, delay='0.605826192092ms')
        self.addLink( RENO , ALBU, bw=10, delay='1.4018238197ms')
        self.addLink( RENO , MINE, bw=10, delay='0.232315346482ms')
        self.addLink( BISM , ALBU, bw=10, delay='1.07297714274ms')

topos = { 'att': ( lambda: AttMplsTopoExt() ) }
