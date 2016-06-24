#! /usr/bin/env python

import requests

from requests.auth import HTTPBasicAuth
import sys

if len(sys.argv) != 3:
    print "usage: post-netcfg onos-node json-file-name"
    sys.exit(1)

node = sys.argv[1]
configFileName = sys.argv[2]

jsonFile = open(configFileName, 'rb')
configJson = jsonFile.read()

request = requests.post('http://' + node + ':8181/onos/v1/network/configuration',
                        auth=HTTPBasicAuth('onos', 'rocks'),
                        data=configJson)

if request.status_code != 200:
    print request.text
    sys.exit(1)

sys.exit(0)



