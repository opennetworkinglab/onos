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

#include "include/headers.p4"
#include "include/defines.p4"
#include "include/parsers.p4"
#include "include/actions.p4"
#include "include/port_counters.p4"
#include "include/checksums.p4"
#include "include/packet_io.p4"
#include "include/table0.p4"

// FIXME: this program is obsolete and should be removed.
// The PI ECMP demo app should be refactored to use the WCMP capability of default.p4

// Expected number of ports of an ECMP group.
// This value is fixed, .i.e. we do not support ECMP over port groups of different
// size. Due to hardware limitations, this value must be constant and a power of 2.

#define ECMP_GROUP_SIZE 128w2

//------------------------------------------------------------------------------
// INGRESS PIPELINE
//------------------------------------------------------------------------------

control ingress(inout headers_t hdr,
                inout local_metadata_t local_metadata,
                inout standard_metadata_t standard_metadata) {

    direct_counter(CounterType.packets_and_bytes) ecmp_table_counter;

    table ecmp_table {
        key = {
            local_metadata.next_hop_id : exact;
            local_metadata.selector    : exact;
        }
        actions = {
            set_egress_port(standard_metadata);
        }
        counters = ecmp_table_counter;
    }

    action set_ecmp_selector() {
        hash(local_metadata.selector, HashAlgorithm.crc16, (bit<64>) 0,
             {
                 hdr.ipv4.src_addr,
                 hdr.ipv4.dst_addr,
                 hdr.ipv4.protocol,
                 local_metadata.l4_src_port,
                 local_metadata.l4_dst_port
             },
             ECMP_GROUP_SIZE);
    }

    apply {
        port_counters_ingress.apply(hdr, standard_metadata);
        packetio_ingress.apply(hdr, standard_metadata);
        table0_control.apply(hdr, local_metadata, standard_metadata);
        if (local_metadata.next_hop_id > 0) {
            set_ecmp_selector();
            ecmp_table.apply();
        }
     }
}

//------------------------------------------------------------------------------
// EGRESS PIPELINE
//------------------------------------------------------------------------------

control egress(inout headers_t hdr,
               inout local_metadata_t local_metadata,
               inout standard_metadata_t standard_metadata) {

    apply {
        port_counters_egress.apply(hdr, standard_metadata);
        packetio_egress.apply(hdr, standard_metadata);
    }
}

//------------------------------------------------------------------------------
// SWITCH INSTANTIATION
//------------------------------------------------------------------------------

V1Switch(parser_impl(),
         verify_checksum_control(),
         ingress(),
         egress(),
         compute_checksum_control(),
         deparser()) main;
