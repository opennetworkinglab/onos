#!/usr/bin/python3

import requests
import json
import itertools

#
# Creates connectivity json
#
def tapi_input(sip_uuids):
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

    create_input_json = json.dumps(tapi_input(sip_uuids))
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
    return sip["layer-protocol-name"]=="PHOTONIC_MEDIA"

#
# Filter method used to keep only SIPs that are DSR
#
def is_dsr_media(sip):
    return sip["layer-protocol-name"]=="DSR"

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
# Creates a connection first getting the context, parsing for SIPS and then issuing the request.
#
def create_connection(url_context, url_connectivity):
    context = get_context(url_context)
    return request_connection(url_connectivity, context)

#
# Obtains existing connectivity services
#
def get_connection(url_connectivity, uuid):
    if(uuid == ""):
        json = '{}'
    else:
        #TODO use uuid to retrieve given topo
        print "Not Yet implemented"
        json = '{}'
    headers = {'Content-type': 'application/json'}
    resp = requests.post(url_connectivity, data=json, headers=headers,  auth=('onos', 'rocks'))
    if resp.status_code != 200:
            raise Exception('POST {}'.format(resp.status_code))
    return resp






