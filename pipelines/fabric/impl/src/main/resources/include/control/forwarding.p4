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


control Forwarding (inout parsed_headers_t hdr,
                    inout fabric_metadata_t fabric_metadata,
                    inout standard_metadata_t standard_metadata) {

    @hidden
    action set_next_id(next_id_t next_id) {
        fabric_metadata.next_id = next_id;
    }

    /*
     * Bridging Table.
     */
    direct_counter(CounterType.packets_and_bytes) bridging_counter;

    action set_next_id_bridging(next_id_t next_id) {
        set_next_id(next_id);
        bridging_counter.count();
    }

    // FIXME: using ternary for eth_dst prevents our ability to scale in
    //  bridging heavy environments. Do we really need ternary? Can we come up
    //  with a multi-table/algorithmic approach?
    table bridging {
        key = {
            fabric_metadata.vlan_id: exact @name("vlan_id");
            hdr.ethernet.dst_addr: ternary @name("eth_dst");
        }
        actions = {
            set_next_id_bridging;
            @defaultonly nop;
        }
        const default_action = nop();
        counters = bridging_counter;
        size = BRIDGING_TABLE_SIZE;
    }

    /*
     * MPLS Table.
     */
    direct_counter(CounterType.packets_and_bytes) mpls_counter;

    action pop_mpls_and_next(next_id_t next_id) {
        fabric_metadata.mpls_label = 0;
        set_next_id(next_id);
        mpls_counter.count();
    }

    table mpls {
        key = {
            fabric_metadata.mpls_label: exact @name("mpls_label");
        }
        actions = {
            pop_mpls_and_next;
            @defaultonly nop;
        }
        const default_action = nop();
        counters = mpls_counter;
        size = MPLS_TABLE_SIZE;
    }

    /*
     * IPv4 Routing Table.
     */
#ifdef WTIH_DEBUG
    direct_counter(CounterType.packets_and_bytes) routing_v4_counter;
#endif // WITH_DEBUG

    action set_next_id_routing_v4(next_id_t next_id) {
        set_next_id(next_id);
#ifdef WTIH_DEBUG
        routing_v4_counter.count();
#endif // WITH_DEBUG
    }

    action nop_routing_v4() {
        // no-op
#ifdef WTIH_DEBUG
        routing_v4_counter.count();
#endif // WITH_DEBUG
    }

    #ifdef _ROUTING_V4_TABLE_ANNOT
    _ROUTING_V4_TABLE_ANNOT
    #endif
    table routing_v4 {
        key = {
            fabric_metadata.ipv4_dst_addr: lpm @name("ipv4_dst");
        }
        actions = {
            set_next_id_routing_v4;
            nop_routing_v4;
            @defaultonly nop;
        }
        default_action = nop();
#ifdef WTIH_DEBUG
        counters = routing_v4_counter;
#endif // WITH_DEBUG
        size = ROUTING_V4_TABLE_SIZE;
    }

#ifdef WITH_IPV6
    /*
     * IPv6 Routing Table.
     */
    direct_counter(CounterType.packets_and_bytes) routing_v6_counter;

    action set_next_id_routing_v6(next_id_t next_id) {
        set_next_id(next_id);
        routing_v6_counter.count();
    }

    table routing_v6 {
        key = {
            hdr.ipv6.dst_addr: lpm @name("ipv6_dst");
        }
        actions = {
            set_next_id_routing_v6;
            @defaultonly nop;
        }
        const default_action = nop();
        counters = routing_v6_counter;
        size = ROUTING_V6_TABLE_SIZE;
    }
#endif // WITH_IPV6

    apply {
        if (fabric_metadata.fwd_type == FWD_BRIDGING) bridging.apply();
        else if (fabric_metadata.fwd_type == FWD_MPLS) mpls.apply();
        else if (fabric_metadata.fwd_type == FWD_IPV4_UNICAST) routing_v4.apply();
#ifdef WITH_IPV6
        else if (fabric_metadata.fwd_type == FWD_IPV6_UNICAST) routing_v6.apply();
#endif // WITH_IPV6
    }
}
