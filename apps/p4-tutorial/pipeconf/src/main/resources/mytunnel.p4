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

 /*
  * This program describes a pipeline implementing a very simple
  * tunneling protocol called MyTunnel. The pipeline defines also table called
  * t_l2_fwd that provides basic L2 forwarding capabilities and actions to
  * send packets to the controller. This table is needed to provide
  * compatibility with existing ONOS applications such as Proxy-ARP, LLDP Link
  * Discovery and Reactive Forwarding.
  */

#include <core.p4>
#include <v1model.p4>

#define MAX_PORTS 255

const bit<16> ETH_TYPE_MYTUNNEL = 0x1212;
const bit<16> ETH_TYPE_IPV4 = 0x800;

typedef bit<9> port_t;
const port_t CPU_PORT = 255;

//------------------------------------------------------------------------------
// HEADERS
//------------------------------------------------------------------------------

header ethernet_t {
    bit<48> dst_addr;
    bit<48> src_addr;
    bit<16> ether_type;
}

header my_tunnel_t {
    bit<16> proto_id;
    bit<32> tun_id;
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

// Packet-in header. Prepended to packets sent to the controller and used to
// carry the original ingress port where the packet was received.
@controller_header("packet_in")
header packet_in_header_t {
    bit<9> ingress_port;
    bit<7> _padding;
}

// Packet-out header. Prepended to packets received by the controller and used
// to tell the switch on which port this packet should be forwarded.
@controller_header("packet_out")
header packet_out_header_t {
    bit<9> egress_port;
    bit<7> _padding;
}

// For convenience we collect all headers under the same struct.
struct headers_t {
    ethernet_t ethernet;
    my_tunnel_t my_tunnel;
    ipv4_t ipv4;
    packet_out_header_t packet_out;
    packet_in_header_t packet_in;
}

// Metadata can be used to carry information from one table to another.
struct metadata_t {
    // Empty. We don't use it in this program.
}

//------------------------------------------------------------------------------
// PARSER
//------------------------------------------------------------------------------

parser c_parser(packet_in packet,
                  out headers_t hdr,
                  inout metadata_t meta,
                  inout standard_metadata_t standard_metadata) {

    // A P4 parser is described as a state machine, with initial state "start"
    // and final one "accept". Each intermediate state can specify the next
    // state by using a select statement over the header fields extracted.
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
            ETH_TYPE_MYTUNNEL: parse_my_tunnel;
            ETH_TYPE_IPV4: parse_ipv4;
            default: accept;
        }
    }

    state parse_my_tunnel {
        packet.extract(hdr.my_tunnel);
        transition select(hdr.my_tunnel.proto_id) {
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

control c_ingress(inout headers_t hdr,
                    inout metadata_t meta,
                    inout standard_metadata_t standard_metadata) {

    // We use these counters to count packets/bytes received/sent on each port.
    // For each counter we instantiate a number of cells equal to MAX_PORTS.
    counter(MAX_PORTS, CounterType.packets_and_bytes) tx_port_counter;
    counter(MAX_PORTS, CounterType.packets_and_bytes) rx_port_counter;

    action send_to_cpu() {
        standard_metadata.egress_spec = CPU_PORT;
        // Packets sent to the controller needs to be prepended with the
        // packet-in header. By setting it valid we make sure it will be
        // deparsed on the wire (see c_deparser).
        hdr.packet_in.setValid();
        hdr.packet_in.ingress_port = standard_metadata.ingress_port;
    }

    action set_out_port(port_t port) {
        // Specifies the output port for this packet by setting the
        // corresponding metadata.
        standard_metadata.egress_spec = port;
    }

    action _drop() {
        mark_to_drop(standard_metadata);
    }

    action my_tunnel_ingress(bit<32> tun_id) {
        hdr.my_tunnel.setValid();
        hdr.my_tunnel.tun_id = tun_id;
        hdr.my_tunnel.proto_id = hdr.ethernet.ether_type;
        hdr.ethernet.ether_type = ETH_TYPE_MYTUNNEL;
    }

    action my_tunnel_egress(bit<9> port) {
        standard_metadata.egress_spec = port;
        hdr.ethernet.ether_type = hdr.my_tunnel.proto_id;
        hdr.my_tunnel.setInvalid();
    }

    // Table counter used to count packets and bytes matched by each entry of
    // t_l2_fwd table.
    direct_counter(CounterType.packets_and_bytes) l2_fwd_counter;

    table t_l2_fwd {
        key = {
            standard_metadata.ingress_port  : ternary;
            hdr.ethernet.dst_addr           : ternary;
            hdr.ethernet.src_addr           : ternary;
            hdr.ethernet.ether_type         : ternary;
        }
        actions = {
            set_out_port;
            send_to_cpu;
            _drop;
            NoAction;
        }
        default_action = NoAction();
        counters = l2_fwd_counter;
    }

    table t_tunnel_ingress {
        key = {
            hdr.ipv4.dst_addr: lpm;
        }
        actions = {
            my_tunnel_ingress;
            _drop;
        }
        default_action = _drop();
    }

    table t_tunnel_fwd {
        key = {
            hdr.my_tunnel.tun_id: exact;
        }
        actions = {
            set_out_port;
            my_tunnel_egress;
            _drop;
        }
        default_action = _drop();
    }

    // Defines the processing applied by this control block. You can see this as
    // the main function applied to every packet received by the switch.
    apply {
        if (standard_metadata.ingress_port == CPU_PORT) {
            // Packet received from CPU_PORT, this is a packet-out sent by the
            // controller. Skip table processing, set the egress port as
            // requested by the controller (packet_out header) and remove the
            // packet_out header.
            standard_metadata.egress_spec = hdr.packet_out.egress_port;
            hdr.packet_out.setInvalid();
        } else {
            // Packet received from data plane port.
            // Applies table t_l2_fwd to the packet.
            if (t_l2_fwd.apply().hit) {
                // Packet hit an entry in t_l2_fwd table. A forwarding action
                // has already been taken. No need to apply other tables, exit
                // this control block.
                return;
            }

            if (hdr.ipv4.isValid() && !hdr.my_tunnel.isValid()) {
                // Process only non-tunneled IPv4 packets.
                t_tunnel_ingress.apply();
            }

            if (hdr.my_tunnel.isValid()) {
                // Process all tunneled packets.
                t_tunnel_fwd.apply();
            }
        }

        // Update port counters at index = ingress or egress port.
        if (standard_metadata.egress_spec < MAX_PORTS) {
            tx_port_counter.count((bit<32>) standard_metadata.egress_spec);
        }
        if (standard_metadata.ingress_port < MAX_PORTS) {
            rx_port_counter.count((bit<32>) standard_metadata.ingress_port);
        }
     }
}

//------------------------------------------------------------------------------
// EGRESS PIPELINE
//------------------------------------------------------------------------------

control c_egress(inout headers_t hdr,
                 inout metadata_t meta,
                 inout standard_metadata_t standard_metadata) {
    apply {
        // Nothing to do on the egress pipeline.
    }
}

//------------------------------------------------------------------------------
// CHECKSUM HANDLING
//------------------------------------------------------------------------------

control c_verify_checksum(inout headers_t hdr, inout metadata_t meta) {
    apply {
        // Nothing to do here, we assume checksum is always correct.
    }
}

control c_compute_checksum(inout headers_t hdr, inout metadata_t meta) {
    apply {
        // No need to compute checksum as we do not modify packet headers.
    }
}

//------------------------------------------------------------------------------
// DEPARSER
//------------------------------------------------------------------------------

control c_deparser(packet_out packet, in headers_t hdr) {
    apply {
        // Emit headers on the wire in the following order.
        // Only valid headers are emitted.
        packet.emit(hdr.packet_in);
        packet.emit(hdr.ethernet);
        packet.emit(hdr.my_tunnel);
        packet.emit(hdr.ipv4);
    }
}

//------------------------------------------------------------------------------
// SWITCH INSTANTIATION
//------------------------------------------------------------------------------

V1Switch(c_parser(),
         c_verify_checksum(),
         c_ingress(),
         c_egress(),
         c_compute_checksum(),
         c_deparser()) main;
