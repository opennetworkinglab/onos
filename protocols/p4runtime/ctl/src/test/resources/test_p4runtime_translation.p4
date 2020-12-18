/*
 * Copyright 2020-present Open Networking Foundation
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

typedef bit<9> port_t;

@p4runtime_translation("", string)
type bit<48> mac_addr_t;

@p4runtime_translation("", 32)
type port_t port_id_bit_t;

@p4runtime_translation("", string)
type port_t port_id_str_t;

const port_t CPU_PORT = 255;

@controller_header("packet_in")
header packet_in_header_t {
    bit<9> ingress_port;
    bit<7> _padding;
}

@controller_header("packet_out")
header packet_out_header_t {
    bit<9> egress_port;
    bit<7> _padding;
}

header ethernet_t {
    mac_addr_t dstAddr;
    mac_addr_t srcAddr;
    bit<16> etherType;
}

struct headers_t {
    packet_out_header_t packet_out;
    packet_in_header_t packet_in;
    ethernet_t ethernet;
}

struct local_metadata_t {
    port_id_bit_t ingress_port;
}

// Test P4 Program for P4Runtime translation (simplified version of basic.p4).

//------------------------------------------------------------------------------
// PARSER
//------------------------------------------------------------------------------
parser parser_impl(packet_in packet,
                  out headers_t hdr,
                  inout local_metadata_t local_metadata,
                  inout standard_metadata_t standard_metadata) {

    state start {
        local_metadata.ingress_port = (port_id_bit_t) standard_metadata.ingress_port;
        transition select(standard_metadata.ingress_port) {
            CPU_PORT: parse_packet_out;
            default: parse_ethernet;
        }
    }

    state parse_packet_out {
        packet.extract(hdr.packet_out);
        transition parse_ethernet;
    }

    state parse_ethernet {
        packet.extract(hdr.ethernet);
        transition accept;
    }
}

//------------------------------------------------------------------------------
// DEPARSER
//------------------------------------------------------------------------------
control deparser(packet_out packet, in headers_t hdr) {
    apply {
        packet.emit(hdr.packet_in);
        packet.emit(hdr.ethernet);
    }
}


//------------------------------------------------------------------------------
// INGRESS PIPELINE
//------------------------------------------------------------------------------

control ingress(inout headers_t hdr,
                inout local_metadata_t local_metadata,
                inout standard_metadata_t standard_metadata) {

    @name(".send_to_cpu")
    action send_to_cpu() {
        standard_metadata.egress_spec = CPU_PORT;
    }

    @name(".set_egress_port")
    action set_egress_port(port_id_str_t port) {
        standard_metadata.egress_spec = (bit<9>) port;
    }
    @name(".set_egress_port2")
    action set_egress_port2(port_id_bit_t port) {
        standard_metadata.egress_spec = (bit<9>) port;
    }

    @name(".drop")
    action drop() {
        mark_to_drop(standard_metadata);
    }
    @name(".table0")
    table table0 {
        key = {
            local_metadata.ingress_port : exact;
            hdr.ethernet.srcAddr        : exact;
            hdr.ethernet.dstAddr        : exact;
            hdr.ethernet.etherType      : optional;
        }
        actions = {
            set_egress_port;
            set_egress_port2;
            send_to_cpu;
            drop;
        }
        const default_action = drop();
    }

    apply {
        table0.apply();
     }
}

//------------------------------------------------------------------------------
// EGRESS PIPELINE
//------------------------------------------------------------------------------

control egress(inout headers_t hdr,
               inout local_metadata_t local_metadata,
               inout standard_metadata_t standard_metadata) {

    apply {
    // no-op
    }
}

control verify_checksum_control(inout headers_t hdr,
                                inout local_metadata_t local_metadata) {
    apply {
        // Assume checksum is always correct.
    }
}

control compute_checksum_control(inout headers_t hdr,
                                 inout local_metadata_t local_metadata) {
    apply {
    // no-op for the test program
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
