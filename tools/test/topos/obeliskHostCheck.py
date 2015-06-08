#!/usr/bin/python

import sys
import os
import json

# TODO: if none given, use OCI
try:
    onosIp = sys.argv[1]
    print "Reading hosts view from ONOS node " + onosIp + ":"
except Exception as e:
    print "Error reading ONOS IP arguement"
    print e
# Grab the json objects from ONOS
output = os.popen("onos " + onosIp + " \"hosts -j\"" )
hosts = json.loads( output.read() )
#hosts = json.loads( output.split( 'Logging in as karaf\n' )[1] )

hostAttachment = True
# FIXME: topo-HA/obelisk specific mappings:
# key is mac and value is dpid
mappings = {}
for i in range( 1, 29 ):  # hosts 1 through 28
    # set up correct variables:
    macId = "00:" * 5 + hex( i ).split( "0x" )[1].upper().zfill(2)
    if i == 1:
        deviceId = "1000".zfill(16)
    elif i == 2:
        deviceId = "2000".zfill(16)
    elif i == 3:
        deviceId = "3000".zfill(16)
    elif i == 4:
        deviceId = "3004".zfill(16)
    elif i == 5:
        deviceId = "5000".zfill(16)
    elif i == 6:
        deviceId = "6000".zfill(16)
    elif i == 7:
        deviceId = "6007".zfill(16)
    elif i >= 8 and i <= 17:
        dpid = '3' + str( i ).zfill( 3 )
        deviceId = dpid.zfill(16)
    elif i >= 18 and i <= 27:
        dpid = '6' + str( i ).zfill( 3 )
        deviceId = dpid.zfill(16)
    elif i == 28:
        deviceId = "2800".zfill(16)
    mappings[ macId ] = deviceId

if hosts or "Error" not in hosts:
    if hosts == []:
        print "WARNING: There are no hosts discovered"
    else:
        for host in hosts:
            mac = None
            location = None
            device = None
            port = None
            try:
                mac = host.get( 'mac' )
                assert mac, "mac field could not be found for this host object"

                location = host.get( 'location' )
                assert location, "location field could not be found for this host object"

                # Trim the protocol identifier off deviceId
                device = str( location.get( 'elementId' ) ).split(':')[1]
                assert device, "elementId field could not be found for this host location object"

                port = location.get( 'port' )
                assert port, "port field could not be found for this host location object"

                # Now check if this matches where they should be
                if mac and device and port:
                    if device != mappings[ str( mac ) ]:
                        print "The attachment device is incorrect for host " + str( mac ) +\
                              ". Expected: " + mappings[ str( mac ) ] + "; Actual: " + device
                        hostAttachment = False
                    if str( port ) != "1":
                        print "The attachment port is incorrect for host " + str( mac ) +\
                              ". Expected: 1; Actual: " + str( port)
                        hostAttachment = False
                else:
                    hostAttachment = False
            except AssertionError as e:
                print "ERROR: Json object not as expected:"
                print e
                print "host object: " + repr( host )
                hostAttachment = False
else:
    print "No hosts json output or \"Error\" in output. hosts = " + repr( hosts )
