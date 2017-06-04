#!/bin/bash
# -----------------------------------------------------------------------------
# Creates a Europe-based topology (with regions) using ONOS null provider
# -----------------------------------------------------------------------------

# config
host=${1:-localhost}
nports=24
sleepfor=5


### start up null provider
onos ${host} null-simulation stop custom
onos ${host} wipe-out please
onos ${host} null-simulation start custom


## unfortunately, it takes a time for the sim to start up
#  this is not ideal... but we'll live with it for now

echo
echo "Sleeping while sim starts up... (${sleepfor} seconds)..."
echo
sleep ${sleepfor}


###------------------------------------------------------
### Start by adding Country regions
# Note that Long/Lat places region icon nicely in the country center

# region-add <region-id> <region-name> <region-type> \
#   <lat/Y> <long/X> <locType> <region-master>

onos ${host} <<-EOF

region-add rUK "United Kingdom" COUNTRY 52.206035 -1.310384 geo ${host}
region-add rIT "Italy"   COUNTRY 44.447951  11.093161 geo ${host}
region-add rFR "France"  COUNTRY 47.066264  2.711458 geo ${host}
region-add rDE "Germany" COUNTRY 50.863152  9.761971 geo ${host}
region-add rES "Spain"   COUNTRY 40.416704 -3.7035824 geo ${host}

region-add rMilan "Milan" METRO 45.4654 9.1859 geo ${host}

EOF


###------------------------------------------------------
### Add layouts, associating backing regions, and optional parent.
# layout-add <layout-id> <bg-ref> \
#   [ <region-id> <parent-layout-id> <scale> <offset-x> <offset-y> ]
#

onos ${host} <<-EOF

# -- root layout
layout-add root @europe . . 4.66 -2562.93 -412.56

# -- layouts for top level regions
layout-add lUK @europe rUK root 14.82 -8533.83 -1377.96
layout-add lIT @europe rIT root 8.93 -6055.26 -1626.86
layout-add lFR @europe rFR root 7.94 -4694.70 -1092.92
layout-add lDE @europe rDE root 8.20 -5367.29 -727.28
layout-add lES @europe rES root 9.93 -5558.99 -2243.35

# -- layouts for country sub-regions
layout-add lMilan +segmentRoutingTwo rMilan lIT 0.86 68.58 54.71

# -- summary of installed layouts
layouts
EOF

###------------------------------------------------------
### Add devices, hosts and links for each of the regions

onos ${host} <<-EOF

# -- UK devices

null-create-device switch London     ${nports} 51.5073 -0.1276
null-create-device switch Reading    ${nports} 51.4543 -0.9781
null-create-device switch Portsmouth ${nports} 50.8198 -1.0880
null-create-device switch Bristol    ${nports} 51.4545 -2.5879
null-create-device switch Warrington ${nports} 53.3900 -2.5970
null-create-device switch Leeds      ${nports} 53.8008 -1.5491

# -- Assign UK devices to UK region

region-add-devices rUK \
    null:0000000000000001 \
    null:0000000000000002 \
    null:0000000000000003 \
    null:0000000000000004 \
    null:0000000000000005 \
    null:0000000000000006 \

# -- UK hosts

null-create-host London  192.168.1.1   51.8697 -0.0287
null-create-host London  192.168.1.2   51.7225  0.7624
null-create-host London  192.168.1.3   51.1437  0.4694

null-create-host Bristol 192.168.1.4   51.7500  -2.6000

# -- UK connectivity

null-create-link direct London Reading
null-create-link direct London Portsmouth
null-create-link direct Reading Bristol
null-create-link direct Portsmouth Bristol
null-create-link direct Reading Warrington
null-create-link direct London Leeds
null-create-link direct Leeds Warrington

# -- UK Peers
region-add-peer-loc rUK rES 50.4060  -3.3860
region-add-peer-loc rUK rFR 50.4060  -1.8482
region-add-peer-loc rUK rIT 50.4060  -0.1361
region-add-peer-loc rUK rDE 50.4060   1.2491


# -- France Devices

null-create-device switch Paris      ${nports} 48.8566  2.3522
null-create-device switch Lyon       ${nports} 45.7640  4.8357
null-create-device switch Bordeaux   ${nports} 44.8378 -0.5792
null-create-device switch Marseille  ${nports} 43.2965  5.3698
null-create-device switch Nice       ${nports} 43.7102  7.2620

# -- Assign France devices to France region

region-add-devices rFR \
    null:0000000000000007 \
    null:0000000000000008 \
    null:0000000000000009 \
    null:000000000000000a \
    null:000000000000000b \


# -- France hosts

null-create-host Paris  192.168.2.1   49.5134 2.8882
null-create-host Lyon   192.168.2.2   46.4590 5.2380

# -- France connectivity

null-create-link direct Paris Lyon
null-create-link direct Paris Bordeaux
null-create-link direct Lyon Bordeaux
null-create-link direct Marseille Bordeaux
null-create-link direct Marseille Lyon
null-create-link direct Marseille Nice
null-create-link direct Lyon Nice

# -- France Peers

region-add-peer-loc rFR rES  42.6806  -2.1273
region-add-peer-loc rFR rUK  50.6164  -2.1013
region-add-peer-loc rFR rIT  45.1105   9.7450
region-add-peer-loc rFR rDE  49.6307   7.9326


