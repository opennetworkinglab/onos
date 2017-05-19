#!/usr/bin/env python

"""
"""
from mininet.topo import Topo

class AttMplsTopo( Topo ):
    "Internet Topology Zoo Specimen."

    def addSwitch( self, name, **opts ):
        kwargs = { 'protocols' : 'OpenFlow13' }
        kwargs.update( opts )
        return super(AttMplsTopo, self).addSwitch( name, **kwargs )

    def __init__( self ):
        "Create a topology."

        # Initialize Topology
        Topo.__init__( self )

        # add nodes, switches first...
        NY54 = self.addSwitch( 's25' ) # 40.728270, -73.994483
        CMBR = self.addSwitch( 's1' )  # 42.373730, -71.109734
        CHCG = self.addSwitch( 's2' )  # 41.877461, -87.642892
        CLEV = self.addSwitch( 's3' )  # 41.498928, -81.695217
        RLGH = self.addSwitch( 's4' )  # 35.780150, -78.644026
        ATLN = self.addSwitch( 's5' )  # 33.749017, -84.394168
        PHLA = self.addSwitch( 's6' )  # 39.952906, -75.172278
        WASH = self.addSwitch( 's7' )  # 38.906696, -77.035509
        NSVL = self.addSwitch( 's8' )  # 36.166410, -86.787305
        STLS = self.addSwitch( 's9' )  # 38.626418, -90.198143
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
        LA03 = self.addSwitch( 's22' ) # 34.056346, -118.235951
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
        self.addLink( NY54 , CMBR)
        self.addLink( NY54 , CMBR)
        self.addLink( NY54 , CMBR)
        self.addLink( NY54 , CHCG)
        self.addLink( NY54 , PHLA)
        self.addLink( NY54 , PHLA)
        self.addLink( NY54 , WASH)
        self.addLink( CMBR , PHLA)
        self.addLink( CHCG , CLEV)
        self.addLink( CHCG , PHLA)
        self.addLink( CHCG , STLS)
        self.addLink( CHCG , DNVR)
        self.addLink( CHCG , KSCY)
        self.addLink( CHCG , KSCY)
        self.addLink( CHCG , SNFN)
        self.addLink( CHCG , STTL)
        self.addLink( CHCG , SLKC)
        self.addLink( CLEV , NSVL)
        self.addLink( CLEV , STLS)
        self.addLink( CLEV , PHLA)
        self.addLink( RLGH , ATLN)
        self.addLink( RLGH , WASH)
        self.addLink( ATLN , WASH)
        self.addLink( ATLN , NSVL)
        self.addLink( ATLN , STLS)
        self.addLink( ATLN , DLLS)
        self.addLink( ATLN , DLLS)
        self.addLink( ATLN , DLLS)
        self.addLink( ATLN , ORLD)
        self.addLink( PHLA , WASH)
        self.addLink( NSVL , STLS)
        self.addLink( NSVL , DLLS)
        self.addLink( STLS , DLLS)
        self.addLink( STLS , KSCY)
        self.addLink( STLS , LA03)
        self.addLink( NWOR , HSTN)
        self.addLink( NWOR , DLLS)
        self.addLink( NWOR , ORLD)
        self.addLink( HSTN , SNAN)
        self.addLink( HSTN , DLLS)
        self.addLink( HSTN , ORLD)
        self.addLink( SNAN , PHNX)
        self.addLink( SNAN , DLLS)
        self.addLink( DLLS , DNVR)
        self.addLink( DLLS , DNVR)
        self.addLink( DLLS , KSCY)
        self.addLink( DLLS , KSCY)
        self.addLink( DLLS , SNFN)
        self.addLink( DLLS , LA03)
        self.addLink( DLLS , LA03)
        self.addLink( DNVR , KSCY)
        self.addLink( DNVR , SNFN)
        self.addLink( DNVR , SNFN)
        self.addLink( DNVR , SLKC)
        self.addLink( KSCY , SNFN)
        self.addLink( SNFN , SCRM)
        self.addLink( SNFN , PTLD)
        self.addLink( SNFN , STTL)
        self.addLink( SNFN , SLKC)
        self.addLink( SNFN , LA03)
        self.addLink( SNFN , LA03)
        self.addLink( SNFN , LA03)
        self.addLink( SCRM , SLKC)
        self.addLink( PTLD , STTL)
        self.addLink( SLKC , LA03)
        self.addLink( LA03 , SNDG)
        self.addLink( LA03 , SNDG)
        self.addLink( LA03 , PHNX)
        self.addLink( LA03 , PHNX)
        self.addLink( SNDG , PHNX)

topos = { 'att': ( lambda: AttMplsTopo() ) }

if __name__ == '__main__':
    from onosnet import run
    run( AttMplsTopo() )
