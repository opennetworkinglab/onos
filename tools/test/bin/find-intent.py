#! /usr/bin/env python

import requests

from requests.auth import HTTPBasicAuth

r = requests.get('http://192.168.56.101:8181/onos/v1/flows', auth=HTTPBasicAuth('onos', 'rocks'))
deviceId = "of:0000000000000001"
port = 4
flowsJson = r.json()

for flow in flowsJson["flows"]:
    if deviceId == flow["deviceId"]:
        if flow["treatment"]["instructions"][0]["port"] == port:
            print flow




