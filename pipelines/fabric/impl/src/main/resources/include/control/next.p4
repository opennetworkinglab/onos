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
    action routing(port_num_t port_num, mac_addr_t smac, mac_addr_t dmac) {
        rewrite_smac(smac);
        rewrite_dmac(dmac);
        output(port_num);
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

    table simple {
        key = {
            fabric_metadata.next_id: exact @name("next_id");
        }
        actions = {
            output_simple;
            routing_simple;
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
    #ifdef _CUSTOM_HASHED_SELECTOR_ANNOTATION
        _CUSTOM_HASHED_SELECTOR_ANNOTATION
    #endif
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

    table hashed {
        key = {
            fabric_metadata.next_id: exact @name("next_id");
            fabric_metadata.ipv4_src_addr: selector;
            fabric_metadata.ipv4_dst_addr: selector;
            fabric_metadata.ip_proto: selector;
            fabric_metadata.l4_sport: selector;
            fabric_metadata.l4_dport: selector;
        }
        actions = {
            output_hashed;
            routing_hashed;
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
    }
}

control EgressNextControl (inout parsed_headers_t hdr,
                           inout fabric_metadata_t fabric_metadata,
                           inout standard_metadata_t standard_metadata) {
    @hidden
    action pop_mpls_if_present() {
        hdr.mpls.setInvalid();
        // Assuming there's an IP header after the MPLS one.
        hdr.eth_type.value = fabric_metadata.ip_eth_type;
    }

    @hidden
    action set_mpls() {
        hdr.mpls.setValid();
        hdr.mpls.label = fabric_metadata.mpls_label;
        hdr.mpls.tc = 3w0;
        hdr.mpls.bos = 1w1; // BOS = TRUE
        hdr.mpls.ttl = fabric_metadata.mpls_ttl; // Decrement after push.
        hdr.eth_type.value = ETHERTYPE_MPLS;
    }

    @hidden
    action push_outer_vlan() {
        // If VLAN is already valid, we overwrite it with a potentially new VLAN
        // ID, and same CFI, PRI, and eth_type values found in ingress.
        hdr.vlan_tag.setValid();
        hdr.vlan_tag.cfi = fabric_metadata.vlan_cfi;
        hdr.vlan_tag.pri = fabric_metadata.vlan_pri;
        hdr.vlan_tag.eth_type = ETHERTYPE_VLAN;
        hdr.vlan_tag.vlan_id = fabric_metadata.vlan_id;
    }

#ifdef WITH_DOUBLE_VLAN_TERMINATION
    @hidden
    action push_inner_vlan() {
        // Push inner VLAN TAG, rewriting correclty the outer vlan eth_type
        hdr.inner_vlan_tag.setValid();
        hdr.inner_vlan_tag.cfi = fabric_metadata.inner_vlan_cfi;
        hdr.inner_vlan_tag.pri = fabric_metadata.inner_vlan_pri;
        hdr.inner_vlan_tag.vlan_id = fabric_metadata.inner_vlan_id;
        hdr.inner_vlan_tag.eth_type = ETHERTYPE_VLAN;
        hdr.vlan_tag.eth_type = ETHERTYPE_VLAN;
    }
#endif // WITH_DOUBLE_VLAN_TERMINATION

    /*
     * Egress VLAN Table.
     * Pushes or Pops the VLAN tag if the pair egress port and VLAN ID is matched.
     * Instead, it drops the packets on miss.
     */
    direct_counter(CounterType.packets_and_bytes) egress_vlan_counter;

    action push_vlan() {
        push_outer_vlan();
        egress_vlan_counter.count();
    }

    action pop_vlan() {
        hdr.vlan_tag.setInvalid();
        egress_vlan_counter.count();
    }

    action drop() {
        mark_to_drop(standard_metadata);
        egress_vlan_counter.count();
    }

    table egress_vlan {
        key = {
            fabric_metadata.vlan_id: exact @name("vlan_id");
            standard_metadata.egress_port: exact @name("eg_port");
        }
        actions = {
            push_vlan;
            pop_vlan;
            @defaultonly drop;
        }
        const default_action = drop();
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

#ifdef WITH_DOUBLE_VLAN_TERMINATION
        if (fabric_metadata.push_double_vlan == _TRUE) {
            // Double VLAN termination.
            push_outer_vlan();
            push_inner_vlan();
        } else {
            // If no push double vlan, inner_vlan_tag must be popped
            hdr.inner_vlan_tag.setInvalid();
#endif // WITH_DOUBLE_VLAN_TERMINATION
            // Port-based VLAN tagging; if there is no match drop the packet!
            egress_vlan.apply();
#ifdef WITH_DOUBLE_VLAN_TERMINATION
        }
#endif // WITH_DOUBLE_VLAN_TERMINATION

        // TTL decrement and check.
        if (hdr.mpls.isValid()) {
            hdr.mpls.ttl = hdr.mpls.ttl - 1;
            if (hdr.mpls.ttl == 0) mark_to_drop(standard_metadata);
        } else {
            if(hdr.ipv4.isValid() && fabric_metadata.fwd_type != FWD_BRIDGING) {
                hdr.ipv4.ttl = hdr.ipv4.ttl - 1;
                if (hdr.ipv4.ttl == 0) mark_to_drop(standard_metadata);
            }
#ifdef WITH_IPV6
            else if (hdr.ipv6.isValid() && fabric_metadata.fwd_type != FWD_BRIDGING) {
                hdr.ipv6.hop_limit = hdr.ipv6.hop_limit - 1;
                if (hdr.ipv6.hop_limit == 0) mark_to_drop(standard_metadata);
            }
#endif // WITH_IPV6
        }
    }
}
