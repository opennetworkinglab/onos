#!/bin/bash
#
# A simple test topology of three regions, A, B, and C.
#
# Script Configuration:
#
# host     : the controller instance against which this script is run

host=${1:-127.0.0.1}


###------------------------------------------------------
### Start by adding the three regions A, B, and C

# region-add <region-id> <region-name> <region-type> \
#   <lat/Y> <long/X> <locType> <region-master>

onos ${host} <<-EOF

# -- define regions
region-add rA "Region A" LOGICAL_GROUP 30 20 grid ${host}
region-add rB "Region B" LOGICAL_GROUP 30 40 grid ${host}
region-add rC "Region C" LOGICAL_GROUP 30 60 grid ${host}

# -- set peer locations
region-add-peer-loc rA rB 40 70 grid
region-add-peer-loc rA rC 50 70 grid

region-add-peer-loc rB rA 30 10 grid
region-add-peer-loc rB rC 30 70 grid

region-add-peer-loc rC rA 10 10 grid
region-add-peer-loc rC rB 20 10 grid

EOF

###------------------------------------------------------
### Add layouts, associating backing regions, and optional parent

# layout-add <layout-id> <bg-ref> \
#   [ <region-id> <parent-layout-id> <scale> <offset-x> <offset-y> ]
#

onos ${host} <<-EOF

# -- top level
layout-add root +plain . . 1.0 0.0 0.0

# -- layouts for top level regions
layout-add lA +plain rA root 1.0 0.0 0.0
layout-add lB +plain rB root 1.0 0.0 0.0
layout-add lC +plain rC root 1.0 0.0 0.0

# -- summary
layouts
EOF

###------------------------------------------------------
### Assign devices to each of their regions

onos ${host} <<-EOF

region-add-devices rA \
    of:0000000000000001 \
    of:0000000000000002 \

region-add-devices rB \
    of:0000000000000003 \
    of:0000000000000004 \

region-add-devices rC \
    of:0000000000000005 \
    of:0000000000000006 \

EOF

###------------------------------------------------------
### Configure devices and hosts

onos-netcfg ${host} regionabc.json


