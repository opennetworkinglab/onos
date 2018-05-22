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

#ifndef __WCMP__
#define __WCMP__

#include "headers.p4"
#include "defines.p4"

control wcmp_control(inout headers_t hdr,
                     inout local_metadata_t local_metadata,
                     inout standard_metadata_t standard_metadata) {

    direct_counter(CounterType.packets_and_bytes) wcmp_table_counter;
    action_selector(HashAlgorithm.crc16, 32w64, 32w16) wcmp_selector;

    action set_egress_port(port_t port) {
        standard_metadata.egress_spec = port;
    }

    table wcmp_table {
        support_timeout = false;
        key = {
            local_metadata.next_hop_id : exact;
            hdr.ipv4.src_addr          : selector;
            hdr.ipv4.dst_addr          : selector;
            hdr.ipv4.protocol          : selector;
            local_metadata.l4_src_port : selector;
            local_metadata.l4_dst_port : selector;
        }
        actions = {
            set_egress_port;
        }
        implementation = wcmp_selector;
        counters = wcmp_table_counter;
    }

    apply {
        if (local_metadata.next_hop_id != 0) {
            wcmp_table.apply();
        }
    }
}

#endif