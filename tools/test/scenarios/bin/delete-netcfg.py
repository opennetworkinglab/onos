#! /usr/bin/env python

import requests

from requests.auth import HTTPBasicAuth
import sys



if len(sys.argv) != 3:
    print "usage: delete-netcfg onos-node config-name"
    sys.exit(1)

node = sys.argv[1]
configName = sys.argv[2]

intentRequest = requests.delete('http://' + node + ':8181/onos/v1/network/configuration/' + configName,
                              auth=HTTPBasicAuth('onos', 'rocks'))

if intentRequest.status_code != 204:
    print intentRequest.text
    sys.exit(1)

sys.exit(0)



