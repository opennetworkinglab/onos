
from mininet.topo import Topo

class MyTopo( Topo ):
        "10 'floating' switch topology"

        def __init__( self ):
                # Initialize topology
                Topo.__init__( self )

                sw_list = []
                swC = self.addSwitch('sc', dpid = 'ffffffff00000001')

                for i in range(1, 201):
                        switch=self.addSwitch('s'+str(i), dpid = str(i).zfill(16))
                        self.addLink(switch,swC)

                        sw_list.append(switch)

topos = { 'mytopo': ( lambda: MyTopo() ) }
