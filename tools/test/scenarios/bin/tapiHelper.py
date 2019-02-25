#!/usr/bin/python

import requests
import json


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
                  }
                ,
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
         "end-point" : [
            {
               "layer-protocol-qualifier" : "tapi-photonic-media:PHOTONIC_LAYER_QUALIFIER_NMC",
               "role" : "UNKNOWN",
               "local-id" : "Src_end_point",
               "direction" : "BIDIRECTIONAL",
               "service-interface-point" : {
                  "service-interface-point-uuid" : sip_uuids[0]
               },
               "protection-role" : "WORK",
               "layer-protocol-name" : "PHOTONIC_MEDIA"
            },
            {
               "direction" : "BIDIRECTIONAL",
               "service-interface-point" : {
                  "service-interface-point-uuid" : sip_uuids[1]
               },
               "protection-role" : "WORK",
               "layer-protocol-name" : "PHOTONIC_MEDIA",
               "layer-protocol-qualifier" : "tapi-photonic-media:PHOTONIC_LAYER_QUALIFIER_NMC",
               "role" : "UNKNOWN",
               "local-id" : "Dst_end_point"
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
# Create a client-side connection. Firstly, get the context, parsing for SIPs that connect
# with each other in line-side; Secondly, issue the request
#
def create_client_connection(url_context, url_connectivity):
    context = get_context(url_context)
    # select the first topo from all topologies
    topo = context["tapi-common:context"]["tapi-topology:topology-context"]["topology"][0]

    # select the first link from all links of topo
    nep_pair = topo["link"][0]["node-edge-point"]
    assert topo["uuid"] == nep_pair[0]["topology-uuid"]
    assert topo["uuid"] == nep_pair[1]["topology-uuid"]
    (src_onep, dst_onep) = (find_client_onep(nep_pair[0], topo["node"]),
                            find_client_onep(nep_pair[1], topo["node"]))
    src_sip_uuid, dst_sip_uuid = \
        (src_onep["mapped-service-interface-point"][0]["service-interface-point-uuid"],
         dst_onep["mapped-service-interface-point"][0]["service-interface-point-uuid"])
    print "\nBuild client-side connectivity:\n|Item|SRC|DST|\n|:--|:--|:--|\n|onos-cp|%s|%s|\n|connection id|%s|%s|\n|sip uuid|%s|%s|" % \
          (src_onep["name"][2]["value"], dst_onep["name"][2]["value"],
           src_onep["name"][1]["value"], dst_onep["name"][1]["value"],
           src_sip_uuid, dst_sip_uuid)
    create_input_json = json.dumps(tapi_client_input((src_sip_uuid, dst_sip_uuid)))
    print "\nThe json content of creation operation for client-side connectivity service is \n\t\t%s." % \
          create_input_json
    headers = {'Content-type': 'application/json'}
    resp = requests.post(url_connectivity, data=create_input_json, headers=headers, auth=('onos', 'rocks'))
    if resp.status_code != 200:
        raise Exception('POST {}'.format(resp.status_code))
    return resp


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
                    assert onep["name"][1]["value-name"] == "odtn-connection-id"
                    assert onep["name"][0]["value"] == "line"
                    conn_id = onep["name"][1]["value"]
                    break
            if conn_id is None:
                raise AssertionError("Cannot find owned node edge point with node id %s and nep id %s."
                                     % (line_nep_in_link["node-uuid"], line_nep_in_link["node-edge-point-uuid"], ))
            for onep in node["owned-node-edge-point"]:
                if onep["name"][1]["value"] == conn_id and onep["name"][0]["value"] == "client":
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

    # select the first link from all links of topo
    nep_pair = topo["link"][0]["node-edge-point"]
    assert topo["uuid"] == nep_pair[0]["topology-uuid"]
    assert topo["uuid"] == nep_pair[1]["topology-uuid"]
    src_onep, dst_onep = (find_line_onep(nep_pair[0], topo["node"]),
                          find_line_onep(nep_pair[1], topo["node"]))
    src_sip_uuid, dst_sip_uuid = \
        (src_onep["mapped-service-interface-point"][0]["service-interface-point-uuid"],
         dst_onep["mapped-service-interface-point"][0]["service-interface-point-uuid"])
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


def find_line_onep(line_nep_in_link, nodes):
    for node in nodes:
        if node["uuid"] == line_nep_in_link["node-uuid"]:
            for onep in node["owned-node-edge-point"]:
                if onep["uuid"] == line_nep_in_link["node-edge-point-uuid"]:
                    # check the length equals 1 to verify the 1-to-1 mapping relationship
                    assert len(onep["mapped-service-interface-point"]) == 1
                    return onep
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
