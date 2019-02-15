#! /usr/bin/env python

import sys
import tapiHelper
import json

if len(sys.argv) < 4:
    print "usage: execute-tapi-post-call <onos-node> <context> <empty> [uuid]."
    print "\t- If <empty> is \"empty\", it measn that it should be no devices, links or ports"
    print "\t- Uuid is optional and defaults to empty"
    print "\t- For example:\n\t\t- line-side connectivity creation: %s\n\t\t- client-side connectivity creation: %s" % \
          ("python execute-tapi-post-call.py 127.0.0.1 tapi-connectivity:create-connectivity-service line-side",
           "python execute-tapi-post-call.py 127.0.0.1 tapi-connectivity:create-connectivity-service client-side")
    sys.exit(1)

node = sys.argv[1]
context = sys.argv[2]
empty = sys.argv[3]

if len(sys.argv) == 4:
    uuid = ""
else:
    uuid = sys.argv[4]
# request example:
# python execute-tapi-post-call.py localhost tapi-common:get-service-interface-point-list empty
if "get-connectivity-service-list" in context:
    connectivity_request = 'http://' + node + ':8181/onos/restconf/operations/' + context
    tapi_connection = tapiHelper.get_connection(connectivity_request, uuid)
    tapi_connection_json = tapi_connection.json()
    print tapi_connection_json
    if not tapi_connection_json["tapi-connectivity:output"] and empty != "empty":
       print "No connection was established"
       sys.exit(0)
    if empty == "empty":
        if not tapi_connection_json["tapi-connectivity:output"]:
            sys.exit(0)
        else:
            print "There exist some connectivities!!!"
            sys.exit(1)
    if uuid == "":
        # verify empty connection
        print tapi_connection_json
    elif uuid != "":
        # verify correct connection
        servs = tapi_connection_json["tapi-connectivity:output"]["service"]
        for s in range(len(servs)):
            if servs[s]['uuid'] == uuid:
                print "Find service with uuid %s" % uuid
                print servs[s]
                sys.exit(0)
    else:
        print "Invalid input for 3rd and 4th parameters."
        sys.exit(1)
    sys.exit(0)

# test succeeds by using cmd:
# python execute-tapi-post-call.py 127.0.0.1 tapi-connectivity:create-connectivity-service line-side
# python execute-tapi-post-call.py 127.0.0.1 tapi-connectivity:create-connectivity-service client-side
if "create-connectivity-service" in context:
    context_request = 'http://' + node + ':8181/onos/restconf/data/tapi-common:context'
    connectivity_request = 'http://' + node + ':8181/onos/restconf/operations/' + context
    if empty == "line-side":
        tapi_connection = tapiHelper.create_line_connection(context_request, connectivity_request)
    elif empty == "client-side":
        tapi_connection = tapiHelper.create_client_connection(context_request, connectivity_request)
    else:
        raise NotImplementedError("Not Implementation for option %s." % empty)
    print "\nThe request context is:\t%s." % context
    print "\nThe return message of the request is:\n\t\t%s " % json.dumps(tapi_connection.json())
    sys.exit(0)

sys.exit(1)
