#!/usr/bin/python

import requests
import json
import random
from sets import Set


#
# Creates client-side connectivity json
#
def tapi_client_input(sip_uuids):
    create_input = {
              "tapi-connectivity:input": {
                "end-point" : [
                  {
                    "local-id": sip_uuids[0],
                    "service-interface-point": {
                        "service-interface-point-uuid" : sip_uuids[0]
                    }
                  },
                  {
                    "local-id": sip_uuids[1],
                    "service-interface-point": {
                        "service-interface-point-uuid" : sip_uuids[1]
                    }
                  }
                ]
              }
            }
    return create_input


#
# Creates line-side connectivity json
#
def tapi_line_input(sip_uuids):
    create_input = {
      "tapi-connectivity:input" : {
         "end-point": [
            {
               "layer-protocol-qualifier": "tapi-photonic-media:PHOTONIC_LAYER_QUALIFIER_NMC",
               "role": "UNKNOWN",
               "local-id": "Src_end_point",
               "direction": "BIDIRECTIONAL",
               "service-interface-point": {
                  "service-interface-point-uuid" : sip_uuids[0]
               },
               "protection-role": "WORK",
               "layer-protocol-name": "PHOTONIC_MEDIA"
            },
            {
               "direction": "BIDIRECTIONAL",
               "service-interface-point": {
                  "service-interface-point-uuid": sip_uuids[1]
               },
               "protection-role": "WORK",
               "layer-protocol-name": "PHOTONIC_MEDIA",
               "layer-protocol-qualifier": "tapi-photonic-media:PHOTONIC_LAYER_QUALIFIER_NMC",
               "role": "UNKNOWN",
               "local-id": "Dst_end_point"
            }
         ]
      }
    }
    return create_input


#
# Obtains TAPI context through restconf
#
def get_context(url_context):
    resp = requests.get(url_context, auth=('onos', 'rocks'))
    if resp.status_code != 200:
        raise Exception('GET {}'.format(resp.status_code))
    return resp.json()


#
# Check if the node is transponder.
# True  - transponder
# False - OLS
#
def is_transponder_node(node):
    if len(node["owned-node-edge-point"]) > 0 and "mapped-service-interface-point" in node["owned-node-edge-point"][0]:
        return True
    else:
        return False


#
# Parse src and dst sip-uuids of specific link from topo.
# The ports should be line side but not client side.
#
def parse_src_dst(topo, link_index=-1):
    if link_index == -1:
        # select a link randomly from all links of topo
        link_index = random.randint(0, len(topo["link"]) - 1)
    nep_pair = topo["link"][link_index]["node-edge-point"]
    assert topo["uuid"] == nep_pair[0]["topology-uuid"]
    assert topo["uuid"] == nep_pair[1]["topology-uuid"]
    src_onep, dst_onep = (find_line_onep(nep_pair[0], topo["node"]),
                          find_line_onep(nep_pair[1], topo["node"]))
    if src_onep is not None and dst_onep is not None:
        # If the link is between two transponders directly
        pass
    elif src_onep is None and dst_onep is None:
        raise AssertionError("Impossible for that both two ports are OLS port")
    else:
        # If one of src_onep and dst_onep is None, then make src_onep not None,
        # and find a new dst_onep with same connection id.
        if src_onep is None:
            src_onep = dst_onep
            dst_onep = None
        conn_id = parse_value(src_onep["name"])["odtn-connection-id"]
        for node in topo["node"]:
            cep = src_onep["tapi-connectivity:cep-list"]["connection-end-point"]
            assert len(cep) == 1
            if cep[0]["parent-node-edge-point"]["node-uuid"] != node["uuid"] and is_transponder_node(node):
                # If this node is not the node that includes src_onep, and not a OLS node
                for onep in node["owned-node-edge-point"]:
                    if parse_value(onep["name"])["odtn-connection-id"] == conn_id\
                            and parse_value(onep["name"])["odtn-port-type"] == "line":
                        dst_onep = onep
                        break
            if dst_onep is not None:
                break

    src_sip_uuid, dst_sip_uuid = \
        (src_onep["mapped-service-interface-point"][0]["service-interface-point-uuid"],
         dst_onep["mapped-service-interface-point"][0]["service-interface-point-uuid"])

    return src_onep, dst_onep, src_sip_uuid, dst_sip_uuid


#
# Check whether the sip uuid is used in other existed services.
#
def is_port_used(sip_uuid, conn_context):
    try:
        for service in conn_context["connectivity-service"]:
            for id in [0, 1]:
                if service["end-point"][id]["service-interface-point"]["service-interface-point-uuid"] == sip_uuid:
                    return True
    except KeyError:
        print "There is no line-side service in ONOS now."
    return False


