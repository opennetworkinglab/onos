#! /usr/bin/env python

import requests
import sys

from requests.auth import HTTPBasicAuth

if len(sys.argv) != 7:
    print "usage: find-link onos-node name src-device-id src-port dst-device-id dst-port"
    sys.exit(1)

node = sys.argv[1]
name = sys.argv[2]
srcDeviceId = sys.argv[3]
srcPort = sys.argv[4]
dstDeviceId = sys.argv[5]
dstPort = sys.argv[6]


linksRequest = requests.get('http://' + node + ':8181/onos/v1/links?device=' +
                            srcDeviceId + '&port=' + srcPort,
                            auth=HTTPBasicAuth('onos', 'rocks'))

if linksRequest.status_code != 200:
    print linksRequest.text
    sys.exit(1)

linksJson = linksRequest.json()

for link in linksJson["links"]:
    if srcDeviceId == link["src"]["device"]:
        if dstDeviceId == link["dst"]["device"]:
            print "@stc " + name + "SrcDevice=" + link["src"]["device"]
            print "@stc " + name + "SrcPort=" + link["src"]["port"]
            print "@stc " + name + "DstDevice=" + link["dst"]["device"]
            print "@stc " + name + "DstPort=" + link["dst"]["port"]
            print "@stc " + name + "Type=" + link["type"]
            print "@stc " + name + "State=" + link["state"]
            sys.exit(0)

sys.exit(1)




