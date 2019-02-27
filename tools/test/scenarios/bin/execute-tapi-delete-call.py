#! /usr/bin/env python

import sys
import tapiHelper
import json
import requests

if len(sys.argv) != 3 and len(sys.argv) != 4:
    print "usage: execute-tapi-delete-call <onos-node> <deletion-type> [uuid]"
    print "\t- <onos-node> is onos IP. 'localhost' is invalid."
    print "\t- <deletion-type> is one of {line, client, both}, which mean line-side deletion, " \
          "client-side deletion, and all deletion respectively."
    print "\t- [uuid] is the created service uuid, which is optional. If uuid is empty, " \
          "all connectivity services with <deletion-type> will be deleted."
    print "\t  If [uuid] is not empty, and <deletion-type> is 'both', this script doesn't work."
    print "\t  Otherwise, delete line-side or client-side connectivity with specific uuid."
    print "For example, if we want to delete all client-side services on local host, the command should be:"
    print "\t python execute-tapi-delete-call.py 127.0.0.1 client"
    sys.exit(1)


#
# Define the input json string for service deletion.
#
def tapi_deletion_input(service_uuid):
    delete_input = {
        "tapi-connectivity:input":
        {
            "service-id-or-name": service_uuid
        }
    }
    return delete_input


#
# Return sip uuid pair of service structure
#
def parse_sip_uuid_pair(sv): return \
    (sv["end-point"][0]["service-interface-point"]["service-interface-point-uuid"],
     sv["end-point"][1]["service-interface-point"]["service-interface-point-uuid"])


#
# Find sip in sip array through sip uuid.
#
def find_sip(sip_uuid, sips):
    for sip in sips:
        if sip["uuid"] == sip_uuid:
            return sip
    return None


#
# Post service deletion request to ONOS.
#
def post_deletion(service_uuid, del_request):
    delete_input_json = json.dumps(tapi_deletion_input(service_uuid))
    print "\nThe json content of deletion operation for connectivity service is \n\t\t%s." % \
          delete_input_json
    headers = {'Content-type': 'application/json'}
    resp = requests.post(del_request, data=delete_input_json, headers=headers, auth=('onos', 'rocks'))
    if resp.status_code != 200:
        raise Exception('POST {}'.format(resp.status_code))
    return resp


# 1. Parse the input params.
node = sys.argv[1]
del_type = sys.argv[2]
serv_uuid = None
assert del_type in {"line", "client", "both"}
if len(sys.argv) == 4:
    serv_uuid = sys.argv[3]
# 2. Get the subtree of tapi-common:context
context_request = 'http://' + node + ':8181/onos/restconf/data/tapi-common:context'
delete_request = 'http://' + node + ':8181/onos/restconf/operations/tapi-connectivity:delete-connectivity-service'
context = tapiHelper.get_context(context_request)["tapi-common:context"]
sips = context["service-interface-point"]
try:
    services = context["tapi-connectivity:connectivity-context"]["connectivity-service"]
except KeyError:
    print "Warning - there is no connectivity service in ONOS (%s)." % node
    sys.exit(0)
# 3. handle deletion requests according to <deletion-type> and [uuid]
if serv_uuid is None:
    # If [uuid] is empty, traverse all services with <deletion-type>
    service_map = {}
    del_num = 0
    for service in services:
        src, _ = parse_sip_uuid_pair(service)
        sip = find_sip(src, sips)
        if ((del_type == "line" or del_type == "both") and tapiHelper.is_photonic_media(sip)) or \
           ((del_type == "client" or del_type == "both") and tapiHelper.is_dsr_media(sip)):
            json_resp = post_deletion(service["uuid"], delete_request)
            del_num += 1
            print "Returns json string for deletion operations is\n %s\n" % json_resp
    if del_num == 0:
        print "Warning - there is no %s-side connectivity servicein ONOS (%s)." % (del_type, node)
else:
    # If [uuid] is not empty, check the <deletion-type>
    if del_type == "both":
        print "Error - The option 'both' is illegal when [uuid] is assigned."
    else:
        is_del = False
        for service in services:
            if service["uuid"] == serv_uuid:
                src, _ = parse_sip_uuid_pair(service)
                sip = find_sip(src, sips)
                if (del_type == "line" and tapiHelper.is_photonic_media(sip)) or \
                   (del_type == "client" and tapiHelper.is_dsr_media(sip)):
                    json_resp = post_deletion(service["uuid"], delete_request)
                    is_del = True
                    print "Returns json string for deletion operations is\n %s\n" % json_resp
                    break
        if not is_del:
            print "Warning - Cannot find %s-side connectivity service with service uuid %s." % (del_type, serv_uuid)
