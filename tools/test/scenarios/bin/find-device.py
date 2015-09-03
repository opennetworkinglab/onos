#! /usr/bin/env python

import requests
import sys
import urllib

from requests.auth import HTTPBasicAuth

if len(sys.argv) != 4:
    print "usage: find-device onos-node name device-id"
    sys.exit(1)

node = sys.argv[1]
name = sys.argv[2]
id = sys.argv[3]

deviceRequest = requests.get('http://' + node + ':8181/onos/v1/devices/' +
                             urllib.quote_plus(id),
                             auth=HTTPBasicAuth('onos', 'rocks'))

if deviceRequest.status_code != 200:
    print deviceRequest.text
    sys.exit(1)

deviceJson = deviceRequest.json()

print "@stc " + name + "Id=" + deviceJson["id"]
print "@stc " + name + "Type=" + deviceJson["type"]
print "@stc " + name + "Available=" + str(deviceJson["available"])
channelId = deviceJson["annotations"]["channelId"]
channelIdWords = channelId.split(':')
print "@stc " + name + "IpAddress=" + channelIdWords[0]

sys.exit(0)





