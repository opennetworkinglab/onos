#! /usr/bin/env python

import requests
import sys
import urllib

from requests.auth import HTTPBasicAuth

if len(sys.argv) != 4:
    print "usage: find-topo-infrastructure onos-node name connect-point"
    sys.exit(1)

node = sys.argv[1]
name = sys.argv[2]
id = sys.argv[3]

infrastructureRequest = requests.get('http://' + node + ':8181/onos/v1/topology/infrastructure/' +
                           urllib.quote_plus(id),
                           auth=HTTPBasicAuth('onos', 'rocks'))

if infrastructureRequest.status_code != 200:
    print infrastructureRequest.text
    sys.exit(1)

infrastructureJson = infrastructureRequest.json()

print "@stc " + name + "Infrastructure=" + str(infrastructureJson["infrastructure"])

sys.exit(0)





