#! /usr/bin/env python

import requests

from requests.auth import HTTPBasicAuth
import sys



if len(sys.argv) != 6:
    print "usage: create-flow onos-node name device in-port out-port"
    sys.exit(1)

node = sys.argv[1]
name = sys.argv[2]
device = sys.argv[3]
inPort = sys.argv[4]
outPort = sys.argv[5]

flowJsonTemplate = \
    '{{' + \
        '"priority": 1,' + \
        '"isPermanent": true,' + \
        '"treatment": {{' + \
            '"instructions": [' + \
                '{{' + \
                    '"type": "OUTPUT",' + \
                    '"port": {}' + \
                '}}' + \
            ']' + \
        '}},' + \
        '"selector": {{' + \
            '"criteria": [' + \
                '{{' + \
                    '"type": "IN_PORT",' + \
                    '"port": {}' + \
                '}}' + \
            ']' + \
        '}}' + \
    '}}'

flowJson = flowJsonTemplate.format(inPort, outPort)
payload = {'appId': 'org.onosproject.cli'}
flowRequest = requests.post('http://' + node + ':8181/onos/v1/flows/' + device,
                              auth=HTTPBasicAuth('onos', 'rocks'),
                              data=flowJson,
                              params=payload)

if flowRequest.status_code != 201:
    print flowRequest.text
    sys.exit(1)

location = flowRequest.headers["location"]
print "@stc " + name + "Location=" + location
sys.exit(0)



