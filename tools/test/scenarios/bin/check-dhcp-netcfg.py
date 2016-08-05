#! /usr/bin/env python

import requests
import sys

from requests.auth import HTTPBasicAuth

if len(sys.argv) < 3:
    print "usage: find-dhcp-netcfg onos-node name1=value1 ..."
    sys.exit(1)

node = sys.argv[1]

cfgRequest = requests.get('http://' + node +
                          ':8181/onos/v1/network/configuration/apps/org.onosproject.dhcp',
                          auth=HTTPBasicAuth('onos', 'rocks'))

print cfgRequest.text

if cfgRequest.status_code != 200:
    sys.exit(1)

cfgJson = cfgRequest.json()

for index in range(2, len(sys.argv)):
    pair = sys.argv[index].split("=")

    dhcp = cfgJson["dhcp"]
    appFound = True

    name = pair[0]
    value = pair[1]

    if dhcp[name] != value:
        print name + " differs: expected " + value + " but found " + dhcp[name]
        print cfgJson
        sys.exit(1)


sys.exit(0)





