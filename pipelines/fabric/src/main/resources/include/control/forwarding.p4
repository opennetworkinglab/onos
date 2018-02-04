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

    direct_counter(CounterType.packets_and_bytes) bridging_counter;
    direct_counter(CounterType.packets_and_bytes) mpls_counter;
    direct_counter(CounterType.packets_and_bytes) unicast_v4_counter;
    direct_counter(CounterType.packets_and_bytes) acl_counter;

    action drop() {
        mark_to_drop();
    }

    action set_next_id(next_id_t next_id) {
        fabric_metadata.next_id = next_id;
    }

    action pop_mpls_and_next(next_id_t next_id) {
        hdr.mpls.setInvalid();
        fabric_metadata.next_id = next_id;
    }

    action duplicate_to_controller() {
        standard_metadata.egress_spec = CPU_PORT;
    }

    table bridging {
        key = {
            hdr.vlan_tag.vlan_id: exact;
            hdr.ethernet.dst_addr: ternary;
        }

        actions = {
            set_next_id;
        }
        counters = bridging_counter;
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

    table unicast_v4 {
        key = {
            hdr.ipv4.dst_addr: lpm;
        }

        actions = {
            set_next_id;
        }
        counters = unicast_v4_counter;
    }

#ifdef WITH_MULTICAST
    direct_counter(CounterType.packets_and_bytes) multicast_v4_counter;

    table multicast_v4 {
        key = {
            hdr.vlan_tag.vlan_id: exact;
            hdr.ipv4.dst_addr: lpm;
        }

        actions = {
            set_next_id;
        }
        counters = multicast_v4_counter;
    }
#endif // WITH_MULTICAST

#ifdef WITH_IPV6
    direct_counter(CounterType.packets_and_bytes) unicast_v6_counter;

    table unicast_v6 {
        key = {
            hdr.ipv6.dst_addr: lpm;
        }

        actions = {
            set_next_id;
        }
        counters = unicast_v6_counter;
    }

#ifdef WITH_MULTICAST
    direct_counter(CounterType.packets_and_bytes) multicast_v6_counter;

    table multicast_v6 {
        key = {
            hdr.vlan_tag.vlan_id: exact;
            hdr.ipv6.dst_addr: lpm;
        }

        actions = {
            set_next_id;
        }
        counters = multicast_v6_counter;
    }
#endif // WITH_MULTICAST
#endif // WITH_IPV6

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
            set_next_id;
            duplicate_to_controller;
            drop;
            nop;
        }

        const default_action = nop();
        size = 256;
        counters = acl_counter;
    }

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
