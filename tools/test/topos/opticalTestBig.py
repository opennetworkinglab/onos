#!/usr/bin/python

from opticalUtils import MininetOE, LINCSwitch, LINCLink
from mininet.topo import Topo
from mininet.log import setLogLevel
from mininet.node import RemoteController
from mininet.cli import CLI

class BigOpticalTopo( Topo ):

	def build( self ):
		# Optical layer ROADMs
		o1ann = { "latitude": 32.508086, "longitude": -99.741564, "optical.regens": 0 }
		O1 = self.addSwitch( 'ABLNTXRO', dpid='0000ffffffffff01', annotations=o1ann, cls=LINCSwitch )
		o2ann = { "latitude": 35.084446, "longitude": -106.649719, "optical.regens": 0 }
		O2 = self.addSwitch( 'ALBQNMMA', dpid='0000ffffffffff02', annotations=o2ann, cls=LINCSwitch )
		o3ann = { "latitude": 42.652222, "longitude":	-73.758333, "optical.regens": 0 }
		O3 = self.addSwitch( 'ALBYNYSS', dpid='0000ffffffffff03', annotations=o3ann, cls=LINCSwitch )
		o4ann = { "latitude": 33.755833, "longitude":	-97.743057, "optical.regens": 5 }
		O4 = self.addSwitch( 'ATLNGATL', dpid='0000ffffffffff04', annotations=o4ann, cls=LINCSwitch )  # ATLNGATL Connected to packet node  
		o5ann = { "latitude": 42.882778, "longitude":	-78.877778, "optical.regens": 0 }
		O5 = self.addSwitch( 'BFLONYFR', dpid='0000ffffffffff05', annotations=o5ann, cls=LINCSwitch )
		o6ann = { "latitude": 45.781667, "longitude":	-108.509167, "optical.regens": 0 }
		O6 = self.addSwitch( 'BLNGMTMA', dpid='0000ffffffffff06', annotations=o6ann, cls=LINCSwitch )
		o7ann = { "latitude": 39.293781, "longitude":	-76.614127, "optical.regens": 0 }
		O7 = self.addSwitch( 'BLTMMDCH', dpid='0000ffffffffff07', annotations=o7ann, cls=LINCSwitch )
		o8ann = { "latitude": 33.517223, "longitude":	-86.812225, "optical.regens": 0 }
		O8 = self.addSwitch( 'BRHMALMT', dpid='0000ffffffffff08', annotations=o8ann, cls=LINCSwitch )
		o9ann = { "latitude": 46.836379, "longitude":	-100.796917, "optical.regens": 0 }
		O9 = self.addSwitch( 'BSMRNDJC', dpid='0000ffffffffff09', annotations=o9ann, cls=LINCSwitch )
		o10ann = { "latitude": 30.449722, "longitude":	-91.184167, "optical.regens": 0 }
		O10 = self.addSwitch( 'BTRGLAMA', dpid='0000ffffffffff0a', annotations=o10ann, cls=LINCSwitch )
		o11ann = { "latitude": 41.881484, "longitude":	-87.640432, "optical.regens": 4 }
		O11 = self.addSwitch( 'CHCGILCL', dpid='0000ffffffffff0b', annotations=o11ann, cls=LINCSwitch )
		o12ann = { "latitude": 35.224924, "longitude":	-80.837502, "optical.regens": 0 }
		O12 = self.addSwitch( 'CHRLNCCA', dpid='0000ffffffffff0c', annotations=o12ann, cls=LINCSwitch )
		o13ann = { "latitude": 32.785278, "longitude":	-79.938056, "optical.regens": 0 }
		O13 = self.addSwitch( 'CHTNSCDT', dpid='0000ffffffffff0d', annotations=o13ann, cls=LINCSwitch )
		o14ann = { "latitude": 41.498333, "longitude":	-81.686943, "optical.regens": 0 }
		O14 = self.addSwitch( 'CLEVOH02', dpid='0000ffffffffff0e', annotations=o14ann, cls=LINCSwitch )
		o15ann = { "latitude": 39.965279, "longitude":	-82.996666, "optical.regens": 0 }
		O15 = self.addSwitch( 'CLMBOH11', dpid='0000ffffffffff0f', annotations=o15ann, cls=LINCSwitch )
		o16ann = { "latitude": 42.36745, "longitude":	-71.084918, "optical.regens": 0 }
		O16 = self.addSwitch( 'CMBRMA01', dpid='0000ffffffffff10', annotations=o16ann, cls=LINCSwitch )
		o17ann = { "latitude": 39.102778, "longitude":	-84.516944, "optical.regens": 0 }
		O17 = self.addSwitch( 'CNCNOHWS', dpid='0000ffffffffff11', annotations=o17ann, cls=LINCSwitch )
		o18ann = { "latitude": 32.797524, "longitude":	-96.780431, "optical.regens": 0 }
		O18 = self.addSwitch( 'DLLSTXTL', dpid='0000ffffffffff12', annotations=o18ann, cls=LINCSwitch )   # DLLSTXTL Connected to packet node 
		o19ann = { "latitude": 39.744999, "longitude":	-104.996391, "optical.regens": 0 }
		O19 = self.addSwitch( 'DNVRCOMA', dpid='0000ffffffffff13', annotations=o19ann, cls=LINCSwitch )
		o20ann = { "latitude": 42.332779, "longitude":	-83.054169, "optical.regens": 5 }
		O20 = self.addSwitch( 'DTRTMIBA', dpid='0000ffffffffff14', annotations=o20ann, cls=LINCSwitch )   
		o21ann = { "latitude": 31.756389, "longitude":	-106.483611, "optical.regens": 0 }
		O21 = self.addSwitch( 'ELPSTXMA', dpid='0000ffffffffff15', annotations=o21ann, cls=LINCSwitch )
		o22ann = { "latitude": 36.73923, "longitude": -119.79423, "optical.regens": 0 }
		O22 = self.addSwitch( 'FRSNCA01', dpid='0000ffffffffff16', annotations=o22ann, cls=LINCSwitch )
		o23ann = { "latitude": 36.072222, "longitude":	-79.793889, "optical.regens": 0 }
		O23 = self.addSwitch( 'GNBONCEU', dpid='0000ffffffffff17', annotations=o23ann, cls=LINCSwitch )
		o24ann = { "latitude": 41.765833, "longitude":	-72.676389, "optical.regens": 0 }
		O24 = self.addSwitch( 'HRFRCT03', dpid='0000ffffffffff18', annotations=o24ann, cls=LINCSwitch )
		o25ann = { "latitude": 29.748333, "longitude":	-95.36528, "optical.regens": 0 }
		O25 = self.addSwitch( 'HSTNTX01', dpid='0000ffffffffff19', annotations=o25ann, cls=LINCSwitch )
		o26ann = { "latitude": 30.33071, "longitude":	-81.43, "optical.regens": 0 }
		O26 = self.addSwitch( 'JCVLFLCL', dpid='0000ffffffffff1a', annotations=o26ann, cls=LINCSwitch )
		o27ann = { "latitude": 39.096649, "longitude":	-94.578716, "optical.regens": 0 }
		O27 = self.addSwitch( 'KSCYMO09', dpid='0000ffffffffff1b', annotations=o27ann, cls=LINCSwitch )
		o28ann = { "latitude": 40.5899999,"longitude":	-73.6699993, "optical.regens": 0 }
		O28 = self.addSwitch( 'LGISLAND', dpid='0000ffffffffff1c', annotations=o28ann, cls=LINCSwitch )    
		o29ann = { "latitude": 34.051227, "longitude":	-118.252958, "optical.regens": 0 }
		O29 = self.addSwitch( 'LSANCA03', dpid='0000ffffffffff1d', annotations=o29ann, cls=LINCSwitch ) # LSANCA03 Connected to packet node 
		o30ann = { "latitude": 36.168056, "longitude":	-115.138889, "optical.regens": 0 }
		O30 = self.addSwitch( 'LSVGNV02', dpid='0000ffffffffff1e', annotations=o30ann, cls=LINCSwitch )
		o31ann = { "latitude":38.249167, "longitude":	-85.760833, "optical.regens": 0 }
		O31 = self.addSwitch( 'LSVLKYCS', dpid='0000ffffffffff1f', annotations=o31ann, cls=LINCSwitch )
		o32ann = { "latitude": 34.740833, "longitude":	-92.271942, "optical.regens": 2 }
		O32 = self.addSwitch( 'LTRKARFR', dpid='0000ffffffffff20', annotations=o32ann, cls=LINCSwitch )
		o33ann = { "latitude": 25.779167, "longitude":	-80.195, "optical.regens": 0 }
		O33 = self.addSwitch( 'MIAMFLAC', dpid='0000ffffffffff21', annotations=o33ann, cls=LINCSwitch )
		o34ann = { "latitude": 43.037224, "longitude":	-87.922501, "optical.regens": 0 }
		O34 = self.addSwitch( 'MILWWIHE', dpid='0000ffffffffff22', annotations=o34ann, cls=LINCSwitch )
		o35ann = { "latitude": 35.145158, "longitude":	-90.048058, "optical.regens": 0 }
		O35 = self.addSwitch( 'MMPHTNMA', dpid='0000ffffffffff23', annotations=o35ann, cls=LINCSwitch )
		o36ann = { "latitude": 44.977365, "longitude":	-93.26718, "optical.regens": 0 }
		O36 = self.addSwitch( 'MPLSMNDT', dpid='0000ffffffffff24', annotations=o36ann, cls=LINCSwitch )   # MPLSMNDT Connected to packet node 
		o37ann = { "latitude": 36.853333, "longitude":	-76.29, "optical.regens": 0 }
		O37 = self.addSwitch( 'NRFLVABS', dpid='0000ffffffffff25', annotations=o37ann, cls=LINCSwitch )
		o38ann = { "latitude": 36.163955, "longitude":	-86.775558, "optical.regens": 0 }
		O38 = self.addSwitch( 'NSVLTNMT', dpid='0000ffffffffff26', annotations=o38ann, cls=LINCSwitch )
		o39ann = { "latitude": 29.949806, "longitude":	-90.07222, "optical.regens": 0 }
		O39 = self.addSwitch( 'NWORLAMA', dpid='0000ffffffffff27', annotations=o39ann, cls=LINCSwitch )
		o40ann = { "latitude": 40.734408, "longitude":	-74.177978, "optical.regens": 0 }
		O40 = self.addSwitch( 'NWRKNJ02', dpid='0000ffffffffff28', annotations=o40ann, cls=LINCSwitch )  # NWRKNJ02 Connected to packet node 
		o41ann = { "latitude": 40.767497, "longitude":	-73.989713, "optical.regens": 0 }
		O41 = self.addSwitch( 'NYCMNY54', dpid='0000ffffffffff29', annotations=o41ann, cls=LINCSwitch )
		o42ann = { "latitude": 35.470833, "longitude":	-97.515274, "optical.regens": 0 }
		O42 = self.addSwitch( 'OKCYOKCE', dpid='0000ffffffffff2a', annotations=o42ann, cls=LINCSwitch )
		o43ann = { "latitude": 37.805556, "longitude":	-122.268889, "optical.regens": 2 }
		O43 = self.addSwitch( 'OKLDCA03', dpid='0000ffffffffff2b', annotations=o43ann, cls=LINCSwitch )
		o44ann = { "latitude": 41.259167, "longitude":-95.940277, "optical.regens": 0 }
		O44 = self.addSwitch( 'OMAHNENW', dpid='0000ffffffffff2c', annotations=o44ann, cls=LINCSwitch )
		o45ann = { "latitude": 28.543279, "longitude":	-81.377502, "optical.regens": 0 }
		O45 = self.addSwitch( 'ORLDFLMA', dpid='0000ffffffffff2d', annotations=o45ann, cls=LINCSwitch )  # ORLDFLMA Connected to packet node 
		o46ann = { "latitude": 39.946446, "longitude":	-75.184139, "optical.regens": 0 }
		O46 = self.addSwitch( 'PHLAPASL', dpid='0000ffffffffff2e', annotations=o46ann, cls=LINCSwitch )
		o47ann = { "latitude": 33.450361, "longitude":	-112.07709, "optical.regens": 0 }
		O47 = self.addSwitch( 'PHNXAZMA', dpid='0000ffffffffff2f', annotations=o47ann, cls=LINCSwitch )  # PHNXAZMA Connected to packet node 
		o48ann = { "latitude":40.441387, "longitude":	-79.995552, "optical.regens": 0 }
		O48 = self.addSwitch( 'PITBPADG', dpid='0000ffffffffff30', annotations=o48ann, cls=LINCSwitch )
		o49ann = { "latitude":41.818889, "longitude":	-71.415278, "optical.regens": 0 }
		O49 = self.addSwitch( 'PRVDRIGR', dpid='0000ffffffffff31', annotations=o49ann, cls=LINCSwitch )
		o50ann = { "latitude": 45.522499, "longitude":	-122.678055, "optical.regens": 2 }
		O50 = self.addSwitch( 'PTLDOR62', dpid='0000ffffffffff32', annotations=o50ann, cls=LINCSwitch )
		o51ann = { "latitude": 37.540752, "longitude":	-77.436096, "optical.regens": 0 }
		O51 = self.addSwitch( 'RCMDVAGR', dpid='0000ffffffffff33', annotations=o51ann, cls=LINCSwitch )
		o52ann = { "latitude": 35.779656, "longitude":	-78.640831, "optical.regens": 0 }
		O52 = self.addSwitch( 'RLGHNCMO', dpid='0000ffffffffff34', annotations=o52ann, cls=LINCSwitch )
		o53ann = { "latitude": 43.157222, "longitude":	-77.616389, "optical.regens": 0 }
		O53 = self.addSwitch( 'ROCHNYXA', dpid='0000ffffffffff35', annotations=o53ann, cls=LINCSwitch )  # ROCHNYXA Connected to packet node 
		o54ann = { "latitude": 38.578609, "longitude":	-121.487221, "optical.regens": 0 }
		O54 = self.addSwitch( 'SCRMCA01', dpid='0000ffffffffff36', annotations=o54ann, cls=LINCSwitch )
		o55ann = { "latitude": 41.415278, "longitude":	-75.649167, "optical.regens": 0 }
		O55 = self.addSwitch( 'SCTNPA01', dpid='0000ffffffffff37', annotations=o55ann, cls=LINCSwitch )
		o56ann = { "latitude": 40.767776, "longitude":	-111.888336, "optical.regens": 0 }
		O56 = self.addSwitch( 'SLKCUTMA', dpid='0000ffffffffff38', annotations=o56ann, cls=LINCSwitch )
		o57ann = { "latitude": 29.429445, "longitude":	-98.488892, "optical.regens": 0 }
		O57 = self.addSwitch( 'SNANTXCA', dpid='0000ffffffffff39', annotations=o57ann, cls=LINCSwitch )  # SNANTXCA Connected to packet node 
		o58ann = { "latitude": 34.418889, "longitude":	-119.7, "optical.regens": 0 }
		O58 = self.addSwitch( 'SNBBCA01', dpid='0000ffffffffff3a', annotations=o58ann, cls=LINCSwitch )
		o59ann = { "latitude":32.746944, "longitude":	-117.158611, "optical.regens": 0 }
		O59 = self.addSwitch( 'SNDGCA02', dpid='0000ffffffffff3b', annotations=o59ann, cls=LINCSwitch )
		o60ann = { "latitude":37.785143, "longitude":	-122.397263, "optical.regens": 0 }
		O60 = self.addSwitch( 'SNFCCA21', dpid='0000ffffffffff3c', annotations=o60ann, cls=LINCSwitch )
		o61ann = { "latitude": 37.333333, "longitude":	-121.892778, "optical.regens": 0 }
		O61 = self.addSwitch( 'SNJSCA02', dpid='0000ffffffffff3d', annotations=o61ann, cls=LINCSwitch )   # SNJSCA02 Connected to packet node 
		o62ann = { "latitude": 39.795278, "longitude":	-89.649444, "optical.regens": 0 }
		O62 = self.addSwitch( 'SPFDILSD', dpid='0000ffffffffff3e', annotations=o62ann, cls=LINCSwitch )
		o63ann = { "latitude": 47.654724, "longitude":	-117.419167, "optical.regens": 0 }
		O63 = self.addSwitch( 'SPKNWA01', dpid='0000ffffffffff3f', annotations=o63ann, cls=LINCSwitch )
		o64ann = { "latitude": 38.633335, "longitude":	-90.215279, "optical.regens": 0 }
		O64 = self.addSwitch( 'STLSMO09', dpid='0000ffffffffff40', annotations=o64ann, cls=LINCSwitch )
		o65ann = { "latitude": 47.606945, "longitude":	-122.333336, "optical.regens": 0 }
		O65 = self.addSwitch( 'STTLWA06', dpid='0000ffffffffff41', annotations=o65ann, cls=LINCSwitch )
		o66ann = { "latitude": 43.049444, "longitude":	-76.1475, "optical.regens": 3 }
		O66 = self.addSwitch( 'SYRCNYSU', dpid='0000ffffffffff42', annotations=o66ann, cls=LINCSwitch )
		o67ann = { "latitude": 28.0225,   "longitude": -82.522778, "optical.regens": 0 }
		O67 = self.addSwitch( 'TAMQFLFN', dpid='0000ffffffffff43', annotations=o67ann, cls=LINCSwitch )
		o68ann = { "latitude": 32.224444, "longitude":	-110.968333, "optical.regens": 0 }
		O68 = self.addSwitch( 'TCSNAZMA', dpid='0000ffffffffff44', annotations=o68ann, cls=LINCSwitch )
		o69ann = { "latitude": 30.456389, "longitude": -84.290833, "optical.regens": 0 }
		O69 = self.addSwitch( 'TLHSFLAT', dpid='0000ffffffffff45', annotations=o69ann, cls=LINCSwitch )
		o70ann = { "latitude": 41.65,     "longitude": -83.538056, "optical.regens": 2 }
		O70 = self.addSwitch( 'TOLDOH21', dpid='0000ffffffffff46', annotations=o70ann, cls=LINCSwitch )
		o71ann = { "latitude": 36.151669, "longitude": -95.985832, "optical.regens": 0 }
		O71 = self.addSwitch( 'TULSOKTB', dpid='0000ffffffffff47', annotations=o71ann, cls=LINCSwitch )
		o72ann = { "latitude": 38.88306	, "longitude": -77.01028, "optical.regens": 0 }
		O72 = self.addSwitch( 'WASHDCSW', dpid='0000ffffffffff48', annotations=o72ann, cls=LINCSwitch )  # WASHDCSW  Connected to packet node
		o73ann = { "latitude": 39.739167, "longitude": -75.553889, "optical.regens": 0 }
		O73 = self.addSwitch( 'WLMGDE01', dpid='0000ffffffffff49', annotations=o73ann, cls=LINCSwitch )
		o74ann = { "latitude": 26.709391, "longitude": -80.05278, "optical.regens": 0 }
		O74 = self.addSwitch( 'WPBHFLAN', dpid='0000ffffffffff4a', annotations=o74ann, cls=LINCSwitch )
		o75ann = { "latitude": 29.57, "longitude": -96.7, "optical.regens": 0 }
		O75 = self.addSwitch( 'AUSTTXGR', dpid='0000ffffffffff4b', annotations=o75ann, cls=LINCSwitch )
		#o25ann = { "latitude": 29.748333, "longitude":	-95.36528, "optical.regens": 0 }
		#o57ann = { "latitude": 29.429445, "longitude":	-98.488892, "optical.regens": 0 }


		# Packet Layer switches  
		''' # from opticalTest.py
        SFOR10 = self.addSwitch( 'SFO-R10', dpid='0000ffffffff0001', annotations={"latitude": 37.6, "longitude": -122.3} )
        LAXR10 = self.addSwitch( 'LAX-R10', dpid='0000ffffffff0002', annotations={ "latitude": 33.9, "longitude": -118.4 } )
        SDGR10 = self.addSwitch( 'SDG-R10', dpid='0000ffffffff0003', annotations={ "latitude": 32.8, "longitude": -117.1 } )
        CHGR10 = self.addSwitch( 'CHG-R10', dpid='0000ffffffff0004', annotations={ "latitude": 41.8, "longitude": -87.6 } )
        JFKR10 = self.addSwitch( 'JFK-R10', dpid='0000ffffffff0005', annotations={ "latitude": 40.8, "longitude": -73.1 } )
        ATLR10 = self.addSwitch( 'ATL-R10', dpid='0000ffffffff0006', annotations={ "latitude": 33.8, "longitude": -84.1 } )
		'''
		WASHDCSWR = self.addSwitch( 'WASHDCSW-R', dpid='0000ffffff000001', annotations={ "latitude": 38.8, "longitude": -77.0 } )      # this switch is O72 
		SNJSCA02R = self.addSwitch( 'SNJSCA02-R', dpid='0000ffffff000002', annotations={ "latitude": 37.3, "longitude": -121.8 } )     # O61 
		SNANTXCAR = self.addSwitch( 'SNANTXCA-R', dpid='0000ffffff000003', annotations={ "latitude": 29.4, "longitude": -98.4 } )      # O57
		ROCHNYXAR = self.addSwitch( 'ROCHNYXA-R', dpid='0000ffffff000004', annotations={ "latitude": 43.1, "longitude": -77.6 } )      # O53
		PHNXAZMAR = self.addSwitch( 'PHNXAZMA-R', dpid='0000ffffff000005', annotations={ "latitude": 33.4, "longitude": -112.0 } )     # O47
		ORLDFLMAR = self.addSwitch( 'ORLDFLMA-R', dpid='0000ffffff000006', annotations={ "latitude": 28.5, "longitude": -81.3 } )      # O45
		NWRKNJ02R = self.addSwitch( 'NWRKNJ02-R', dpid='0000ffffff000007', annotations={ "latitude": 40.7, "longitude": -74.1 } )      # O40
		MPLSMNDTR = self.addSwitch( 'MPLSMNDT-R', dpid='0000ffffff000008', annotations={ "latitude": 44.9, "longitude": -93.2 } )      # O36
		LSANCA03R = self.addSwitch( 'LSANCA03-R', dpid='0000ffffff000009', annotations={ "latitude": 34.1, "longitude":	-118.3 } )    # O29 
		DLLSTXTLR = self.addSwitch( 'DLLSTXTL-R', dpid='0000ffffff00000a', annotations={ "latitude": 32.7, "longitude": -96.7 } )      # O18
		ATLNGATLR = self.addSwitch( 'ATLNGATL-R', dpid='0000ffffff00000b', annotations={ "latitude": 33.7, "longitude": -97.7 } )      # O4

    
  
		# Optical Links between the ROADMs (although length is not used; we should keep these for future reference)
		self.addLink( O1, O18, port1=100, port2=105, annotations={ "length": 334, "durable": "true" }, cls=LINCLink )  # ABLNTXRO  DLLSTXTL
		self.addLink( O1, O21, port1=101, port2=106, annotations={ "length": 768, "durable": "true" }, cls=LINCLink )  # ABLNTXRO  ELPSTXMA
		self.addLink( O3, O16, port1=100, port2=105, annotations={ "length": 265, "durable": "true" }, cls=LINCLink )  # ALBYNYSS  CMBRMA01
		self.addLink( O3, O66, port1=101, port2=106, annotations={ "length": 239, "durable": "true" }, cls=LINCLink )  # ALBYNYSS  SYRCNYSU
		self.addLink( O2, O18, port1=100, port2=107, annotations={ "length": 1134, "durable": "true" }, cls=LINCLink )  # ALBQNMMA  DLLSTXTL
		self.addLink( O2, O19, port1=101, port2=106, annotations={ "length": 646, "durable": "true" }, cls=LINCLink )  # ALBQNMMA  DNVRCOMA
		self.addLink( O2, O21, port1=102, port2=107, annotations={ "length": 444, "durable": "true" }, cls=LINCLink )  # ALBQNMMA  ELPSTXMA
		self.addLink( O2, O30, port1=103, port2=106, annotations={ "length": 931, "durable": "true" }, cls=LINCLink )  # ALBQNMMA  LSVGNV02
		self.addLink( O4, O8, port1=101, port2=105, annotations={ "length": 271, "durable": "true" }, cls=LINCLink )  # ATLNGATL  BRHMALMT
		self.addLink( O4, O12, port1=102, port2=106, annotations={ "length": 436, "durable": "true" }, cls=LINCLink )  # ATLNGATL  CHRLNCCA
		self.addLink( O4, O26, port1=103, port2=105, annotations={ "length": 566, "durable": "true" }, cls=LINCLink )  # ATLNGATL  JCVLFLCL
		self.addLink( O75, O25, port1=101, port2=106, annotations={ "length": 283, "durable": "true" }, cls=LINCLink )  # AUSTTXGR  HSTNTX01
		self.addLink( O75, O57, port1=102, port2=105, annotations={ "length": 141, "durable": "true" }, cls=LINCLink )  # AUSTTXGR  SNANTXCA
		self.addLink( O7, O46, port1=101, port2=106, annotations={ "length": 170, "durable": "true" }, cls=LINCLink )  # BLTMMDCH  PHLAPASL
		self.addLink( O7, O48, port1=102, port2=105, annotations={ "length": 378, "durable": "true" }, cls=LINCLink )  # BLTMMDCH  PITBPADG
		self.addLink( O7, O70, port1=103, port2=107, annotations={ "length": 68, "durable": "true" }, cls=LINCLink )  # BLTMMDCH  WASHDCSW
		self.addLink( O10, O25, port1=101, port2=105, annotations={ "length": 491, "durable": "true" }, cls=LINCLink )  # BTRGLAMA  HSTNTX01
		self.addLink( O10, O39, port1=102, port2=106, annotations={ "length": 144, "durable": "true" }, cls=LINCLink )  # BTRGLAMA  NWORLAMA
		self.addLink( O6, O9, port1=101, port2=105, annotations={ "length": 724, "durable": "true" }, cls=LINCLink )  # BLNGMTMA  BSMRNDJC
		self.addLink( O6, O19, port1=102, port2=107, annotations={ "length": 875, "durable": "true" }, cls=LINCLink )  # BLNGMTMA  DNVRCOMA
		self.addLink( O6, O63, port1=103, port2=105, annotations={ "length": 852, "durable": "true" }, cls=LINCLink )  # BLNGMTMA  SPKNWA01
		self.addLink( O8, O38, port1=101, port2=106, annotations={ "length": 353, "durable": "true" }, cls=LINCLink )  # BRHMALMT  NSVLTNMT
		self.addLink( O8, O39, port1=102, port2=105, annotations={ "length": 602, "durable": "true" }, cls=LINCLink )  # BRHMALMT  NWORLAMA
		self.addLink( O9, O36, port1=101, port2=106, annotations={ "length": 741, "durable": "true" }, cls=LINCLink )  # BSMRNDJC  MPLSMNDT
		self.addLink( O16, O49, port1=101, port2=105, annotations={ "length": 80, "durable": "true" }, cls=LINCLink )  # CMBRMA01  PRVDRIGR
		self.addLink( O5, O14, port1=103, port2=106, annotations={ "length": 333, "durable": "true" }, cls=LINCLink )  # BFLONYFR  CLEVOH02
		self.addLink( O5, O53, port1=104, port2=105, annotations={ "length": 128, "durable": "true" }, cls=LINCLink )  # BFLONYFR  ROCHNYXA
		self.addLink( O13, O26, port1=101, port2=107, annotations={ "length": 368, "durable": "true" }, cls=LINCLink )  # CHTNSCDT  JCVLFLCL
		self.addLink( O13, O52, port1=102, port2=105, annotations={ "length": 424, "durable": "true" }, cls=LINCLink )  # CHTNSCDT  RLGHNCMO
		self.addLink( O12, O23, port1=101, port2=106, annotations={ "length": 160, "durable": "true" }, cls=LINCLink )  # CHRLNCCA  GNBONCEU
		self.addLink( O11, O20, port1=101, port2=105, annotations={ "length": 458, "durable": "true" }, cls=LINCLink )  # CHCGILCL  DTRTMIBA
		self.addLink( O11, O34, port1=102, port2=106, annotations={ "length": 156, "durable": "true" }, cls=LINCLink )  # CHCGILCL  MILWWIHE
		self.addLink( O11, O62, port1=103, port2=105, annotations={ "length": 344, "durable": "true" }, cls=LINCLink )  # CHCGILCL  SPFDILSD
		self.addLink( O17, O15, port1=101, port2=106, annotations={ "length": 194, "durable": "true" }, cls=LINCLink )  # CNCNOHWS  CLMBOH11
		self.addLink( O17, O31, port1=102, port2=105, annotations={ "length": 172, "durable": "true" }, cls=LINCLink )  # CNCNOHWS  LSVLKYCS
		self.addLink( O17, O72, port1=103, port2=106, annotations={ "length": 779, "durable": "true" }, cls=LINCLink )  # CNCNOHWS  WASHDCSW
		self.addLink( O14, O15, port1=101, port2=105, annotations={ "length": 243, "durable": "true" }, cls=LINCLink )  # CLEVOH02  CLMBOH11
		self.addLink( O14, O70, port1=102, port2=101, annotations={ "length": 185, "durable": "true" }, cls=LINCLink )  # CLEVOH02  TOLDOH21
		self.addLink( O15, O48, port1=101, port2=107, annotations={ "length": 312, "durable": "true" }, cls=LINCLink )  # CLMBOH11  PITBPADG
		self.addLink( O18, O25, port1=101, port2=107, annotations={ "length": 437, "durable": "true" }, cls=LINCLink )  # DLLSTXTL  HSTNTX01
		self.addLink( O18, O32, port1=102, port2=105, annotations={ "length": 563, "durable": "true" }, cls=LINCLink )  # DLLSTXTL  LTRKARFR
		self.addLink( O18, O42, port1=103, port2=106, annotations={ "length": 365, "durable": "true" }, cls=LINCLink )  # DLLSTXTL  OKCYOKCE
		self.addLink( O19, O44, port1=101, port2=105, annotations={ "length": 940, "durable": "true" }, cls=LINCLink )  # DNVRCOMA  OMAHNENW
		self.addLink( O19, O56, port1=102, port2=105, annotations={ "length": 714, "durable": "true" }, cls=LINCLink )  # DNVRCOMA  SLKCUTMA
		self.addLink( O20, O70, port1=101, port2=106, annotations={ "length": 103, "durable": "true" }, cls=LINCLink )  # DTRTMIBA  TOLDOH21
		self.addLink( O21, O57, port1=101, port2=106, annotations={ "length": 969, "durable": "true" }, cls=LINCLink )  # ELPSTXMA  SNANTXCA
		self.addLink( O21, O68, port1=102, port2=107, annotations={ "length": 511, "durable": "true" }, cls=LINCLink )  # ELPSTXMA  TCSNAZMA
		self.addLink( O22, O31, port1=101, port2=106, annotations={ "length": 505, "durable": "true" }, cls=LINCLink )  # FRSNCA01  LSVGNV02
		self.addLink( O22, O29, port1=102, port2=106, annotations={ "length": 396, "durable": "true" }, cls=LINCLink )  # FRSNCA01  LSANCA03
		self.addLink( O22, O42, port1=103, port2=108, annotations={ "length": 298, "durable": "true" }, cls=LINCLink )  # FRSNCA01  OKLDCA03
		self.addLink( O23, O31, port1=101, port2=108, annotations={ "length": 697, "durable": "true" }, cls=LINCLink )  # GNBONCEU  LSVLKYCS
		self.addLink( O23, O52, port1=102, port2=107, annotations={ "length": 130, "durable": "true" }, cls=LINCLink )  # GNBONCEU  RLGHNCMO
		self.addLink( O23, O51, port1=103, port2=106, annotations={ "length": 319, "durable": "true" }, cls=LINCLink )  # GNBONCEU  RCMDVAGR
		self.addLink( O24, O28, port1=101, port2=108, annotations={ "length": 185, "durable": "true" }, cls=LINCLink )  # HRFRCT03  L_Island
		self.addLink( O24, O49, port1=102, port2=106, annotations={ "length": 125, "durable": "true" }, cls=LINCLink )  # HRFRCT03  PRVDRIGR
		self.addLink( O26, O45, port1=101, port2=105, annotations={ "length": 238, "durable": "true" }, cls=LINCLink )  # JCVLFLCL  ORLDFLMA
		self.addLink( O27, O44, port1=101, port2=106, annotations={ "length": 320, "durable": "true" }, cls=LINCLink )  # KSCYMO09  OMAHNENW
		self.addLink( O27, O64, port1=102, port2=105, annotations={ "length": 457, "durable": "true" }, cls=LINCLink )  # KSCYMO09  STLSMO09
		self.addLink( O27, O71, port1=103, port2=106, annotations={ "length": 420, "durable": "true" }, cls=LINCLink )  # KSCYMO09  TULSOKTB
		self.addLink( O30, O47, port1=101, port2=105, annotations={ "length": 494, "durable": "true" }, cls=LINCLink )  # LSVGNV02  PHNXAZMA
		self.addLink( O30, O56, port1=102, port2=106, annotations={ "length": 701, "durable": "true" }, cls=LINCLink )  # LSVGNV02  SLKCUTMA
		self.addLink( O32, O35, port1=101, port2=105, annotations={ "length": 249, "durable": "true" }, cls=LINCLink )  # LTRKARFR  MMPHTNMA
		self.addLink( O28, O41, port1=101, port2=106, annotations={ "length": 40, "durable": "true" }, cls=LINCLink )  # L_Island  NYCMNY54
		self.addLink( O29, O59, port1=101, port2=105, annotations={ "length": 212, "durable": "true" }, cls=LINCLink )  # LSANCA03  SNDGCA02
		self.addLink( O29, O58, port1=102, port2=105, annotations={ "length": 167, "durable": "true" }, cls=LINCLink )  # LSANCA03  SNBBCA01
		self.addLink( O31, O38, port1=104, port2=107, annotations={ "length": 298, "durable": "true" }, cls=LINCLink )  # LSVLKYCS  NSVLTNMT
		self.addLink( O31, O64, port1=102, port2=108, annotations={ "length": 468, "durable": "true" }, cls=LINCLink )  # LSVLKYCS  STLSMO09
		self.addLink( O35, O38, port1=101, port2=105, annotations={ "length": 380, "durable": "true" }, cls=LINCLink )  # MMPHTNMA  NSVLTNMT
		self.addLink( O33, O67, port1=101, port2=105, annotations={ "length": 407, "durable": "true" }, cls=LINCLink )  # MIAMFLAC  TAMQFLFN
		self.addLink( O33, O74, port1=102, port2=105, annotations={ "length": 125, "durable": "true" }, cls=LINCLink )  # MIAMFLAC  WPBHFLAN
		self.addLink( O34, O36, port1=101, port2=105, annotations={ "length": 574, "durable": "true" }, cls=LINCLink )  # MILWWIHE  MPLSMNDT
		self.addLink( O36, O44, port1=101, port2=107, annotations={ "length": 560, "durable": "true" }, cls=LINCLink )  # MPLSMNDT  OMAHNENW
		self.addLink( O39, O69, port1=101, port2=105, annotations={ "length": 670, "durable": "true" }, cls=LINCLink )  # NWORLAMA  TLHSFLAT
		self.addLink( O41, O40, port1=101, port2=106, annotations={ "length": 19, "durable": "true" }, cls=LINCLink )  # NYCMNY54  NWRKNJ02
		self.addLink( O41, O55, port1=102, port2=105, annotations={ "length": 187, "durable": "true" }, cls=LINCLink )  # NYCMNY54  SCTNPA01
		self.addLink( O41, O73, port1=103, port2=106, annotations={ "length": 210, "durable": "true" }, cls=LINCLink )  # NYCMNY54  WLMGDE01
		self.addLink( O40, O46, port1=101, port2=105, annotations={ "length": 146, "durable": "true" }, cls=LINCLink )  # NWRKNJ02  PHLAPASL
		self.addLink( O37, O52, port1=101, port2=106, annotations={ "length": 290, "durable": "true" }, cls=LINCLink )  # NRFLVABS  RLGHNCMO
		self.addLink( O37, O73, port1=102, port2=107, annotations={ "length": 392, "durable": "true" }, cls=LINCLink )  # NRFLVABS  WLMGDE01
		self.addLink( O43, O54, port1=101, port2=107, annotations={ "length": 131, "durable": "true" }, cls=LINCLink )  # OKLDCA03  SCRMCA01
		self.addLink( O43, O56, port1=102, port2=107, annotations={ "length": 1142, "durable": "true" }, cls=LINCLink )  # OKLDCA03  SLKCUTMA
		self.addLink( O43, O60, port1=103, port2=105, annotations={ "length": 13, "durable": "true" }, cls=LINCLink )  # OKLDCA03  SNFCCA21
		self.addLink( O42, O71, port1=101, port2=107, annotations={ "length": 188, "durable": "true" }, cls=LINCLink )  # OKCYOKCE  TULSOKTB
		self.addLink( O45, O74, port1=101, port2=106, annotations={ "length": 290, "durable": "true" }, cls=LINCLink )  # ORLDFLMA  WPBHFLAN
		self.addLink( O46, O55, port1=101, port2=106, annotations={ "length": 201, "durable": "true" }, cls=LINCLink )  # PHLAPASL  SCTNPA01
		self.addLink( O47, O59, port1=101, port2=106, annotations={ "length": 575, "durable": "true" }, cls=LINCLink )  # PHNXAZMA  SNDGCA02
		self.addLink( O47, O68, port1=102, port2=106, annotations={ "length": 205, "durable": "true" }, cls=LINCLink )  # PHNXAZMA  TCSNAZMA
		self.addLink( O48, O55, port1=101, port2=107, annotations={ "length": 457, "durable": "true" }, cls=LINCLink )  # PITBPADG  SCTNPA01
		self.addLink( O50, O54, port1=101, port2=106, annotations={ "length": 934, "durable": "true" }, cls=LINCLink )  # PTLDOR62  SCRMCA01
		self.addLink( O50, O56, port1=102, port2=108, annotations={ "length": 1225, "durable": "true" }, cls=LINCLink )  # PTLDOR62  SLKCUTMA
		self.addLink( O50, O65, port1=103, port2=106, annotations={ "length": 280, "durable": "true" }, cls=LINCLink )  # PTLDOR62  STTLWA06
		self.addLink( O51, O72, port1=101, port2=105, annotations={ "length": 184, "durable": "true" }, cls=LINCLink )  # RCMDVAGR  WASHDCSW
		self.addLink( O53, O66, port1=101, port2=107, annotations={ "length": 143, "durable": "true" }, cls=LINCLink )  # ROCHNYXA  SYRCNYSU
		self.addLink( O60, O61, port1=101, port2=105, annotations={ "length": 80., "durable": "true" }, cls=LINCLink )  # SNFCCA21  SNJSCA02
		self.addLink( O61, O58, port1=101, port2=106, annotations={ "length": 455, "durable": "true" }, cls=LINCLink )  # SNJSCA02  SNBBCA01
		self.addLink( O55, O66, port1=101, port2=105, annotations={ "length": 223, "durable": "true" }, cls=LINCLink )  # SCTNPA01  SYRCNYSU
		self.addLink( O65, O63, port1=101, port2=107, annotations={ "length": 442, "durable": "true" }, cls=LINCLink )  # STTLWA06  SPKNWA01
		self.addLink( O62, O64, port1=101, port2=106, annotations={ "length": 165, "durable": "true" }, cls=LINCLink )  # SPFDILSD  STLSMO09
		self.addLink( O69, O67, port1=101, port2=106, annotations={ "length": 384, "durable": "true" }, cls=LINCLink )  # TLHSFLAT  TAMQFLFN
		
		# Packet/Optical cross connect links (this will be the tap interfaces)
		self.addLink( WASHDCSWR, O72, port1=2, port2=10, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( WASHDCSWR, O72, port1=3, port2=11, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( WASHDCSWR, O72, port1=4, port2=12, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( WASHDCSWR, O72, port1=5, port2=13, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( WASHDCSWR, O72, port1=6, port2=14, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( SNJSCA02R, O61, port1=2, port2=10, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( SNJSCA02R, O61, port1=3, port2=11, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( SNJSCA02R, O61, port1=4, port2=12, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( SNJSCA02R, O61, port1=5, port2=13, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( SNJSCA02R, O61, port1=6, port2=14, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( SNANTXCAR, O57, port1=2, port2=10, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( SNANTXCAR, O57, port1=3, port2=11, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( SNANTXCAR, O57, port1=4, port2=12, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( SNANTXCAR, O57, port1=5, port2=13, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( SNANTXCAR, O57, port1=6, port2=14, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( ROCHNYXAR, O53, port1=2, port2=10, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( ROCHNYXAR, O53, port1=3, port2=11, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( ROCHNYXAR, O53, port1=4, port2=12, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( ROCHNYXAR, O53, port1=5, port2=13, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( ROCHNYXAR, O53, port1=6, port2=14, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( PHNXAZMAR, O47, port1=2, port2=10, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( PHNXAZMAR, O47, port1=3, port2=11, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( PHNXAZMAR, O47, port1=4, port2=12, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( PHNXAZMAR, O47, port1=5, port2=13, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( PHNXAZMAR, O47, port1=6, port2=14, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( ORLDFLMAR, O45, port1=2, port2=10, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( ORLDFLMAR, O45, port1=3, port2=11, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( ORLDFLMAR, O45, port1=4, port2=12, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( ORLDFLMAR, O45, port1=5, port2=13, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( ORLDFLMAR, O45, port1=6, port2=14, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( NWRKNJ02R, O40, port1=2, port2=10, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( NWRKNJ02R, O40, port1=3, port2=11, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( NWRKNJ02R, O40, port1=4, port2=12, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( NWRKNJ02R, O40, port1=5, port2=13, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( NWRKNJ02R, O40, port1=6, port2=14, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( MPLSMNDTR, O36, port1=2, port2=10, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( MPLSMNDTR, O36, port1=3, port2=11, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( MPLSMNDTR, O36, port1=4, port2=12, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( MPLSMNDTR, O36, port1=5, port2=13, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( MPLSMNDTR, O36, port1=6, port2=14, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( LSANCA03R, O29, port1=2, port2=10, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( LSANCA03R, O29, port1=3, port2=11, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( LSANCA03R, O29, port1=4, port2=12, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( LSANCA03R, O29, port1=5, port2=13, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( LSANCA03R, O29, port1=6, port2=14, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( DLLSTXTLR, O18, port1=2, port2=10, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( DLLSTXTLR, O18, port1=3, port2=11, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( DLLSTXTLR, O18, port1=4, port2=12, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( DLLSTXTLR, O18, port1=5, port2=13, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( DLLSTXTLR, O18, port1=6, port2=14, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( ATLNGATLR, O4, port1=2, port2=10, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( ATLNGATLR, O4, port1=3, port2=11, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( ATLNGATLR, O4, port1=4, port2=12, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( ATLNGATLR, O4, port1=5, port2=13, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		self.addLink( ATLNGATLR, O4, port1=6, port2=14, speed1=10000, annotations={ "bandwidth": 100000, "durable": "true" }, cls=LINCLink )
		

		# Attach hosts to the packet layer switches 
		for i in range( 1, 4 ): #don't make this more than 4!!!!!
			# Create Hosts 1..11
			h1 = self.addHost( 'h1d%d' % i, ip='10.0.1.%d/16' % i )
			h2 = self.addHost( 'h2d%d' % i, ip='10.0.2.%d/16' % i )
			h3 = self.addHost( 'h3d%d' % i, ip='10.0.3.%d/16' % i )
			h4 = self.addHost( 'h4d%d' % i, ip='10.0.4.%d/16' % i )
			h5 = self.addHost( 'h5d%d' % i, ip='10.0.5.%d/16' % i )
			h6 = self.addHost( 'h6d%d' % i, ip='10.0.6.%d/16' % i )
			h7 = self.addHost( 'h7d%d' % i, ip='10.0.7.%d/16' % i )
			h8 = self.addHost( 'h8d%d' % i, ip='10.0.8.%d/16' % i )
			h9 = self.addHost( 'h9d%d' % i, ip='10.0.9.%d/16' % i )
			h10 = self.addHost( 'h10d%d' % i, ip='10.0.10.%d/16' % i )
			h11 = self.addHost( 'h11d%d' % i, ip='10.0.11.%d/16' % i )

			port = i + 6
			self.addLink( SNJSCA02R, h1, port1=port )
			self.addLink( SNANTXCAR, h2, port1=port )
			self.addLink( ROCHNYXAR, h3, port1=port )
			self.addLink( PHNXAZMAR, h4, port1=port )
			self.addLink( ORLDFLMAR, h5, port1=port )
			self.addLink( NWRKNJ02R, h6, port1=port )
			self.addLink( MPLSMNDTR, h7, port1=port )
			self.addLink( LSANCA03R, h8, port1=port )
			self.addLink( DLLSTXTLR, h9, port1=port )
			self.addLink( ATLNGATLR, h10, port1=port )
			self.addLink( WASHDCSWR, h11, port1=port )

if __name__ == '__main__':
    import sys
    if len( sys.argv ) >= 2:
        controllers = sys.argv[1:]
    else:
        print 'Usage: sudo -E python opticalTestBig.py (<Controller IP>)+'
        print 'Using localhost...\n'
        controllers = [ '127.0.0.1' ]

    setLogLevel( 'info' )
    net = MininetOE( topo=BigOpticalTopo(), controller=None, autoSetMacs=True )
    net.addControllers( controllers )
    net.start()
    CLI( net )
    net.stop()
