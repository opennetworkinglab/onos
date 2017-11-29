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
    direct_counter(CounterType.packets_and_bytes) multicast_v4_counter;
    direct_counter(CounterType.packets_and_bytes) unicast_v6_counter;
    direct_counter(CounterType.packets_and_bytes) multicast_v6_counter;
    direct_counter(CounterType.packets_and_bytes) acl_counter;

    action set_next_id(next_id_t next_id) {
        fabric_metadata.next_id = next_id;
    }

    action pop_mpls_and_next(next_id_t next_id) {
        hdr.mpls.setInvalid();
        if (hdr.ipv4.isValid()) {
            hdr.ethernet.ether_type = ETHERTYPE_IPV4;
        } else {
            hdr.ethernet.ether_type = ETHERTYPE_IPV6;
        }
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

    table unicast_v6 {
        key = {
            hdr.ipv6.dst_addr: lpm;
        }

        actions = {
            set_next_id;
        }
        counters = unicast_v6_counter;
    }

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

    table acl {
        key = {
            standard_metadata.ingress_port: ternary;
            fabric_metadata.ip_proto: ternary;
            hdr.ethernet.dst_addr: ternary;
            hdr.ethernet.src_addr: ternary;
            hdr.ethernet.ether_type: ternary;
            hdr.vlan_tag.vlan_id: ternary;
            hdr.vlan_tag.pri: ternary;
            hdr.mpls.tc: ternary;
            hdr.mpls.bos: ternary;
            hdr.mpls.label: ternary;
            hdr.ipv4.src_addr: ternary;
            hdr.ipv4.dst_addr: ternary;
            hdr.ipv4.protocol: ternary;
            hdr.ipv6.src_addr: ternary;
            hdr.ipv6.dst_addr: ternary;
            hdr.ipv6.next_hdr: ternary;
            hdr.tcp.src_port: ternary;
            hdr.tcp.dst_port: ternary;
            hdr.udp.src_port: ternary;
            hdr.udp.dst_port: ternary;
            hdr.icmp.icmp_type: ternary;
            hdr.icmp.icmp_code: ternary;
        }

        actions = {
            set_next_id;
            duplicate_to_controller;
            drop;
            nop;
        }

        const default_action = nop();
        counters = acl_counter;
    }

    apply {
        if(fabric_metadata.fwd_type == FWD_BRIDGING) bridging.apply();
        else if (fabric_metadata.fwd_type == FWD_MPLS) mpls.apply();
        else if (fabric_metadata.fwd_type == FWD_IPV4_UNICAST) unicast_v4.apply();
        else if (fabric_metadata.fwd_type == FWD_IPV4_MULTICAST) multicast_v4.apply();
        else if (fabric_metadata.fwd_type == FWD_IPV6_UNICAST) unicast_v6.apply();
        else if (fabric_metadata.fwd_type == FWD_IPV6_MULTICAST) multicast_v6.apply();
        acl.apply();
    }
}
