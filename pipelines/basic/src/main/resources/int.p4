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

/* -*- P4_16 -*- */
#include <core.p4>
#include <v1model.p4>

#include "include/defines.p4"
#include "include/headers.p4"
#include "include/actions.p4"
#include "include/int_definitions.p4"
#include "include/int_headers.p4"
#include "include/packet_io.p4"
#include "include/port_counters.p4"
#include "include/table0.p4"
#include "include/checksums.p4"
#include "include/int_parser.p4"
#include "include/int_source.p4"
#include "include/int_transit.p4"
#include "include/int_sink.p4"

control ingress (
    inout headers_t hdr,
    inout local_metadata_t local_metadata,
    inout standard_metadata_t standard_metadata) {

    apply {
        port_counters_ingress.apply(hdr, standard_metadata);
        packetio_ingress.apply(hdr, standard_metadata);
        table0_control.apply(hdr, local_metadata, standard_metadata);
    }
}

control egress (
    inout headers_t hdr,
    inout local_metadata_t local_metadata,
    inout standard_metadata_t standard_metadata) {

    apply {
        if (standard_metadata.ingress_port != CPU_PORT &&
            standard_metadata.egress_port != CPU_PORT &&
            (hdr.udp.isValid() || hdr.tcp.isValid())) {
            process_set_source_sink.apply(hdr, local_metadata, standard_metadata);
            if (local_metadata.int_meta.source == 1) {
                process_int_source.apply(hdr, local_metadata, standard_metadata);
            }
            if(hdr.int_header.isValid()) {
                process_int_transit.apply(hdr, local_metadata, standard_metadata);
                // update underlay header based on INT information inserted
                process_int_outer_encap.apply(hdr, local_metadata, standard_metadata);
                if (local_metadata.int_meta.sink == 1) {
                    // int sink
                    process_int_sink.apply(hdr, local_metadata, standard_metadata);
                }
            }
        }
        port_counters_egress.apply(hdr, standard_metadata);
        packetio_egress.apply(hdr, standard_metadata);
    }
}

V1Switch(
    int_parser(),
    verify_checksum_control(),
    ingress(),
    egress(),
    compute_checksum_control(),
    int_deparser()
) main;