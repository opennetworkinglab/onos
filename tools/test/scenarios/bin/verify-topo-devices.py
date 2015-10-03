#! /usr/bin/env python

import requests
import sys
import urllib

from requests.auth import HTTPBasicAuth

if len(sys.argv) != 5:
    print "usage: verify-topo-links onos-node cluster-id first-index last-index"
    sys.exit(1)

node = sys.argv[1]
cluster = sys.argv[2]
first = int(sys.argv[3])
last = int(sys.argv[4])

found = 0

topoRequest = requests.get('http://' + node + ':8181/onos/v1/topology/clusters/'
                           + cluster
                           + "/devices",
                           auth=HTTPBasicAuth('onos', 'rocks'))

if topoRequest.status_code != 200:
    print topoRequest.text
    sys.exit(1)

topoJson = topoRequest.json()

for deviceIndex in range(first, last+1):
    lookingFor = "of:" + format(deviceIndex, '016x')
    print lookingFor
    for arrayIndex in range(0, len(topoJson["devices"])):
        device = topoJson["devices"][arrayIndex]
        if device == lookingFor:
            found = found + 1
            print "Match found for " + device
            break


if found == last - first:
    sys.exit(0)

print "Found " + str(found) + " matches, need " + str(last - first)
sys.exit(2)





