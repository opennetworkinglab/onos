#! /usr/bin/env python

import requests
import sys

from requests.auth import HTTPBasicAuth

if len(sys.argv) < 3:
    print "usage: find-dhcp-netcfg onos-node name1=value1 ..."
    sys.exit(1)

node = sys.argv[1]

cfgRequest = requests.get('http://' + node + ':8181/onos/v1/network/configuration',
                          auth=HTTPBasicAuth('onos', 'rocks'))

if cfgRequest.status_code != 200:
    print cfgRequest.text
    sys.exit(1)

cfgJson = cfgRequest.json()
appFound = False


for index in range(2, len(sys.argv)):
    pair = sys.argv[index].split("=")
    for app in cfgJson["apps"]:
        if app == "org.onosproject.dhcp":
            dhcp = cfgJson["apps"][app]["dhcp"]
            appFound = True

            name = pair[0]
            value = pair[1]

            if dhcp[name] != value:
                print name + " differs: expected " + value + " but found " + dhcp[name]
                print cfgJson
                sys.exit(1)

if appFound:
    sys.exit(0)

print "DHCP app not found"
print cfgJson
sys.exit(2)




