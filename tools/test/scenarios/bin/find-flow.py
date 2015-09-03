#! /usr/bin/env python

import requests
import sys

from requests.auth import HTTPBasicAuth

if len(sys.argv) != 4:
    print "usage: find-flow onos-node name device-id"
    sys.exit(1)

node = sys.argv[1]
name = sys.argv[2]
deviceId = sys.argv[3]

flowsRequest = requests.get('http://' + node + ':8181/onos/v1/flows/' + deviceId,
                            auth=HTTPBasicAuth('onos', 'rocks'))

if flowsRequest.status_code != 200:
    print flowsRequest.text
    sys.exit(1)

flowsJson = flowsRequest.json()

for flow in flowsJson["flows"]:
    if deviceId == flow["deviceId"]:
        for criterion in flow["selector"]["criteria"]:
            if criterion["type"] == 'IN_PORT' and criterion["port"] > 0:
                for instruction in flow["treatment"]["instructions"]:
                    if instruction["port"] > 0 and instruction["type"] == 'OUTPUT':
                        print "@stc " + name + "FlowState=" + flow["state"]
                        print "@stc " + name + "FlowId=" + flow["id"]
                        print "@stc " + name + "FlowPort=" + str(instruction["port"])
                        sys.exit(0)

sys.exit(1)




