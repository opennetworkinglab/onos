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
 *
 */

#define S1U_PORT 1
#define SGI_PORT 2
#define AS_MAC_ADDR  0x52540029c9b7
#define S1U_MAC_ADDR 0xc242592d3a84
#define SGI_MAC_ADDR 0x3ac1e253e150
#define ENB_MAC_ADDR 0x525400057b59

#include <core.p4>
#include <v1model.p4>

#include "include/define.p4"
#include "include/header.p4"
#include "include/checksum.p4"
#include "include/parser.p4"
#include "include/control/packetio.p4"

#include "include/spgw.p4"

control table0_control(inout parsed_headers_t hdr,
                       inout fabric_metadata_t fabric_metadata,
                       inout standard_metadata_t standard_metadata) {

    action send_to_cpu() {
        standard_metadata.egress_spec = CPU_PORT;
        exit;
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
            fabric_metadata.l4_src_port     : ternary;
            fabric_metadata.l4_dst_port     : ternary;
        }
        actions = {
            send_to_cpu();
            NoAction();
        }
        const default_action = NoAction();
    }

    apply {
        table0.apply();
    }
}

//------------------------------------------------------------------------------
// INGRESS PIPELINE
//------------------------------------------------------------------------------

control ingress_impl(inout parsed_headers_t hdr,
                     inout fabric_metadata_t fabric_metadata,
                     inout standard_metadata_t standard_metadata) {

    apply {

        PacketIoIngress.apply(hdr, fabric_metadata, standard_metadata);

        // Drop garbage, so we get a clean pcap.
        if (standard_metadata.ingress_port == S1U_PORT
                && hdr.ethernet.src_addr != ENB_MAC_ADDR) {
            mark_to_drop();
            exit;
        } else if (standard_metadata.ingress_port == SGI_PORT
                && hdr.ethernet.src_addr != AS_MAC_ADDR) {
            mark_to_drop();
            exit;
        }

        table0_control.apply(hdr, fabric_metadata, standard_metadata);

        if (standard_metadata.egress_spec == CPU_PORT) {
            return;
        }

        if (standard_metadata.ingress_port == S1U_PORT) {
            if (hdr.ethernet.dst_addr != S1U_MAC_ADDR) {
                mark_to_drop();
                exit;
            }
            standard_metadata.egress_spec = SGI_PORT;
            hdr.ethernet.src_addr = SGI_MAC_ADDR;
            hdr.ethernet.dst_addr = AS_MAC_ADDR;
        } else if (standard_metadata.ingress_port == SGI_PORT) {
            if (hdr.ethernet.dst_addr != SGI_MAC_ADDR) {
                mark_to_drop();
                exit;
            }
            standard_metadata.egress_spec = S1U_PORT;
            hdr.ethernet.src_addr = S1U_MAC_ADDR;
            hdr.ethernet.dst_addr = ENB_MAC_ADDR;
        }

#ifdef WITH_SPGW_PCC_GATING
        fabric_metadata.spgw.l4_src_port = fabric_metadata.l4_src_port;
        fabric_metadata.spgw.l4_dst_port = fabric_metadata.l4_dst_port;
#endif // WITH_SPGW_PCC_GATING
        spgw_ingress.apply(hdr.gtpu_ipv4, hdr.gtpu_udp, hdr.gtpu,
                           hdr.ipv4, hdr.udp, fabric_metadata.spgw);
    }
}

//------------------------------------------------------------------------------
// EGRESS PIPELINE
//------------------------------------------------------------------------------

control egress_impl(inout parsed_headers_t hdr,
                    inout fabric_metadata_t fabric_metadata,
                    inout standard_metadata_t standard_metadata) {
    apply {
        PacketIoEgress.apply(hdr, fabric_metadata, standard_metadata);
        spgw_egress.apply(hdr.gtpu_ipv4, hdr.gtpu_udp, hdr.gtpu,
                          fabric_metadata.spgw, standard_metadata);
     }
}

//------------------------------------------------------------------------------
// SWITCH INSTANTIATION
//------------------------------------------------------------------------------

V1Switch(FabricParser(),
         FabricVerifyChecksum(),
         ingress_impl(),
         egress_impl(),
         FabricComputeChecksum(),
         FabricDeparser()) main;
