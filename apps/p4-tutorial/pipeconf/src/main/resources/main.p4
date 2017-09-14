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

#define ETH_TYPE_IPV4 0x0800
#define MAX_PORTS 511

typedef bit<9> port_t;
const port_t CPU_PORT = 255;
const port_t DROP_PORT = 511;

//------------------------------------------------------------------------------
// HEADERS
//------------------------------------------------------------------------------

header ethernet_t {
    bit<48> dst_addr;
    bit<48> src_addr;
    bit<16> ether_type;
}

header ipv4_t {
    bit<4>  version;
    bit<4>  ihl;
    bit<8>  diffserv;
    bit<16> len;
    bit<16> identification;
    bit<3>  flags;
    bit<13> frag_offset;
    bit<8>  ttl;
    bit<8>  protocol;
    bit<16> hdr_checksum;
    bit<32> src_addr;
    bit<32> dst_addr;
}

/*
Packet-in header. Prepended to packets sent to the controller and used to carry
the original ingress port where the packet was received.
 */
@controller_header("packet_in")
header packet_in_header_t {
    bit<9> ingress_port;
}

/*
Packet-out header. Prepended to packets received by the controller and used to
tell the switch on which physical port this packet should be forwarded.
 */
@controller_header("packet_out")
header packet_out_header_t {
    bit<9> egress_port;
}

/*
For convenience we collect all headers under the same struct.
 */
struct headers_t {
    ethernet_t ethernet;
    ipv4_t ipv4;
    packet_out_header_t packet_out;
    packet_in_header_t packet_in;
}

/*
Metadata can be used to carry information from one table to another.
 */
struct metadata_t {
    /* Empty. We don't use it in this program. */
}

//------------------------------------------------------------------------------
// PARSER
//------------------------------------------------------------------------------

parser ParserImpl(packet_in packet,
                  out headers_t hdr,
                  inout metadata_t meta,
                  inout standard_metadata_t standard_metadata) {

    state start {
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
        transition select(hdr.ethernet.ether_type) {
            ETH_TYPE_IPV4: parse_ipv4;
            default: accept;
        }
    }

    state parse_ipv4 {
        packet.extract(hdr.ipv4);
        transition accept;
    }
}

//------------------------------------------------------------------------------
// INGRESS PIPELINE
//------------------------------------------------------------------------------

control IngressImpl(inout headers_t hdr,
                    inout metadata_t meta,
                    inout standard_metadata_t standard_metadata) {

    action send_to_cpu() {
        standard_metadata.egress_spec = CPU_PORT;
        /*
        Packets sent to the controller needs to be prepended with the packet-in
        header. By setting it valid we make sure it will be deparsed before the
        ethernet header (see DeparserImpl).
         */
        hdr.packet_in.setValid();
        hdr.packet_in.ingress_port = standard_metadata.ingress_port;
    }

    action set_egress_port(port_t port) {
        standard_metadata.egress_spec = port;
    }

    action _drop() {
        standard_metadata.egress_spec = DROP_PORT;
    }

    table table0 {
        key = {
            standard_metadata.ingress_port  : ternary;
            hdr.ethernet.dst_addr           : ternary;
            hdr.ethernet.src_addr           : ternary;
            hdr.ethernet.ether_type         : ternary;
        }
        actions = {
            set_egress_port();
            send_to_cpu();
            _drop();
        }
        default_action = _drop();
    }

    table ip_proto_filter_table {
        key = {
            hdr.ipv4.src_addr : ternary;
            hdr.ipv4.protocol : exact;
        }
        actions = {
            _drop();
        }
    }

    /*
    Port counters.
    We use these counter instances to count packets/bytes received/sent on each
    port. BMv2 always counts both packets and bytes, even if the counter is
    instantiated as "packets". For each counter we instantiate a number of cells
    equal to MAX_PORTS.
     */
    counter(MAX_PORTS, CounterType.packets) egr_port_counter;
    counter(MAX_PORTS, CounterType.packets) igr_port_counter;

    /*
    We define here the processing to be executed by this ingress pipeline.
     */
    apply {
        if (standard_metadata.ingress_port == CPU_PORT) {
            /*
            Packet received from CPU_PORT, this is a packet-out sent by the
            controller. Skip pipeline processing, set the egress port as
            requested by the controller (packet_out header) and remove the
            packet_out header.
             */
            standard_metadata.egress_spec = hdr.packet_out.egress_port;
            hdr.packet_out.setInvalid();
        } else {
            /*
            Packet received from switch port. Apply table0, if action is
            set_egress_port and packet is IPv4, then apply
            ip_proto_filter_table.
             */
            switch(table0.apply().action_run) {
                set_egress_port: {
                    if (hdr.ipv4.isValid()) {
                        ip_proto_filter_table.apply();
                    }
                }
            }
        }

        /*
        For each port counter, we update the cell at index = ingress/egress
        port. We avoid counting packets sent/received on CPU_PORT or dropped
        (DROP_PORT).
         */
        if (standard_metadata.egress_spec < MAX_PORTS) {
            egr_port_counter.count((bit<32>) standard_metadata.egress_spec);
        }
        if (standard_metadata.ingress_port < MAX_PORTS) {
            igr_port_counter.count((bit<32>) standard_metadata.ingress_port);
        }
     }
}

//------------------------------------------------------------------------------
// EGRESS PIPELINE
//------------------------------------------------------------------------------

control EgressImpl(inout headers_t hdr,
                   inout metadata_t meta,
                   inout standard_metadata_t standard_metadata) {
    apply {
        /*
        Nothing to do on the egress pipeline.
        */
    }
}

//------------------------------------------------------------------------------
// CHECKSUM HANDLING
//------------------------------------------------------------------------------

control VerifyChecksumImpl(in headers_t hdr, inout metadata_t meta) {
    apply {
        /*
        Nothing to do here, we assume checksum is always correct.
        */
    }
}

control ComputeChecksumImpl(inout headers_t hdr, inout metadata_t meta) {
    apply {
        /*
        Nothing to do here, as we do not modify packet headers.
        */
    }
}

//------------------------------------------------------------------------------
// DEPARSER
//------------------------------------------------------------------------------

control DeparserImpl(packet_out packet, in headers_t hdr) {
    apply {
        /*
        Deparse headers in order. Only valid headers are emitted.
        */
        packet.emit(hdr.packet_in);
        packet.emit(hdr.ethernet);
        packet.emit(hdr.ipv4);
    }
}

//------------------------------------------------------------------------------
// SWITCH INSTANTIATION
//------------------------------------------------------------------------------

V1Switch(ParserImpl(),
         VerifyChecksumImpl(),
         IngressImpl(),
         EgressImpl(),
         ComputeChecksumImpl(),
         DeparserImpl()) main;
