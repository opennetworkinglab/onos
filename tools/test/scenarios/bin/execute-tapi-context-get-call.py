#! /usr/bin/env python

import requests
import sys
import tapiHelper

from requests.auth import HTTPBasicAuth

if len(sys.argv) < 3:
    print "usage: execute-tapi-context-get-call onos-node state"
    sys.exit(1)

node = sys.argv[1]
state = sys.argv[2] #if empty tapi context must be empty, if full it needs to contain all devices and ports

if state != "empty" and len(sys.argv) == 3:
    print "usage: execute-tapi-context-get-call onos-node full devices links ports"
    sys.exit(1)

request = 'http://' + node + ':8181/onos/restconf/data/tapi-common:context'
tapiContext = tapiHelper.get_context(request)

if state == "empty":
    uuid = tapiContext['tapi-common:context']['tapi-topology:topology-context']['topology'][0]['uuid']
    if uuid == "":
        print "empty uuid"
        sys.exit(1)
    print "@stc tapi topology uuid=" + uuid
    sys.exit(0)

if state == "full":
    devices = sys.argv[3]
    links = sys.argv[4]
    ports = sys.argv[5]
    #TODO parse reply for number of devices, links and ports
    print "Parsing for given topology not yet implemented"
    sys.exit(0)

sys.exit(1)




