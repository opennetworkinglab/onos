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

#include "../define.p4"
#include "../header.p4"
#include "../action.p4"


control Forwarding (
    inout parsed_headers_t hdr,
    inout fabric_metadata_t fabric_metadata,
    inout standard_metadata_t standard_metadata) {

    /*
     * Bridging Table.
     * Matches destination mac address and VLAN Id and make egress decision.
     */
    direct_counter(CounterType.packets_and_bytes) bridging_counter;

    action set_next_id_bridging(next_id_t next_id) {
        fabric_metadata.next_id = next_id;
        bridging_counter.count();
    }

    table bridging {
        key = {
            hdr.vlan_tag.vlan_id: exact;
            hdr.ethernet.dst_addr: ternary;
        }

        actions = {
            set_next_id_bridging;
        }
        counters = bridging_counter;
    }

    /*
     * MPLS Table.
     * Matches MPLS label and make egress decision.
     */
    direct_counter(CounterType.packets_and_bytes) mpls_counter;

    action pop_mpls_and_next(next_id_t next_id) {
        hdr.mpls.setInvalid();
        fabric_metadata.next_id = next_id;
        mpls_counter.count();
    }

    table mpls {
        key = {
            hdr.mpls.label: exact;
        }

        actions = {
            pop_mpls_and_next;
        }
        counters = mpls_counter;
    }

    /*
     * IPv4 Routing Table.
     * Matches IPv4 prefix and make egress decision.
     */
    direct_counter(CounterType.packets_and_bytes) routing_v4_counter;

    action set_next_id_routing_v4(next_id_t next_id) {
        fabric_metadata.next_id = next_id;
        routing_v4_counter.count();
    }

    action nop_routing_v4() {
        routing_v4_counter.count();
    }

    table routing_v4 {
        key = {
            hdr.ipv4.dst_addr: lpm;
        }

        actions = {
            set_next_id_routing_v4;
            nop_routing_v4;
        }
        counters = routing_v4_counter;
    }

    /*
     * ACL Table.
     * Make final egress decision based on general metch fields.
     */
    direct_counter(CounterType.packets_and_bytes) acl_counter;

    action set_next_id_acl(next_id_t next_id) {
        fabric_metadata.next_id = next_id;
        acl_counter.count();
    }

    // Send immendiatelly to CPU - skip the rest of pipeline.
    action punt_to_cpu() {
        standard_metadata.egress_spec = CPU_PORT;
        acl_counter.count();
        exit;
    }

    action clone_to_cpu() {
        // FIXME: works only if pkt will be replicated via PRE multicast group.
        fabric_metadata.clone_to_cpu = _TRUE;
        acl_counter.count();
    }

    action drop() {
        mark_to_drop();
        acl_counter.count();
    }

    action nop_acl() {
        acl_counter.count();
    }

    table acl {
        key = {
            standard_metadata.ingress_port: ternary; // 9
            fabric_metadata.ip_proto: ternary; // 8
            fabric_metadata.l4_src_port: ternary; // 16
            fabric_metadata.l4_dst_port: ternary; // 16

            hdr.ethernet.dst_addr: ternary; // 48
            hdr.ethernet.src_addr: ternary; // 48
            hdr.vlan_tag.vlan_id: ternary; // 12
            hdr.vlan_tag.ether_type: ternary; //16
            hdr.ipv4.src_addr: ternary; // 32
            hdr.ipv4.dst_addr: ternary; // 32
            hdr.icmp.icmp_type: ternary; // 8
            hdr.icmp.icmp_code: ternary; // 8
        }

        actions = {
            set_next_id_acl;
            punt_to_cpu;
            clone_to_cpu;
            drop;
            nop_acl;
        }

        const default_action = nop_acl();
        size = 128;
        counters = acl_counter;
    }

#ifdef WITH_IPV6
    /*
     * IPv6 Routing Table.
     * Matches IPv6 prefix and make egress decision.
     */
    direct_counter(CounterType.packets_and_bytes) routing_v6_counter;

    action set_next_id_routing_v6(next_id_t next_id) {
        fabric_metadata.next_id = next_id;
        routing_v6_counter.count();
    }

    table routing_v6 {
        key = {
            hdr.ipv6.dst_addr: lpm;
        }

        actions = {
            set_next_id_routing_v6;
        }
        counters = routing_v6_counter;
    }
#endif // WITH_IPV6

    apply {
        if(fabric_metadata.fwd_type == FWD_BRIDGING) bridging.apply();
        else if (fabric_metadata.fwd_type == FWD_MPLS) {
            mpls.apply();

            // TODO: IPv6
            hdr.vlan_tag.ether_type = ETHERTYPE_IPV4;
        }
        else if (fabric_metadata.fwd_type == FWD_IPV4_UNICAST) routing_v4.apply();
#ifdef WITH_IPV6
        else if (fabric_metadata.fwd_type == FWD_IPV6_UNICAST) routing_v6.apply();
#endif // WITH_IPV6
        acl.apply();
    }
}
