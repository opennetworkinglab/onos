/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include <core.p4>
#include <v1model.p4>

#include "../header.p4"
#include "../action.p4"

control Next (
    inout parsed_headers_t hdr,
    inout fabric_metadata_t fabric_metadata,
    inout standard_metadata_t standard_metadata) {
    action_selector(HashAlgorithm.crc16, 32w64, 32w16) ecmp_selector;
    direct_counter(CounterType.packets_and_bytes) simple_counter;
    direct_counter(CounterType.packets_and_bytes) hashed_counter;

    action output(port_num_t port_num) {
        standard_metadata.egress_spec = port_num;
    }

    action set_vlan_output(vlan_id_t new_vlan_id, port_num_t port_num){
        hdr.vlan_tag.vlan_id = new_vlan_id;

        // don't remove the vlan from egress since we set the vlan to it.
        fabric_metadata.pop_vlan_at_egress = false;
        output(port_num);
    }

    action rewrite_smac(mac_addr_t smac) {
        hdr.ethernet.src_addr = smac;
    }

    action rewrite_dmac(mac_addr_t dmac) {
        hdr.ethernet.dst_addr = dmac;
    }

    action l3_routing(port_num_t port_num, mac_addr_t smac, mac_addr_t dmac) {
        rewrite_smac(smac);
        rewrite_dmac(dmac);
        output(port_num);
    }

    action push_mpls (mpls_label_t label, bit<3> tc) {
        // Suppose that the maximum number of label is one.
        hdr.mpls.setValid();
        hdr.vlan_tag.ether_type = ETHERTYPE_MPLS;
        hdr.mpls.label = label;
        hdr.mpls.tc = tc;
        hdr.mpls.bos = 1w1; // BOS = TRUE
        hdr.mpls.ttl = DEFAULT_MPLS_TTL;
    }

    action mpls_routing_v4 (port_num_t port_num, mac_addr_t smac, mac_addr_t dmac,
                            mpls_label_t label) {
        l3_routing(port_num, smac, dmac);

        // TODO: set tc according to diffserv from ipv4
        push_mpls(label, 3w0);
    }

    action mpls_routing_v6 (port_num_t port_num, mac_addr_t smac, mac_addr_t dmac,
                            mpls_label_t label) {
        l3_routing(port_num, smac, dmac);

        // TODO: set tc according to traffic_class from ipv4
        push_mpls(label, 3w0);
    }

    table simple {
        key = {
            fabric_metadata.next_id: exact;
        }

        actions = {
            output;
            set_vlan_output;
            l3_routing;
            mpls_routing_v4;
        }
        counters = simple_counter;
    }

    table hashed {
        key = {
            fabric_metadata.next_id: exact;
            hdr.ipv4.dst_addr: selector;
            hdr.ipv4.src_addr: selector;
            fabric_metadata.ip_proto: selector;
            fabric_metadata.l4_src_port: selector;
            fabric_metadata.l4_dst_port: selector;
        }

        actions = {
            l3_routing;
            mpls_routing_v4;
            mpls_routing_v6;
        }

        implementation = ecmp_selector;
        counters = hashed_counter;
    }

#ifdef WITH_MULTICAST
    /*
     * Work in progress
     */
    action set_mcast_group(group_id_t gid, mac_addr_t smac) {
        standard_metadata.mcast_grp = gid;
        rewrite_smac(smac);
    }

    direct_counter(CounterType.packets_and_bytes) multicast_counter;

    table multicast {
        key = {
            fabric_metadata.next_id: exact;
        }
        actions = {
            set_mcast_group;
        }
        counters = multicast_counter;
    }
#endif // WITH_MULTICAST

    apply {
        if (simple.apply().hit) {
            if (!hdr.mpls.isValid()) {
                if(hdr.ipv4.isValid()) {
                    hdr.ipv4.ttl = hdr.ipv4.ttl - 1;
                }
#ifdef WITH_IPV6
                else if (hdr.ipv6.isValid()) {
                    hdr.ipv6.hop_limit = hdr.ipv6.hop_limit - 1;
                }
#endif // WITH_IPV6
            }
        }
        hashed.apply();
#ifdef WITH_MULTICAST
        multicast.apply();
#endif // WITH_MULTICAST
    }
}

control EgressNextControl (
    inout parsed_headers_t hdr,
    inout fabric_metadata_t fabric_metadata,
    inout standard_metadata_t standard_metadata){

    apply {
        // pop internal vlan if the meta is set
        if (fabric_metadata.pop_vlan_at_egress) {
            hdr.ethernet.ether_type = hdr.vlan_tag.ether_type;
            hdr.vlan_tag.setInvalid();
        }
    }
}
