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

#ifndef __TABLE0__
#define __TABLE0__

#include "headers.p4"
#include "defines.p4"

control table0_control(inout headers_t hdr,
                       inout local_metadata_t local_metadata,
                       inout standard_metadata_t standard_metadata) {

    direct_counter(CounterType.packets_and_bytes) table0_counter;

    action set_next_hop_id(next_hop_id_t next_hop_id) {
        local_metadata.next_hop_id = next_hop_id;
    }

    action send_to_cpu() {
        standard_metadata.egress_spec = CPU_PORT;
    }

    action set_egress_port(port_t port) {
        standard_metadata.egress_spec = port;
    }

    action drop() {
        mark_to_drop(standard_metadata);
    }

    table table0 {
        key = {
            standard_metadata.ingress_port : ternary;
            hdr.ethernet.src_addr          : ternary;
            hdr.ethernet.dst_addr          : ternary;
            hdr.ethernet.ether_type        : ternary;
            hdr.ipv4.src_addr              : ternary;
            hdr.ipv4.dst_addr              : ternary;
            hdr.ipv4.protocol              : ternary;
            local_metadata.l4_src_port     : ternary;
            local_metadata.l4_dst_port     : ternary;
        }
        actions = {
            set_egress_port;
            send_to_cpu;
            set_next_hop_id;
            drop;
        }
        const default_action = drop();
        counters = table0_counter;
    }

    apply {
        table0.apply();
     }
}

#endif