#
# Requests a connectivity service
#
def request_connection(url_connectivity, context):
    # All Context SIPs
    sips = context["tapi-common:context"]["service-interface-point"]

    # Sorted Photonic Media SIPs. filter is an iterable
    esips = list(filter(is_dsr_media, sorted(sips, key=lambda sip: sip["name"][0]["value"])))
    endpoints = [esips[0], esips[-1]]
    sip_uuids = []
    for sip in endpoints:
        sip_uuids.append(sip["uuid"])
    for uuid in sip_uuids:
        print(uuid)

    create_input_json = json.dumps(tapi_client_input(sip_uuids))
    print (create_input_json)
    headers = {'Content-type': 'application/json'}
    resp = requests.post(url_connectivity, data=create_input_json, headers=headers,  auth=('onos', 'rocks'))
    if resp.status_code != 200:
        raise Exception('POST {}'.format(resp.status_code))
    return resp


#
# Filter method used to keep only SIPs that are photonic_media
#
def is_photonic_media(sip):
    return sip["layer-protocol-name"] == "PHOTONIC_MEDIA"


#
# Filter method used to keep only SIPs that are DSR
#
def is_dsr_media(sip):
    return sip["layer-protocol-name"] == "DSR"


#
# Processes the topology to verify the correctness
#
def process_topology():
   # TODO use method to parse topology
   # Getting the Topology
   # topology = context["tapi-common:context"]["tapi-topology:topology-context"]["topology"][0]
   # nodes = topology["node"];
   # links = topology["link"];
   noop


#
# Find mapped client-side sip_uuid according to a line-side sip_uuid.
# connection-ids of these two owned-node-edge-point should be the same.
#
def find_mapped_client_sip_uuid(line_sip_uuid, nodes):
    line_node = None
    line_onep = None
    for node in nodes:
        if is_transponder_node(node):
            for onep in node["owned-node-edge-point"]:
                if onep["mapped-service-interface-point"][0]["service-interface-point-uuid"] == line_sip_uuid:
                    line_node = node
                    line_onep = onep
                    break
    if line_node is None:
        raise AssertionError("Cannot match line-side sip uuid in topology.")
    conn_id = parse_value(line_onep["name"])["odtn-connection-id"]
    for onep in line_node["owned-node-edge-point"]:
        vals = parse_value(onep["name"])
        if vals["odtn-connection-id"] == conn_id and vals["odtn-port-type"] == "client":
            return onep["mapped-service-interface-point"][0]["service-interface-point-uuid"], vals
    return None, None


#
# Create a client-side connection. Firstly, get the context, parsing for SIPs that connect
# with each other in line-side; Secondly, issue the request
#
def create_client_connection(url_context, url_connectivity):
    headers = {'Content-type': 'application/json'}
    context = get_context(url_context)
    # select the first topo from all topologies
    topo = context["tapi-common:context"]["tapi-topology:topology-context"]["topology"][0]
    # Gather all current used sip_uuids
    used_sip_uuids = Set()
    try:
        services = context["tapi-common:context"]["tapi-connectivity:connectivity-context"]["connectivity-service"]
        for service in services:
            used_sip_uuids.add(service["end-point"][0]["service-interface-point"]["service-interface-point-uuid"])
            used_sip_uuids.add(service["end-point"][1]["service-interface-point"]["service-interface-point-uuid"])
    except KeyError:
        print "There is no existed connectivity service inside ONOS."

    # select the first available line-side service as bridge. If there is no available line-side service,
    # then only create a client-to-client service for src and dst node.
    empty_client_src_sip_uuid, empty_client_dst_sip_uuid = None, None
    empty_src_name, empty_dst_name, empty_client_src_name, empty_client_dst_name = None, None, None, None
    for link_index in range(0, len(topo["link"])):
        src_onep, dst_onep, src_sip_uuid, dst_sip_uuid = parse_src_dst(topo, link_index)
        client_src_sip_uuid, client_src_name = find_mapped_client_sip_uuid(src_sip_uuid, topo["node"])
        client_dst_sip_uuid, client_dst_name = find_mapped_client_sip_uuid(dst_sip_uuid, topo["node"])
        if client_src_sip_uuid is None or client_dst_sip_uuid is None:
            # If there is no two mapped client-side ports existed, then skip all next steps,
            # and traverse the next link directly
            continue
        # firstly, check if line-side service exists
        # If line-side service exists
        if src_sip_uuid in used_sip_uuids and dst_sip_uuid in used_sip_uuids:
            # secondly, check if mapped client-side service exists
            if (client_src_sip_uuid not in used_sip_uuids) and (client_dst_sip_uuid not in used_sip_uuids):
                # If there is no such client-side connection exists
                # Create new client-side connection directly
                print "Create client-side connection between %s and %s." % \
                      (client_src_name["onos-cp"], client_dst_name["onos-cp"])
                create_input_json = json.dumps(tapi_client_input((client_src_sip_uuid, client_dst_sip_uuid)))
                resp = requests.post(url_connectivity, data=create_input_json, headers=headers,
                                     auth=('onos', 'rocks'))
                if resp.status_code != 200:
                    raise Exception('POST {}'.format(resp.status_code))
                return resp
            else:
                # If there exists such client-side connection
                # Do nothing, just continue
                pass
        else:
            # If line-side service doesn't exist
            # save 4 sip uuids, and continue
            empty_client_src_sip_uuid = client_src_sip_uuid
            empty_client_dst_sip_uuid = client_dst_sip_uuid
            empty_client_src_name = client_src_name
            empty_client_dst_name = client_dst_name
            empty_src_name = parse_value(src_onep["name"])
            empty_dst_name = parse_value(dst_onep["name"])
            pass

    # After FOR loop, if this method doesn't return, there is no available line-side
    # service for mapped client-side service creation.
    # So, we need to create two client-side services.
    if empty_client_src_sip_uuid is None:
        # None case means all client-side services exist.
        raise AssertionError("There is no available client-side service could be created.")
    else:
        print "Create client-side services:"
        print "\t- from %s to %s." % (empty_client_src_name["onos-cp"], empty_client_dst_name["onos-cp"])
        print "This service should go through:"
        print "\t- %s and %s." % (empty_src_name["onos-cp"], empty_dst_name["onos-cp"])
        create_input_json = json.dumps(tapi_client_input((empty_client_src_sip_uuid, empty_client_dst_sip_uuid)))
        resp = requests.post(url_connectivity, data=create_input_json, headers=headers,
                             auth=('onos', 'rocks'))
        if resp.status_code != 200:
            raise Exception('POST {}'.format(resp.status_code))
        return resp


