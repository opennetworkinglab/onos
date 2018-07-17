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

    /*
     * General actions.
     */
    action pop_vlan() {
        hdr.ethernet.ether_type = hdr.vlan_tag.ether_type;
        hdr.vlan_tag.setInvalid();
    }

    action rewrite_smac(mac_addr_t smac) {
        hdr.ethernet.src_addr = smac;
    }

    action rewrite_dmac(mac_addr_t dmac) {
        hdr.ethernet.dst_addr = dmac;
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

    /*
     * VLAN Metadata Table.
     * Modify VLAN Id according to metadata from NextObjective(next id).
     */
    direct_counter(CounterType.packets_and_bytes) vlan_meta_counter;

    action set_vlan(vlan_id_t new_vlan_id) {
        hdr.vlan_tag.vlan_id = new_vlan_id;
        vlan_meta_counter.count();
    }

    table vlan_meta {
        key = {
            fabric_metadata.next_id: exact;
        }

        actions = {
            set_vlan;
            @defaultonly nop;
        }
        default_action = nop;
        counters = vlan_meta_counter;
    }

    /*
     * Simple Table.
     * Do a single egress action based on next id.
     */
    direct_counter(CounterType.packets_and_bytes) simple_counter;

    action output_simple(port_num_t port_num) {
        standard_metadata.egress_spec = port_num;
        simple_counter.count();
    }

    action set_vlan_output(vlan_id_t new_vlan_id, port_num_t port_num){
        hdr.vlan_tag.vlan_id = new_vlan_id;
        output_simple(port_num);
    }

    action l3_routing_simple(port_num_t port_num, mac_addr_t smac, mac_addr_t dmac) {
        rewrite_smac(smac);
        rewrite_dmac(dmac);
        output_simple(port_num);
    }

    action mpls_routing_v4_simple(port_num_t port_num, mac_addr_t smac, mac_addr_t dmac,
                            mpls_label_t label) {
        l3_routing_simple(port_num, smac, dmac);

        // TODO: set tc according to diffserv from ipv4
        push_mpls(label, 3w0);
    }

    action mpls_routing_v6_simple (port_num_t port_num, mac_addr_t smac, mac_addr_t dmac,
                            mpls_label_t label) {
        l3_routing_simple(port_num, smac, dmac);

        // TODO: set tc according to traffic_class from ipv4
        push_mpls(label, 3w0);
    }

    action l3_routing_vlan(port_num_t port_num, mac_addr_t smac, mac_addr_t dmac, vlan_id_t new_vlan_id) {
        rewrite_smac(smac);
        rewrite_dmac(dmac);
        set_vlan_output(new_vlan_id, port_num);
    }

    table simple {
        key = {
            fabric_metadata.next_id: exact;
        }

        actions = {
            output_simple;
            set_vlan_output;
            l3_routing_simple;
            mpls_routing_v4_simple;
            mpls_routing_v6_simple;
            l3_routing_vlan;
        }
        counters = simple_counter;
    }

    /*
     * Hashed table.
     * Execute an action profile group based on next id.
     * One action profile group may contains multple egress decision.
     * The execution picks one action profile group memebr by using 5-tuple
     * hashing.
     */
    action_selector(HashAlgorithm.crc16, 32w64, 32w16) ecmp_selector;
    direct_counter(CounterType.packets_and_bytes) hashed_counter;

    action output_hashed(port_num_t port_num) {
        standard_metadata.egress_spec = port_num;
        hashed_counter.count();
    }

    action l3_routing_hashed(port_num_t port_num, mac_addr_t smac, mac_addr_t dmac) {
        rewrite_smac(smac);
        rewrite_dmac(dmac);
        output_hashed(port_num);
    }

    action mpls_routing_v4_hashed (port_num_t port_num, mac_addr_t smac, mac_addr_t dmac,
                            mpls_label_t label) {
        l3_routing_hashed(port_num, smac, dmac);

        // TODO: set tc according to diffserv from ipv4
        push_mpls(label, 3w0);
    }

    action mpls_routing_v6_hashed (port_num_t port_num, mac_addr_t smac, mac_addr_t dmac,
                            mpls_label_t label) {
        l3_routing_hashed(port_num, smac, dmac);

        // TODO: set tc according to traffic_class from ipv4
        push_mpls(label, 3w0);
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
            l3_routing_hashed;
            mpls_routing_v4_hashed;
            mpls_routing_v6_hashed;
        }

        implementation = ecmp_selector;
        counters = hashed_counter;
    }

    /*
     * Multicast Table.
     * Setup multicast group id for packet replication engine (PRE).
     */
    direct_counter(CounterType.packets_and_bytes) multicast_counter;

    action set_mcast_group(group_id_t gid) {
        standard_metadata.mcast_grp = gid;
        fabric_metadata.drop_if_egress_is_ingress = _TRUE;
        multicast_counter.count();
    }

    table multicast {
        key = {
            fabric_metadata.next_id: exact;
        }
        actions = {
            set_mcast_group;
        }
        counters = multicast_counter;
    }

    apply {
        vlan_meta.apply();
        if (!simple.apply().hit) {
            if (!hashed.apply().hit) {
                if (!multicast.apply().hit) {
                    // Next ID doesn't match any table.
                    return;
                }
            }
        }
        // Decrement TTL
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
}

control EgressNextControl (
    inout parsed_headers_t hdr,
    inout fabric_metadata_t fabric_metadata,
    inout standard_metadata_t standard_metadata) {

    /*
     * Egress VLAN Table.
     * Pops VLAN tag according to interface(Port and VLAN) configuration.
     */
    direct_counter(CounterType.packets_and_bytes) egress_vlan_counter;

    action pop_vlan() {
        hdr.ethernet.ether_type = hdr.vlan_tag.ether_type;
        hdr.vlan_tag.setInvalid();
        egress_vlan_counter.count();
    }

    table egress_vlan {
        key = {
            hdr.vlan_tag.vlan_id: exact;
            standard_metadata.egress_port: exact;
        }
        actions = {
            pop_vlan;
            @defaultonly nop;
        }
        default_action = nop;
        counters = egress_vlan_counter;
    }

    apply {
        if (fabric_metadata.drop_if_egress_is_ingress == _TRUE
             && standard_metadata.ingress_port == standard_metadata.egress_port) {
            drop_now();
        }
        egress_vlan.apply();
    }
}
