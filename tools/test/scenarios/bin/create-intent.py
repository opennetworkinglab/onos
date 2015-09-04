#! /usr/bin/env python

import requests

from requests.auth import HTTPBasicAuth
import sys



if len(sys.argv) != 7:
    print "usage: create-intent onos-node name ingressDevice ingressPort egressDevice egressPort"
    sys.exit(1)

node = sys.argv[1]
name = sys.argv[2]
ingress = sys.argv[3]
ingressPort = sys.argv[4]
egress = sys.argv[5]
egressPort = sys.argv[6]

intentJsonTemplate = \
    '{{' + \
        '"type": "PointToPointIntent",' + \
        '"appId": "org.onosproject.cli",' + \
        '"ingressPoint": {{' + \
        '    "device": "{}",' + \
        '    "port": "{}"' + \
        '}},' + \
        '"egressPoint": {{' + \
        '    "device": "{}",' + \
        '    "port": "{}"' + \
        '}}' + \
    '}}'

intentJson = intentJsonTemplate.format(ingress, ingressPort, egress, egressPort)
intentRequest = requests.post('http://' + node + ':8181/onos/v1/intents/',
                              auth=HTTPBasicAuth('onos', 'rocks'),
                              data=intentJson)

if intentRequest.status_code != 201:
    print intentRequest.text
    sys.exit(1)

location = intentRequest.headers["location"]
print "@stc " + name + "Location=" + location
sys.exit(0)