#
# Parse array structure "name" under structure "owned node edge point"
#
def parse_value(arr):
    rtn = {}
    for item in arr:
        rtn[item["value-name"]] = item["value"]
    return rtn


#
# Find node edge point of node structure in topology with client-side port, by using nep with line-side port.
# The odtn-connection-id should be the same in both line-side nep and client-side nep
#
def find_client_onep(line_nep_in_link, nodes):
    for node in nodes:
        if node["uuid"] == line_nep_in_link["node-uuid"]:
            conn_id = None
            for onep in node["owned-node-edge-point"]:
                if onep["uuid"] == line_nep_in_link["node-edge-point-uuid"]:
                    name = parse_value(onep["name"])
                    if name["odtn-port-type"] == "line":
                        conn_id = name["odtn-connection-id"]
                        break
            if conn_id is None:
                raise AssertionError("Cannot find owned node edge point with node id %s and nep id %s."
                                     % (line_nep_in_link["node-uuid"], line_nep_in_link["node-edge-point-uuid"], ))
            for onep in node["owned-node-edge-point"]:
                name = parse_value(onep["name"])
                if name["odtn-port-type"] == "client" and name["odtn-connection-id"] == conn_id:
                    return onep

    return None


#
# Create a line-side connection. Firstly, get the context, parsing for SIPs with photonic_media type,
# and select one pair of them; Secondly, issue the request
#
def create_line_connection(url_context, url_connectivity):
    context = get_context(url_context)
    # select the first topo from all topologies
    topo = context["tapi-common:context"]["tapi-topology:topology-context"]["topology"][0]
    # select randomly the src_sip_uuid and dst_sip_uuid with same connection id.
    src_onep, dst_onep, src_sip_uuid, dst_sip_uuid = parse_src_dst(topo)
    while is_port_used(src_sip_uuid, context["tapi-common:context"]["tapi-connectivity:connectivity-context"]):
        print "Conflict occurs between randomly selected line-side link and existed ones."
        src_onep, dst_onep, src_sip_uuid, dst_sip_uuid = parse_src_dst(topo)

    print "\nBuild line-side connectivity:\n|Item|SRC|DST|\n|:--|:--|:--|\n|onos-cp|%s|%s|\n|connection id|%s|%s|\n|sip uuid|%s|%s|" % \
          (src_onep["name"][2]["value"], dst_onep["name"][2]["value"],
           src_onep["name"][1]["value"], dst_onep["name"][1]["value"],
           src_sip_uuid, dst_sip_uuid)
    create_input_json = json.dumps(tapi_line_input((src_sip_uuid, dst_sip_uuid)))
    print "\nThe json content of creation operation for line-side connectivity service is \n\t\t%s." % \
          create_input_json
    headers = {'Content-type': 'application/json'}
    resp = requests.post(url_connectivity, data=create_input_json, headers=headers, auth=('onos', 'rocks'))
    if resp.status_code != 200:
        raise Exception('POST {}'.format(resp.status_code))
    return resp


#
# find owned-node-edge-point from all nodes according to line_nep_in_links
#
def find_line_onep(line_nep_in_link, nodes):
    for node in nodes:
        if node["uuid"] == line_nep_in_link["node-uuid"]:
            if not is_transponder_node(node):
                break
            for onep in node["owned-node-edge-point"]:
                if onep["uuid"] == line_nep_in_link["node-edge-point-uuid"]:
                    # check the length equals 1 to verify the 1-to-1 mapping relationship
                    assert len(onep["mapped-service-interface-point"]) == 1
                    return onep
    # When node is OLS, this method will return None
    return None


#
# Obtains existing connectivity services
#
def get_connection(url_connectivity, uuid):
    # uuid is useless for this method
    json = '{}'
    headers = {'Content-type': 'application/json'}
    resp = requests.post(url_connectivity, data=json, headers=headers,  auth=('onos', 'rocks'))
    if resp.status_code != 200:
            raise Exception('POST {}'.format(resp.status_code))
    return resp
