#!/bin/bash
# -----------------------------------------------------------------------------
# Creates a Korean-based topology (with regions) using ONOS null provider
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

region-add rGG "Gyeonggi-do"    COUNTRY 37.4138000 127.5183000 geo ${host}
region-add rGW "Gangwon-do"     COUNTRY 37.8228000 128.1555000 geo ${host}
region-add rCC "Chungcheng-do"  COUNTRY 36.5622940 126.9541070 geo ${host}
region-add rJL "Jeolla-do"      COUNTRY 35.3564250 126.9541070 geo ${host}
region-add rGS "Gyeongsang-do"  COUNTRY 35.8059060 128.9876740 geo ${host}
region-add rJJ "Jeju-do"        COUNTRY 33.4890110 126.4983020 geo ${host}

EOF

###------------------------------------------------------
### Add layouts, associating backing regions, and optional parent.
# layout-add <layout-id> <bg-ref> \
#   [ <region-id> <parent-layout-id> <scale> <offset-x> <offset-y> ]
#

onos ${host} <<-EOF

# -- root layout
layout-add root @s_korea . . 1 0 -10

# -- layouts for top level regions
layout-add lGG @s_korea rGG root 5.480  -1582.5196  -770.0172
layout-add lGW @s_korea rGW root 2.606  -1050.3955  -30.8123
layout-add lCC @s_korea rCC root 3.083  -988.8076   -699.3067
layout-add lJL @s_korea rJL root 2.747  -705.7564   -1211.7502
layout-add lGS @s_korea rGS root 3.024  -1737.0638  -1231.4722
layout-add lJJ @s_korea rJJ root 2.885  -496.0931   -2347.7142

# -- summary of installed layouts
layouts
EOF

###------------------------------------------------------
### Add devices, hosts and links for each of the regions

onos ${host} <<-EOF

# -- GG devices

null-create-device switch Seoul         ${nports} 37.5665350 126.9779690
null-create-device switch Incheon       ${nports} 37.4562560 126.7052060
null-create-device switch Suwon         ${nports} 37.2635730 127.0286010
null-create-device switch Goyang        ${nports} 37.6583600 126.8320200
null-create-device switch Yongin        ${nports} 37.2410860 127.1775540
null-create-device switch Seongnam      ${nports} 37.4449170 127.1388680

# -- Assign GG devices to GG region

region-add-devices rGG \
    null:0000000000000001 \
    null:0000000000000002 \
    null:0000000000000003 \
    null:0000000000000004 \
    null:0000000000000005 \
    null:0000000000000006 \

# -- GG connectivity

null-create-link direct Seoul Suwon
null-create-link direct Seoul Seongnam
null-create-link direct Incheon Yongin
null-create-link direct Incheon Goyang
null-create-link direct Goyang Suwon
null-create-link direct Seongnam Suwon

# -- GG peers

region-add-peer-loc rGG rGW 37.7252   127.6672
region-add-peer-loc rGG rCC 37.0227   127.3566
region-add-peer-loc rGG rGS 37.1117   127.7196
region-add-peer-loc rGG rJL 36.9509   127.1277
region-add-peer-loc rGG rJJ 36.9232   126.8540


# -- GW devices

null-create-device switch Wonju             ${nports} 37.3422190 127.9201620
null-create-device switch Chuncheon         ${nports} 37.8813150 127.7299710
null-create-device switch Gangneung         ${nports} 37.7518530 128.8760570

# -- Assign GW devices to GW region

region-add-devices rGW \
    null:0000000000000007 \
    null:0000000000000008 \
    null:0000000000000009 \

# -- GW connectivity

null-create-link direct Wonju Chuncheon
null-create-link direct Wonju Gangneung
null-create-link direct Gangneung Chuncheon

# -- GW peers

region-add-peer-loc rGW rGG 37.8043   127.0020
region-add-peer-loc rGW rCC 36.8942   127.0999
region-add-peer-loc rGW rGS 36.9275   129.1693
region-add-peer-loc rGW rJL 36.8730   128.4439
region-add-peer-loc rGW rJJ 36.7671   127.7459


# -- CC devices

null-create-device switch Daejeon           ${nports} 36.3504120 127.3845480
null-create-device switch Cheongju          ${nports} 36.6424340 127.4890320
null-create-device switch Asan              ${nports} 36.7897960 127.0018490
null-create-device switch Chungju           ${nports} 36.9910110 127.9259500

