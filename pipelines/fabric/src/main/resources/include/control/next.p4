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

control Next (inout parsed_headers_t hdr,
              inout fabric_metadata_t fabric_metadata,
              inout standard_metadata_t standard_metadata) {

    /*
     * General actions.
     */
    @hidden
    action output(port_num_t port_num) {
     standard_metadata.egress_spec = port_num;
    }

    @hidden
    action rewrite_smac(mac_addr_t smac) {
        hdr.ethernet.src_addr = smac;
    }

    @hidden
    action rewrite_dmac(mac_addr_t dmac) {
        hdr.ethernet.dst_addr = dmac;
    }

    @hidden
    action set_mpls_label(mpls_label_t label) {
        fabric_metadata.mpls_label = label;
    }

    @hidden
    action routing(port_num_t port_num, mac_addr_t smac, mac_addr_t dmac) {
        rewrite_smac(smac);
        rewrite_dmac(dmac);
        output(port_num);
    }

    @hidden
    action mpls_routing(port_num_t port_num, mac_addr_t smac, mac_addr_t dmac,
                        mpls_label_t label) {
        set_mpls_label(label);
        routing(port_num, smac, dmac);
    }

    /*
     * Next VLAN table.
     * Modify VLAN ID based on next ID.
     */
    direct_counter(CounterType.packets_and_bytes) next_vlan_counter;

    action set_vlan(vlan_id_t vlan_id) {
        fabric_metadata.vlan_id = vlan_id;
        next_vlan_counter.count();
    }

    table next_vlan {
        key = {
            fabric_metadata.next_id: exact @name("next_id");
        }
        actions = {
            set_vlan;
            @defaultonly nop;
        }
        const default_action = nop();
        counters = next_vlan_counter;
        size = NEXT_VLAN_TABLE_SIZE;
    }

#ifdef WITH_XCONNECT
    /*
     * Cross-connect table.
     * Bidirectional forwarding for the same next id.
     */
    direct_counter(CounterType.packets_and_bytes) xconnect_counter;

    action output_xconnect(port_num_t port_num) {
        output(port_num);
        xconnect_counter.count();
    }

    action set_next_id_xconnect(next_id_t next_id) {
        fabric_metadata.next_id = next_id;
        xconnect_counter.count();
    }

    table xconnect {
        key = {
            standard_metadata.ingress_port: exact @name("ig_port");
            fabric_metadata.next_id: exact @name("next_id");
        }
        actions = {
            output_xconnect;
            set_next_id_xconnect;
            @defaultonly nop;
        }
        counters = xconnect_counter;
        const default_action = nop();
        size = XCONNECT_NEXT_TABLE_SIZE;
    }
#endif // WITH_XCONNECT

#ifdef WITH_SIMPLE_NEXT
    /*
     * Simple Table.
     * Do a single egress action based on next id.
     */
    direct_counter(CounterType.packets_and_bytes) simple_counter;

    action output_simple(port_num_t port_num) {
        output(port_num);
        simple_counter.count();
    }

    action routing_simple(port_num_t port_num, mac_addr_t smac, mac_addr_t dmac) {
        routing(port_num, smac, dmac);
        simple_counter.count();
    }

    action mpls_routing_simple(port_num_t port_num, mac_addr_t smac, mac_addr_t dmac,
                               mpls_label_t label) {
        mpls_routing(port_num, smac, dmac, label);
        simple_counter.count();
    }

    table simple {
        key = {
            fabric_metadata.next_id: exact @name("next_id");
        }
        actions = {
            output_simple;
            routing_simple;
            mpls_routing_simple;
            @defaultonly nop;
        }
        const default_action = nop();
        counters = simple_counter;
        size = SIMPLE_NEXT_TABLE_SIZE;
    }
#endif // WITH_SIMPLE_NEXT

#ifdef WITH_HASHED_NEXT
    /*
     * Hashed table.
     * Execute an action profile selector based on next id.
     */
    @max_group_size(HASHED_SELECTOR_MAX_GROUP_SIZE)
    action_selector(HashAlgorithm.crc16, HASHED_ACT_PROFILE_SIZE, 32w16) hashed_selector;
    direct_counter(CounterType.packets_and_bytes) hashed_counter;

    action output_hashed(port_num_t port_num) {
        output(port_num);
        hashed_counter.count();
    }

    action routing_hashed(port_num_t port_num, mac_addr_t smac, mac_addr_t dmac) {
        routing(port_num, smac, dmac);
        hashed_counter.count();
    }

    action mpls_routing_hashed(port_num_t port_num, mac_addr_t smac, mac_addr_t dmac,
                               mpls_label_t label) {
        mpls_routing(port_num, smac, dmac, label);
        hashed_counter.count();
    }

    table hashed {
        key = {
            fabric_metadata.next_id: exact @name("next_id");
            hdr.ipv4.dst_addr: selector;
            hdr.ipv4.src_addr: selector;
            fabric_metadata.ip_proto: selector;
            fabric_metadata.l4_sport: selector;
            fabric_metadata.l4_dport: selector;
        }
        actions = {
            output_hashed;
            routing_hashed;
            mpls_routing_hashed;
            @defaultonly nop;
        }
        implementation = hashed_selector;
        counters = hashed_counter;
        const default_action = nop();
        size = HASHED_NEXT_TABLE_SIZE;
    }
#endif // WITH_HASHED_NEXT

    /*
     * Multicast
     * Maps next IDs to PRE multicat group IDs.
     */
    direct_counter(CounterType.packets_and_bytes) multicast_counter;

    action set_mcast_group_id(mcast_group_id_t group_id) {
        standard_metadata.mcast_grp = group_id;
        fabric_metadata.is_multicast = _TRUE;
        multicast_counter.count();
    }

    table multicast {
        key = {
            fabric_metadata.next_id: exact @name("next_id");
        }
        actions = {
            set_mcast_group_id;
            @defaultonly nop;
        }
        counters = multicast_counter;
        const default_action = nop();
        size = MULTICAST_NEXT_TABLE_SIZE;
    }

    apply {
#ifdef WITH_XCONNECT
        // xconnect might set a new next_id.
        xconnect.apply();
#endif // WITH_XCONNECT
#ifdef WITH_SIMPLE_NEXT
        simple.apply();
#endif // WITH_SIMPLE_NEXT
#ifdef WITH_HASHED_NEXT
        hashed.apply();
#endif // WITH_HASHED_NEXT
        multicast.apply();
        next_vlan.apply();
    }
}

