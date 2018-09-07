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

#include "include/control/filtering.p4"
#include "include/control/forwarding.p4"
#include "include/control/next.p4"
#include "include/control/packetio.p4"
#include "include/header.p4"
#include "include/checksum.p4"
#include "include/parser.p4"

#ifdef WITH_PORT_COUNTER
#include "include/control/port_counter.p4"
#endif // WITH_PORT_COUNTER

#ifdef WITH_SPGW
#include "include/spgw.p4"
#endif // WITH_SPGW

#ifdef WITH_INT
#include "include/int_source.p4"
#include "include/int_transit.p4"
#include "include/int_sink.p4"
#include "include/int_report.p4"
#endif // WITH_INT

control FabricIngress (
inout parsed_headers_t hdr,
inout fabric_metadata_t fabric_metadata,
inout standard_metadata_t standard_metadata) {
    PacketIoIngress() pkt_io_ingress;
    Filtering() filtering;
    Forwarding() forwarding;
    Next() next;
#ifdef WITH_PORT_COUNTER
    PortCountersControl() port_counters_control;
#endif // WITH_PORT_COUNTER

    apply {
#ifdef WITH_SPGW
        spgw_normalizer.apply(hdr.gtpu.isValid(), hdr.gtpu_ipv4, hdr.gtpu_udp,
                              hdr.ipv4, hdr.udp, hdr.inner_ipv4, hdr.inner_udp);
#endif // WITH_SPGW
        pkt_io_ingress.apply(hdr, fabric_metadata, standard_metadata);
#ifdef WITH_SPGW
#ifdef WITH_SPGW_PCC_GATING
        fabric_metadata.spgw.l4_src_port = fabric_metadata.l4_src_port;
        fabric_metadata.spgw.l4_dst_port = fabric_metadata.l4_dst_port;
#endif // WITH_SPGW_PCC_GATING
        spgw_ingress.apply(hdr.gtpu_ipv4, hdr.gtpu_udp, hdr.gtpu,
                           hdr.ipv4, hdr.udp, fabric_metadata.spgw);
#endif // WITH_SPGW
        filtering.apply(hdr, fabric_metadata, standard_metadata);
        forwarding.apply(hdr, fabric_metadata, standard_metadata);
        next.apply(hdr, fabric_metadata, standard_metadata);
#ifdef WITH_PORT_COUNTER
        port_counters_control.apply(hdr, fabric_metadata, standard_metadata);
#endif // WITH_PORT_COUNTER
#ifdef WITH_INT
        process_set_source_sink.apply(hdr, fabric_metadata, standard_metadata);
        if(fabric_metadata.int_meta.sink == 1) {
            // clone packet for Telemetry Report
            #ifdef __TARGET_BMV2__
            clone(CloneType.I2E, REPORT_MIRROR_SESSION_ID);
            #endif
        }
#endif
    }
}

control FabricEgress (inout parsed_headers_t hdr,
                      inout fabric_metadata_t fabric_metadata,
                      inout standard_metadata_t standard_metadata) {
    PacketIoEgress() pkt_io_egress;
    EgressNextControl() egress_next;

    apply {
        pkt_io_egress.apply(hdr, fabric_metadata, standard_metadata);
        egress_next.apply(hdr, fabric_metadata, standard_metadata);
#ifdef WITH_SPGW
        spgw_egress.apply(hdr.ipv4, hdr.gtpu_ipv4, hdr.gtpu_udp, hdr.gtpu,
                          fabric_metadata.spgw, standard_metadata);
#endif // WITH_SPGW
#ifdef WITH_INT
        if (standard_metadata.ingress_port != CPU_PORT &&
            standard_metadata.egress_port != CPU_PORT &&
            (hdr.udp.isValid() || hdr.tcp.isValid())) {
            if (fabric_metadata.int_meta.source == 1) {
                process_int_source.apply(hdr, fabric_metadata, standard_metadata);
            }
            if(hdr.int_header.isValid()) {
                process_int_transit.apply(hdr, fabric_metadata, standard_metadata);
                // update underlay header based on INT information inserted
                process_int_outer_encap.apply(hdr, fabric_metadata, standard_metadata);
                if (standard_metadata.instance_type == PKT_INSTANCE_TYPE_INGRESS_CLONE) {
                    /* send int report */
                    process_int_report.apply(hdr, fabric_metadata, standard_metadata);
                }
                if (fabric_metadata.int_meta.sink == 1) {
                    // int sink
                    process_int_sink.apply(hdr, fabric_metadata, standard_metadata);
                }
            }
        }
#endif
    }
}

V1Switch(
    FabricParser(),
    FabricVerifyChecksum(),
    FabricIngress(),
    FabricEgress(),
    FabricComputeChecksum(),
    FabricDeparser()
) main;
