#! /usr/bin/env python

import requests
import sys
import urllib

from requests.auth import HTTPBasicAuth

if len(sys.argv) != 4:
    print "usage: query-cluster onos-node name cluster-number"
    sys.exit(1)

node = sys.argv[1]
name = sys.argv[2]
cluster = sys.argv[3]

topoRequest = requests.get('http://' + node + ':8181/onos/v1/topology/clusters/'
                           + cluster,
                           auth=HTTPBasicAuth('onos', 'rocks'))

if topoRequest.status_code != 200:
    print topoRequest.text
    sys.exit(1)

topoJson = topoRequest.json()

print "@stc " + name + "Id=" + str(topoJson["id"])
print "@stc " + name + "DeviceCount=" + str(topoJson["deviceCount"])
print "@stc " + name + "LinkCount=" + str(topoJson["linkCount"])
print "@stc " + name + "Root=" + topoJson["root"]

sys.exit(0)





