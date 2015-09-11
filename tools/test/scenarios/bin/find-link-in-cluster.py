#! /usr/bin/env python

import requests
import sys

from requests.auth import HTTPBasicAuth

if len(sys.argv) != 9:
    print "usage: find-link-in-cluster onos-node name cluster-id expected-length src-device-id src-port dst-device-id dst-port"
    sys.exit(1)

node = sys.argv[1]
name = sys.argv[2]
cluster = sys.argv[3]
length = int(sys.argv[4])
srcDeviceId = sys.argv[5]
srcPort = sys.argv[6]
dstDeviceId = sys.argv[7]
dstPort = sys.argv[8]


linksRequest = requests.get('http://' + node + ':8181/onos/v1/topology/clusters/'
                            + cluster + '/links',
                            auth=HTTPBasicAuth('onos', 'rocks'))

if linksRequest.status_code != 200:
    print linksRequest.text
    sys.exit(1)

linksJson = linksRequest.json()
linksLength = len(linksJson["links"])

if  linksLength != length:
    print "Expected length {} but got {}".format(length, linksLength)
    sys.exit(1)

for link in linksJson["links"]:
    if srcDeviceId == link["src"]["device"] and srcPort == link["src"]["port"]:
        if dstDeviceId == link["dst"]["device"] and dstPort == link["dst"]["port"]:
            print "@stc " + name + "SrcDevice=" + link["src"]["device"]
            print "@stc " + name + "SrcPort=" + link["src"]["port"]
            print "@stc " + name + "DstDevice=" + link["dst"]["device"]
            print "@stc " + name + "DstPort=" + link["dst"]["port"]
            print "@stc " + name + "Type=" + link["type"]
            print "@stc " + name + "State=" + link["state"]
            sys.exit(0)

print "Could not find link from {}:{} to {}:{}"\
    .format(srcDeviceId, srcPort, dstDeviceId, dstPort)
sys.exit(1)




