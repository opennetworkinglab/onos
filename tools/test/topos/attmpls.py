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

class AttMplsTopo( Topo ):
    "Internet Topology Zoo Specimen."

    def __init__( self ):
        "Create a topology."

        # Initialize Topology
        Topo.__init__( self )

        # add nodes, switches first...
        NY54 = self.addSwitch( 's25' ) # 40.728270, -73.994483
        CMBR = self.addSwitch( 's1' )  # 42.373730, -71.109734
        CHCG = self.addSwitch( 's2', protocols='OpenFlow13' )  # 41.877461, -87.642892
        CLEV = self.addSwitch( 's3' )  # 41.498928, -81.695217
        RLGH = self.addSwitch( 's4' )  # 35.780150, -78.644026
        ATLN = self.addSwitch( 's5' )  # 33.749017, -84.394168
        PHLA = self.addSwitch( 's6' )  # 39.952906, -75.172278
        WASH = self.addSwitch( 's7' )  # 38.906696, -77.035509
        NSVL = self.addSwitch( 's8' )  # 36.166410, -86.787305
        STLS = self.addSwitch( 's9', protocols='OpenFlow13' )  # 38.626418, -90.198143
        NWOR = self.addSwitch( 's10' ) # 29.951475, -90.078434
        HSTN = self.addSwitch( 's11' ) # 29.763249, -95.368332
        SNAN = self.addSwitch( 's12' ) # 29.424331, -98.491745
        DLLS = self.addSwitch( 's13' ) # 32.777665, -96.802064
        ORLD = self.addSwitch( 's14' ) # 28.538641, -81.381110
        DNVR = self.addSwitch( 's15' ) # 39.736623, -104.984887
        KSCY = self.addSwitch( 's16' ) # 39.100725, -94.581228
        SNFN = self.addSwitch( 's17' ) # 37.779751, -122.409791
        SCRM = self.addSwitch( 's18' ) # 38.581001, -121.497844
        PTLD = self.addSwitch( 's19' ) # 45.523317, -122.677768
        STTL = self.addSwitch( 's20' ) # 47.607326, -122.331786
        SLKC = self.addSwitch( 's21' ) # 40.759577, -111.895079
        LA03 = self.addSwitch( 's22', protocols='OpenFlow13' ) # 34.056346, -118.235951
        SNDG = self.addSwitch( 's23' ) # 32.714564, -117.153528
        PHNX = self.addSwitch( 's24' ) # 33.448289, -112.076299

        # ... and now hosts
        NY54_host = self.addHost( 'h25' )
        CMBR_host = self.addHost( 'h1' )
        CHCG_host = self.addHost( 'h2' )
        CLEV_host = self.addHost( 'h3' )
        RLGH_host = self.addHost( 'h4' )
        ATLN_host = self.addHost( 'h5' )
        PHLA_host = self.addHost( 'h6' )
        WASH_host = self.addHost( 'h7' )
        NSVL_host = self.addHost( 'h8' )
        STLS_host = self.addHost( 'h9' )
        NWOR_host = self.addHost( 'h10' )
        HSTN_host = self.addHost( 'h11' )
        SNAN_host = self.addHost( 'h12' )
        DLLS_host = self.addHost( 'h13' )
        ORLD_host = self.addHost( 'h14' )
        DNVR_host = self.addHost( 'h15' )
        KSCY_host = self.addHost( 'h16' )
        SNFN_host = self.addHost( 'h17' )
        SCRM_host = self.addHost( 'h18' )
        PTLD_host = self.addHost( 'h19' )
        STTL_host = self.addHost( 'h20' )
        SLKC_host = self.addHost( 'h21' )
        LA03_host = self.addHost( 'h22' )
        SNDG_host = self.addHost( 'h23' )
        PHNX_host = self.addHost( 'h24' )

        # add edges between switch and corresponding host
        self.addLink( NY54 , NY54_host )
        self.addLink( CMBR , CMBR_host )
        self.addLink( CHCG , CHCG_host )
        self.addLink( CLEV , CLEV_host )
        self.addLink( RLGH , RLGH_host )
        self.addLink( ATLN , ATLN_host )
        self.addLink( PHLA , PHLA_host )
        self.addLink( WASH , WASH_host )
        self.addLink( NSVL , NSVL_host )
        self.addLink( STLS , STLS_host )
        self.addLink( NWOR , NWOR_host )
        self.addLink( HSTN , HSTN_host )
        self.addLink( SNAN , SNAN_host )
        self.addLink( DLLS , DLLS_host )
        self.addLink( ORLD , ORLD_host )
        self.addLink( DNVR , DNVR_host )
        self.addLink( KSCY , KSCY_host )
        self.addLink( SNFN , SNFN_host )
        self.addLink( SCRM , SCRM_host )
        self.addLink( PTLD , PTLD_host )
        self.addLink( STTL , STTL_host )
        self.addLink( SLKC , SLKC_host )
        self.addLink( LA03 , LA03_host )
        self.addLink( SNDG , SNDG_host )
        self.addLink( PHNX , PHNX_host )

        # add edges between switches
        self.addLink( NY54 , CMBR, bw=10, delay='0.979030824185ms')
        self.addLink( NY54 , CMBR, bw=10, delay='0.979030824185ms')
        self.addLink( NY54 , CMBR, bw=10, delay='0.979030824185ms')
        self.addLink( NY54 , CHCG, bw=10, delay='0.806374975652ms')
        self.addLink( NY54 , PHLA, bw=10, delay='0.686192970166ms')
        self.addLink( NY54 , PHLA, bw=10, delay='0.686192970166ms')
        self.addLink( NY54 , WASH, bw=10, delay='0.605826192092ms')
        self.addLink( CMBR , PHLA, bw=10, delay='1.4018238197ms')
        self.addLink( CHCG , CLEV, bw=10, delay='0.232315346482ms')
        self.addLink( CHCG , PHLA, bw=10, delay='1.07297714274ms')
        self.addLink( CHCG , STLS, bw=10, delay='1.12827896944ms')
        self.addLink( CHCG , DNVR, bw=10, delay='1.35964770335ms')
        self.addLink( CHCG , KSCY, bw=10, delay='1.5199778541ms')
        self.addLink( CHCG , KSCY, bw=10, delay='1.5199778541ms')
        self.addLink( CHCG , SNFN, bw=10, delay='0.620743405435ms')
        self.addLink( CHCG , STTL, bw=10, delay='0.93027212534ms')
        self.addLink( CHCG , SLKC, bw=10, delay='0.735621751348ms')
        self.addLink( CLEV , NSVL, bw=10, delay='0.523419372248ms')
        self.addLink( CLEV , STLS, bw=10, delay='1.00360290845ms')
        self.addLink( CLEV , PHLA, bw=10, delay='0.882912133249ms')
        self.addLink( RLGH , ATLN, bw=10, delay='1.1644489729ms')
        self.addLink( RLGH , WASH, bw=10, delay='1.48176810502ms')
        self.addLink( ATLN , WASH, bw=10, delay='0.557636936322ms')
        self.addLink( ATLN , NSVL, bw=10, delay='1.32869749865ms')
        self.addLink( ATLN , STLS, bw=10, delay='0.767705554748ms')
        self.addLink( ATLN , DLLS, bw=10, delay='0.544782086448ms')
        self.addLink( ATLN , DLLS, bw=10, delay='0.544782086448ms')
        self.addLink( ATLN , DLLS, bw=10, delay='0.544782086448ms')
        self.addLink( ATLN , ORLD, bw=10, delay='1.46119152532ms')
        self.addLink( PHLA , WASH, bw=10, delay='0.372209320106ms')
        self.addLink( NSVL , STLS, bw=10, delay='1.43250491305ms')
        self.addLink( NSVL , DLLS, bw=10, delay='1.67698215288ms')
        self.addLink( STLS , DLLS, bw=10, delay='0.256389964194ms')
        self.addLink( STLS , KSCY, bw=10, delay='0.395511571791ms')
        self.addLink( STLS , LA03, bw=10, delay='0.257085227363ms')
        self.addLink( NWOR , HSTN, bw=10, delay='0.0952906633914ms')
        self.addLink( NWOR , DLLS, bw=10, delay='1.60231329739ms')
        self.addLink( NWOR , ORLD, bw=10, delay='0.692731063896ms')
        self.addLink( HSTN , SNAN, bw=10, delay='0.284150653798ms')
        self.addLink( HSTN , DLLS, bw=10, delay='1.65690128332ms')
        self.addLink( HSTN , ORLD, bw=10, delay='0.731886304782ms')
        self.addLink( SNAN , PHNX, bw=10, delay='1.34258627257ms')
        self.addLink( SNAN , DLLS, bw=10, delay='1.50063532341ms')
        self.addLink( DLLS , DNVR, bw=10, delay='0.251471593235ms')
        self.addLink( DLLS , DNVR, bw=10, delay='0.251471593235ms')
        self.addLink( DLLS , KSCY, bw=10, delay='0.18026026737ms')
        self.addLink( DLLS , KSCY, bw=10, delay='0.18026026737ms')
        self.addLink( DLLS , SNFN, bw=10, delay='0.74304274592ms')
        self.addLink( DLLS , LA03, bw=10, delay='0.506439293357ms')
        self.addLink( DLLS , LA03, bw=10, delay='0.506439293357ms')
        self.addLink( DNVR , KSCY, bw=10, delay='0.223328790403ms')
        self.addLink( DNVR , SNFN, bw=10, delay='0.889017541903ms')
        self.addLink( DNVR , SNFN, bw=10, delay='0.889017541903ms')
        self.addLink( DNVR , SLKC, bw=10, delay='0.631898982721ms')
        self.addLink( KSCY , SNFN, bw=10, delay='0.922778522233ms')
        self.addLink( SNFN , SCRM, bw=10, delay='0.630352278097ms')
        self.addLink( SNFN , PTLD, bw=10, delay='0.828572513655ms')
        self.addLink( SNFN , STTL, bw=10, delay='1.54076081649ms')
        self.addLink( SNFN , SLKC, bw=10, delay='0.621507502625ms')
        self.addLink( SNFN , LA03, bw=10, delay='0.602936230151ms')
        self.addLink( SNFN , LA03, bw=10, delay='0.602936230151ms')
        self.addLink( SNFN , LA03, bw=10, delay='0.602936230151ms')
        self.addLink( SCRM , SLKC, bw=10, delay='0.461350343644ms')
        self.addLink( PTLD , STTL, bw=10, delay='1.17591515181ms')
        self.addLink( SLKC , LA03, bw=10, delay='0.243225267023ms')
        self.addLink( LA03 , SNDG, bw=10, delay='0.681264950821ms')
        self.addLink( LA03 , SNDG, bw=10, delay='0.681264950821ms')
        self.addLink( LA03 , PHNX, bw=10, delay='0.343709457969ms')
        self.addLink( LA03 , PHNX, bw=10, delay='0.343709457969ms')
        self.addLink( SNDG , PHNX, bw=10, delay='0.345064487693ms')

topos = { 'att': ( lambda: AttMplsTopo() ) }

if __name__ == '__main__':
    from onosnet import run
    run( AttMplsTopo() )
