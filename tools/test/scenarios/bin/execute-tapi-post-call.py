#! /usr/bin/env python

import requests
import sys
import tapiHelper

from requests.auth import HTTPBasicAuth

if len(sys.argv) < 4:
    print "usage: execute-tapi-post-call onos-node context empty uuid. Uuid is optional and defaults to empty"
    sys.exit(1)

node = sys.argv[1]
context = sys.argv[2]
empty = sys.argv[3]

if len(sys.argv) == 4:
    uuid = ""
else:
    uuid = sys.argv[4]

if "get-connectivity-service-list" in context:
    connectivity_request = 'http://' + node + ':8181/onos/restconf/operations/' + context
    tapi_connection = tapiHelper.get_connection(connectivity_request, uuid)
    tapi_connection_json = tapi_connection.json()
    print tapi_connection_json
    if not tapi_connection_json["tapi-connectivity:output"] and empty != "empty":
       print "No connection was established"
       sys.exit(1)
    #TODO verify empty connection if uuid is empty
    #TODO verify correct connection if uuid is not empty
    sys.exit(0)

if "create-connectivity-service" in context:
    context_request = 'http://' + node + ':8181/onos/restconf/data/tapi-common:context'
    connectivity_request = 'http://' + node + ':8181/onos/restconf/operations/' + context
    tapi_connection = tapiHelper.create_connection(context_request, connectivity_request)
    print context
    print tapi_connection.json()
    sys.exit(0)

sys.exit(1)




