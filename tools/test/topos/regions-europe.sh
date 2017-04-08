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
#  this is not ideal...

echo
echo "Sleeping while sim starts up... (${sleepfor} seconds)..."
echo
sleep ${sleepfor}


###------------------------------------------------------
### Start by adding Country regions
# Note that Long/Lat places region icon nicely in the country center

# region-add <region-id> <region-name> <region-type> <lat/Y> <long/X> <region-master>

onos ${host} <<-EOF

region-add rUK "United Kingdom" COUNTRY 52.206035 -1.310384 ${host}
region-add rIT "Italy"   COUNTRY 44.447951  11.093161 ${host}
region-add rFR "France"  COUNTRY 47.066264  2.711458 ${host}
region-add rDE "Germany" COUNTRY 50.863152  9.761971 ${host}
region-add rES "Spain"   COUNTRY 40.416704 -3.7035824 ${host}

EOF


###------------------------------------------------------
### Add layouts, associating backing regions, and optional parent.
# layout-add <layout-id> <bg-ref> \
#   [ <region-id> <parent-layout-id> <scale> <offset-x> <offset-y> ]
#

onos ${host} <<-EOF

# -- root layout
layout-add root @europe . . 6.664 -2992.552 -2473.084

# -- layouts for top level regions
layout-add lUK @europe rUK root 31.43 -14979.6 -12644.8
layout-add lIT @europe rIT root 14.72 -7793.2 -6623.8
layout-add lFR @europe rFR root 16.91 -8225.7 -7198.1
layout-add lDE @europe rDE root 17.63 -9119.6 -7044.9
layout-add lES @europe rES root 21.41 -9994.5 -10135.6

# -- layouts for country sub-regions
# TODO

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

# rUK_rES 50.4060  -3.3860
# rUK_rFR 50.4060  -1.8482
# rUK_rIT 50.4060  -0.1361
# rUK_rDE 50.4060   1.2491


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

# rFR_rES  42.6806  -2.1273
# rFR_rUK  50.6164  -2.1013
# rFR_rIT  45.1105   9.7450
# rFR_rDE  49.6307   7.9326

EOF

### Set up debug log messages for classes we care about
onos ${host} <<-EOF
log:set DEBUG org.onosproject.ui.impl.topo.Topo2ViewMessageHandler
log:set DEBUG org.onosproject.ui.impl.topo.Topo2Jsonifier
log:set DEBUG org.onosproject.ui.impl.UiWebSocket
log:set DEBUG org.onosproject.ui.impl.UiTopoSession
log:list
EOF