control EgressNextControl (inout parsed_headers_t hdr,
                           inout fabric_metadata_t fabric_metadata,
                           inout standard_metadata_t standard_metadata) {
    @hidden
    action pop_mpls_if_present() {
        hdr.mpls.setInvalid();
        // Assuming there's an IP header after the MPLS one.
        fabric_metadata.eth_type = fabric_metadata.ip_eth_type;
    }

    @hidden
    action set_mpls() {
        hdr.mpls.setValid();
        hdr.mpls.label = fabric_metadata.mpls_label;
        hdr.mpls.tc = 3w0;
        hdr.mpls.bos = 1w1; // BOS = TRUE
        hdr.mpls.ttl = fabric_metadata.mpls_ttl; // Decrement after push.
        fabric_metadata.eth_type = ETHERTYPE_MPLS;
    }

    @hidden
    action push_vlan() {
        // If VLAN is already valid, we overwrite it with a potentially new VLAN
        // ID, and same CFI, PRI, and eth_type values found in ingress.
        hdr.vlan_tag.setValid();
        hdr.vlan_tag.cfi = fabric_metadata.vlan_cfi;
        hdr.vlan_tag.pri = fabric_metadata.vlan_pri;
        hdr.vlan_tag.eth_type = fabric_metadata.eth_type;
        hdr.vlan_tag.vlan_id = fabric_metadata.vlan_id;
        hdr.ethernet.eth_type = ETHERTYPE_VLAN;
    }

    /*
     * Egress VLAN Table.
     * Pops the VLAN tag if the pair egress port and VLAN ID is matched.
     */
    direct_counter(CounterType.packets_and_bytes) egress_vlan_counter;

    action pop_vlan() {
        hdr.ethernet.eth_type = fabric_metadata.eth_type;
        hdr.vlan_tag.setInvalid();
        egress_vlan_counter.count();
    }

    table egress_vlan {
        key = {
            fabric_metadata.vlan_id: exact @name("vlan_id");
            standard_metadata.egress_port: exact @name("eg_port");
        }
        actions = {
            pop_vlan;
            @defaultonly nop;
        }
        const default_action = nop();
        counters = egress_vlan_counter;
        size = EGRESS_VLAN_TABLE_SIZE;
    }

    apply {
        if (fabric_metadata.is_multicast == _TRUE
             && standard_metadata.ingress_port == standard_metadata.egress_port) {
            mark_to_drop(standard_metadata);
        }

        if (fabric_metadata.mpls_label == 0) {
            if (hdr.mpls.isValid()) pop_mpls_if_present();
        } else {
            set_mpls();
        }

        if (!egress_vlan.apply().hit) {
            // Push VLAN tag if not the default one.
            if (fabric_metadata.vlan_id != DEFAULT_VLAN_ID) {
                push_vlan();
            }
        }

        // TTL decrement and check.
        if (hdr.mpls.isValid()) {
            hdr.mpls.ttl = hdr.mpls.ttl - 1;
            if (hdr.mpls.ttl == 0) mark_to_drop(standard_metadata);
        } else {
            if(hdr.ipv4.isValid()) {
                hdr.ipv4.ttl = hdr.ipv4.ttl - 1;
                if (hdr.ipv4.ttl == 0) mark_to_drop(standard_metadata);
            }
#ifdef WITH_IPV6
            else if (hdr.ipv6.isValid()) {
                hdr.ipv6.hop_limit = hdr.ipv6.hop_limit - 1;
                if (hdr.ipv6.hop_limit == 0) mark_to_drop(standard_metadata);
            }
#endif // WITH_IPV6
        }
    }
}
