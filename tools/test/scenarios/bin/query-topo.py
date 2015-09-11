#! /usr/bin/env python

import requests
import sys
import urllib

from requests.auth import HTTPBasicAuth

if len(sys.argv) != 3:
    print "usage: query-topo onos-node name"
    sys.exit(1)

node = sys.argv[1]
name = sys.argv[2]

topoRequest = requests.get('http://' + node + ':8181/onos/v1/topology/',
                           auth=HTTPBasicAuth('onos', 'rocks'))

if topoRequest.status_code != 200:
    print topoRequest.text
    sys.exit(1)

topoJson = topoRequest.json()

print "@stc " + name + "Time=" + str(topoJson["time"])
print "@stc " + name + "Devices=" + str(topoJson["devices"])
print "@stc " + name + "Links=" + str(topoJson["links"])
print "@stc " + name + "Clusters=" + str(topoJson["clusters"])

sys.exit(0)





