#! /usr/bin/env python

import requests
import sys
import urllib

from requests.auth import HTTPBasicAuth

if len(sys.argv) != 4:
    print "usage: find-host onos-node name device-id"
    sys.exit(1)

node = sys.argv[1]
name = sys.argv[2]
id = sys.argv[3]

hostRequest = requests.get('http://' + node + ':8181/onos/v1/hosts/' +
                           urllib.quote_plus(id),
                           auth=HTTPBasicAuth('onos', 'rocks'))

if hostRequest.status_code != 200:
    print hostRequest.text
    sys.exit(1)

hostJson = hostRequest.json()

print "@stc " + name + "Id=" + hostJson["id"]
print "@stc " + name + "Mac=" + hostJson["mac"]
print "@stc " + name + "IpAddress=" + hostJson["ipAddresses"][0]

sys.exit(0)