# -- Italy Devices

# these four in a mini fabric (data center?)

null-create-device switch Milan-1      ${nports} 10.0 20.0 grid
null-create-device switch Milan-2      ${nports} 10.0 50.0 grid
null-create-device switch Milan-3      ${nports} 45.0 20.0 grid
null-create-device switch Milan-4      ${nports} 45.0 50.0 grid

null-create-host Milan-3 192.168.3.13 60.0 15.0 grid
null-create-host Milan-4 192.168.3.14 60.0 45.0 grid

region-add-devices rMilan \
    null:000000000000000c \
    null:000000000000000d \
    null:000000000000000e \
    null:000000000000000f \

null-create-device switch Venice  ${nports} 45.4408  12.3155
null-create-device switch Rome    ${nports} 41.9028  12.4964
null-create-device switch Naples  ${nports} 40.8518  14.2681

region-add-devices rIT \
    null:0000000000000010 \
    null:0000000000000011 \
    null:0000000000000012 \

# -- Italy Connectivity

null-create-link direct Milan-1 Milan-3
null-create-link direct Milan-1 Milan-4
null-create-link direct Milan-2 Milan-3
null-create-link direct Milan-2 Milan-4

null-create-link direct Milan-1 Venice
null-create-link direct Milan-2 Rome

null-create-link direct Venice Rome
null-create-link direct Venice Naples
null-create-link direct Rome Naples

# -- Italy Peers

region-add-peer-loc rIT rES  41.8942 2.7590
region-add-peer-loc rIT rUK  47.0678 6.3919
region-add-peer-loc rIT rFR  43.9538 5.1235
region-add-peer-loc rIT rDE  47.3839 10.9857

# -- Germany Devices

null-create-device switch Munich     ${nports} 48.1351 11.5820
null-create-device switch Berlin     ${nports} 52.5200 13.4050
null-create-device switch Bremen     ${nports} 53.0793  8.8017
null-create-device switch Frankfurt  ${nports} 50.1109  8.6821
null-create-device switch Stuttgart  ${nports} 48.7758  9.1829

null-create-host Munich 192.168.4.1  47.4818 11.7441
null-create-host Berlin 192.168.4.2  53.0537 13.5310

region-add-devices rDE \
    null:0000000000000013 \
    null:0000000000000014 \
    null:0000000000000015 \
    null:0000000000000016 \
    null:0000000000000017 \

# -- Germany Connectivity

null-create-link direct Munich Berlin
null-create-link direct Munich Stuttgart
null-create-link direct Munich Stuttgart
null-create-link direct Frankfurt Stuttgart
null-create-link direct Frankfurt Bremen
null-create-link direct Berlin Bremen
null-create-link direct Berlin Frankfurt

# -- Germany Peers

region-add-peer-loc rDE rES  46.9845 2.1152
region-add-peer-loc rDE rUK  51.6325 -0.1912
region-add-peer-loc rDE rFR  48.5239 4.9598
region-add-peer-loc rDE rIT  46.9118 11.1705

# -- Spain Devices

null-create-device switch Madrid     ${nports} 40.4168 -3.7038
null-create-device switch Barcelona  ${nports} 41.3851  2.1734
null-create-device switch Valencia   ${nports} 39.4699 -0.3763
null-create-device switch Seville    ${nports} 37.3891 -5.9845

null-create-host Madrid    192.168.5.1  41.0797 -3.9559
null-create-host Barcelona 192.168.5.2  41.8507  2.0399
null-create-host Valencia  192.168.5.3  38.8488 -0.5097
null-create-host Seville   192.168.5.4  38.0110 -6.4442

region-add-devices rES \
    null:0000000000000018 \
    null:0000000000000019 \
    null:000000000000001a \
    null:000000000000001b \

# -- Spain Connectivity

null-create-link direct Barcelona Madrid
null-create-link direct Barcelona Valencia
null-create-link direct Seville Madrid
null-create-link direct Seville Valencia
null-create-link direct Madrid Valencia

# -- Spain Peers

region-add-peer-loc rES rDE  43.0897 2.4361
region-add-peer-loc rES rUK  44.0273 -6.4832
region-add-peer-loc rES rFR  43.5896 -0.7197
region-add-peer-loc rES rIT  39.8093 4.8966


# -- Inter-Region Connectivity

# Spain-France
null-create-link direct Barcelona Marseille
null-create-link direct Madrid Bordeaux

# France-Italy
null-create-link direct Rome Nice
null-create-link direct Milan-1 Lyon

# Italy-Germany
null-create-link direct Venice Munich

# France-Germany
null-create-link direct Lyon Stuttgart
null-create-link direct Paris Frankfurt

# England-France
null-create-link direct Portsmouth Paris

# England-Germany
null-create-link direct London Bremen
null-create-link direct London Frankfurt

EOF

### Set up debug log messages for classes we care about
onos ${host} <<-EOF
log:set DEBUG org.onosproject.ui.impl.topo.Topo2ViewMessageHandler
log:set DEBUG org.onosproject.ui.impl.topo.Topo2Jsonifier
log:set DEBUG org.onosproject.ui.impl.UiWebSocket
log:set DEBUG org.onosproject.ui.impl.UiTopoSession
log:list
EOF