# -- Assign CC devices to CC region

region-add-devices rCC \
    null:000000000000000a \
    null:000000000000000b \
    null:000000000000000c \
    null:000000000000000d \

# -- CC connectivity

null-create-link direct Daejeon Cheongju
null-create-link direct Daejeon Asan
null-create-link direct Daejeon Chungju
null-create-link direct Chungju Cheongju
null-create-link direct Asan Cheongju

# -- CC peers

region-add-peer-loc rCC rGW 37.2499   128.0815
region-add-peer-loc rCC rGG 37.0604   127.2840
region-add-peer-loc rCC rGS 36.9256   128.5413
region-add-peer-loc rCC rJL 36.0471   127.2376
region-add-peer-loc rCC rJJ 35.9449   127.0086


# -- GS devices

null-create-device switch Busan         ${nports} 35.1795540 129.0756420
null-create-device switch Daegu         ${nports} 35.8714350 128.6014450
null-create-device switch Ulsan         ${nports} 35.5383770 129.3113600
null-create-device switch Pohang        ${nports} 36.0190180 129.3434810

# -- Assign GS devices to GS region

region-add-devices rGS \
    null:000000000000000e \
    null:000000000000000f \
    null:0000000000000010 \
    null:0000000000000011 \

# -- GS connectivity

null-create-link direct Busan Daegu
null-create-link direct Busan Ulsan
null-create-link direct Busan Pohang
null-create-link direct Daegu Pohang
null-create-link direct Pohang Ulsan

# -- GS peers

region-add-peer-loc rGS rGG 36.1994   127.9788
region-add-peer-loc rGS rGW 36.1775   128.7119
region-add-peer-loc rGS rCC 35.9361   127.6306
region-add-peer-loc rGS rJL 35.0274   127.4704
region-add-peer-loc rGS rJJ 34.7682   128.0892


# -- JL devices

null-create-device switch Gwangju       ${nports} 35.1595450 126.8526010
null-create-device switch Jeonju        ${nports} 35.8242240 127.1479530
null-create-device switch Iksan         ${nports} 35.9482860 126.9575990
null-create-device switch Yeosu         ${nports} 34.7603740 127.6622220
null-create-device switch Suncheon      ${nports} 34.9506370 127.4872140

# -- Assign JL devices to JL region

region-add-devices rJL \
    null:0000000000000012 \
    null:0000000000000013 \
    null:0000000000000014 \
    null:0000000000000015 \
    null:0000000000000016 \

# -- JL connectivity

null-create-link direct Gwangju Jeonju
null-create-link direct Gwangju Iksan
null-create-link direct Gwangju Yeosu
null-create-link direct Gwangju Suncheon
null-create-link direct Yeosu Suncheon
null-create-link direct Jeonju Iksan
null-create-link direct Jeonju Yeosu

# -- JL peers

region-add-peer-loc rJL rCC 36.0736   126.7974
region-add-peer-loc rJL rGG 36.1204   127.3223
region-add-peer-loc rJL rGW 36.0254   127.8910
region-add-peer-loc rJL rGS 35.5403   127.8257
region-add-peer-loc rJL rJJ 34.5884   127.1328

# {"lng":9731256818,"lat":4647468107}}}

# -- JJ devices

null-create-device switch Jeju          ${nports} 33.4890110 126.4983020
null-create-device switch Seogwipo      ${nports} 33.2541210 126.5600760

# -- Assign JJ devices to JJ region

region-add-devices rJJ \
    null:0000000000000017 \
    null:0000000000000018 \

# -- JJ connectivity

null-create-link direct Jeju Seogwipo

# -- JJ peers

region-add-peer-loc rJJ rGW 34.0295   126.8138
region-add-peer-loc rJJ rGG 33.9136   126.6015
region-add-peer-loc rJJ rCC 33.7931   126.4294
region-add-peer-loc rJJ rGS 33.6874   127.0029
region-add-peer-loc rJJ rJL 33.6607   126.3719


### Set up debug log messages for classes we care about
onos ${host} <<-EOF
log:set DEBUG org.onosproject.ui.impl.topo.Topo2ViewMessageHandler
log:set DEBUG org.onosproject.ui.impl.topo.Topo2Jsonifier
log:set DEBUG org.onosproject.ui.impl.UiWebSocket
log:set DEBUG org.onosproject.ui.impl.UiTopoSession
log:list
EOF
