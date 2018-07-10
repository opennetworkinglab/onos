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
     * IPv4 Unicast Table.
     * Matches IPv4 prefix and make egress decision.
     */
    direct_counter(CounterType.packets_and_bytes) unicast_v4_counter;

    action set_next_id_unicast_v4(next_id_t next_id) {
        fabric_metadata.next_id = next_id;
        unicast_v4_counter.count();
    }

    table unicast_v4 {
        key = {
            hdr.ipv4.dst_addr: lpm;
        }

        actions = {
            set_next_id_unicast_v4;
        }
        counters = unicast_v4_counter;
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

    action send_to_controller() {
        standard_metadata.egress_spec = CPU_PORT;
        acl_counter.count();
    }

    action drop() {
        mark_to_drop();
        acl_counter.count();
    }

    table acl {
        key = {
            standard_metadata.ingress_port: ternary; // 9
            fabric_metadata.ip_proto: ternary; // 8
            fabric_metadata.l4_src_port: ternary; // 16
            fabric_metadata.l4_dst_port: ternary; // 16
            fabric_metadata.original_ether_type: ternary; //16

            hdr.ethernet.dst_addr: ternary; // 48
            hdr.ethernet.src_addr: ternary; // 48
            hdr.vlan_tag.vlan_id: ternary; // 12
            hdr.ipv4.src_addr: ternary; // 32
            hdr.ipv4.dst_addr: ternary; // 32
            hdr.icmp.icmp_type: ternary; // 8
            hdr.icmp.icmp_code: ternary; // 8
        }

        actions = {
            set_next_id_acl;
            send_to_controller;
            drop;
            @defaultonly nop;
        }

        const default_action = nop();
        size = 256;
        counters = acl_counter;
    }

#ifdef WITH_MULTICAST
    /*
     * IPv4 Multicast Table.
     * Maches multcast IPv4 address and make egress decision.
     */
    direct_counter(CounterType.packets_and_bytes) multicast_v4_counter;
    action set_next_id_multicast_v4(next_id_t next_id) {
        fabric_metadata.next_id = next_id;
        multicast_v4_counter.count();
    }

    table multicast_v4 {
        key = {
            hdr.vlan_tag.vlan_id: exact;
            hdr.ipv4.dst_addr: lpm;
        }

        actions = {
            set_next_id_multicast_v4;
        }
        counters = multicast_v4_counter;
    }
#endif // WITH_MULTICAST

#ifdef WITH_IPV6
    /*
     * IPv6 Unicast Table.
     * Matches IPv6 prefix and make egress decision.
     */
    direct_counter(CounterType.packets_and_bytes) unicast_v6_counter;

    action set_next_id_unicast_v6(next_id_t next_id) {
        fabric_metadata.next_id = next_id;
        unicast_v6_counter.count();
    }

    table unicast_v6 {
        key = {
            hdr.ipv6.dst_addr: lpm;
        }

        actions = {
            set_next_id_unicast_v6;
        }
        counters = unicast_v6_counter;
    }

#ifdef WITH_MULTICAST
    /*
     * IPv6 Multicast Table.
     * Maches multcast IPv6 address and make egress decision.
     */
    direct_counter(CounterType.packets_and_bytes) multicast_v6_counter;

    action set_next_id_multicast_v6(next_id_t next_id) {
        fabric_metadata.next_id = next_id;
        multicast_v6_counter.count();
    }

    table multicast_v6 {
        key = {
            hdr.vlan_tag.vlan_id: exact;
            hdr.ipv6.dst_addr: lpm;
        }

        actions = {
            set_next_id_multicast_v6;
        }
        counters = multicast_v6_counter;
    }
#endif // WITH_MULTICAST
#endif // WITH_IPV6

    apply {
        if(fabric_metadata.fwd_type == FWD_BRIDGING) bridging.apply();
        else if (fabric_metadata.fwd_type == FWD_MPLS) {
            mpls.apply();

            // TODO: IPv6
            hdr.vlan_tag.ether_type = ETHERTYPE_IPV4;
            fabric_metadata.original_ether_type = ETHERTYPE_IPV4;
        }
        else if (fabric_metadata.fwd_type == FWD_IPV4_UNICAST) unicast_v4.apply();
#ifdef WITH_MULTICAST
        else if (fabric_metadata.fwd_type == FWD_IPV4_MULTICAST) multicast_v4.apply();
#endif // WITH_MULTICAST
#ifdef WITH_IPV6
        else if (fabric_metadata.fwd_type == FWD_IPV6_UNICAST) unicast_v6.apply();
#ifdef WITH_MULTICAST
        else if (fabric_metadata.fwd_type == FWD_IPV6_MULTICAST) multicast_v6.apply();
#endif // WITH_MULTICAST
#endif // WITH_IPV6
        acl.apply();
    }
}
