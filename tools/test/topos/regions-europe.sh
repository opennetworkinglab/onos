#!/bin/bash
# -----------------------------------------------------------------------------
# Creates a replica of the GEANT topology using ONOS null provider
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


### Start by adding Country regions
# Note that Long/Lat places region icon nicely in the country center

# region-add <region-id> <region-name> <region-type> <long/Y> <lat/X> <region-master>

onos ${host} <<-EOF

region-add rUK "United Kingdom" COUNTRY 52.206035 -1.310384 ${host}
region-add rIT "Italy"   COUNTRY 44.447951  11.093161 ${host}
region-add rFR "France"  COUNTRY 47.066264  2.711458 ${host}
region-add rDE "Germany" COUNTRY 50.863152  9.761971 ${host}
region-add rES "Spain"   COUNTRY 40.416704 -3.7035824 ${host}

EOF


###------------------------------------------------------
###----- TEMPORARY DATA ---------------------------------

#region-add rLON "London"    COUNTRY 51.507321 -0.1276473 ${host}
#region-add rMIL "Milan"     COUNTRY 45.466797  9.1904984 ${host}
#region-add rPAR "Paris"     COUNTRY 48.856610  2.3514992 ${host}
#region-add rFRA "Frankfurt" COUNTRY 50.110652  8.6820934 ${host}
#region-add rMAD "Madrid"    COUNTRY 40.416704 -3.7035824 ${host}

# null-create-device switch LON ${nports} 51.507321 -0.1276473
# null-create-device switch PAR ${nports} 48.856610 2.3514992
# null-create-device switch MIL ${nports} 45.466797 9.1904984
# null-create-device switch FRA ${nports} 50.110652 8.6820934
# null-create-device switch MAD ${nports} 40.416704 -3.7035824

###------------------------------------------------------


### Add layouts, associating backing regions, and optional parent.
# layout-add <layout-id> <bg-ref> \
#   [ <region-id> <parent-layout-id> <scale> <offset-x> <offset-y> ]
#

onos ${host} <<-EOF

# -- root layout
layout-add root @europe . . 6.664 -2992.552 -2473.084

# -- layouts for top level regions
layout-add lUK @europe rUK root 13.99 -6233.775 -5111.723
layout-add lIT @europe rIT root 14.72 -7793.210 -6623.814
layout-add lFR @europe rFR root 16.91 -8225.716 -7198.134
layout-add lDE @europe rDE root 17.63 -9119.646 -7044.949
layout-add lES @europe rES root 21.41 -9994.596 -10135.655

# -- layouts for country sub-regions
# TODO

# -- summary of installed layouts
layouts

EOF


### Set up debug log messages for classes we care about
onos ${host} <<-EOF
log:set DEBUG org.onosproject.ui.impl.topo.Topo2ViewMessageHandler
log:set DEBUG org.onosproject.ui.impl.topo.Topo2Jsonifier
log:set DEBUG org.onosproject.ui.impl.UiWebSocket
log:set DEBUG org.onosproject.ui.impl.UiTopoSession
log:list
EOF
