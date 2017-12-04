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
#ifndef __INT_PARSER__
#define __INT_PARSER__

parser int_parser (
    packet_in packet,
    out headers_t hdr,
    inout local_metadata_t local_metadata,
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
            ETH_TYPE_IPV4 : parse_ipv4;
            default : accept;
        }
    }

    state parse_ipv4 {
        packet.extract(hdr.ipv4);
        transition select(hdr.ipv4.protocol) {
            IP_PROTO_TCP : parse_tcp;
            IP_PROTO_UDP : parse_udp;
            default: accept;
        }
    }

    state parse_tcp {
        packet.extract(hdr.tcp);
        local_metadata.l4_src_port = hdr.tcp.src_port;
        local_metadata.l4_dst_port = hdr.tcp.dst_port;
        transition select((hdr.ipv4.dscp & INT_DSCP) == INT_DSCP) {
            true: parse_intl4_shim;
            default: accept;
        }
    }

    state parse_udp {
        packet.extract(hdr.udp);
        local_metadata.l4_src_port = hdr.udp.src_port;
        local_metadata.l4_dst_port = hdr.udp.dst_port;
        transition select((hdr.ipv4.dscp & INT_DSCP) == INT_DSCP) {
            true: parse_intl4_shim;
            default: accept;
        }
    }

    state parse_intl4_shim {
        packet.extract(hdr.intl4_shim);
        transition parse_int_header;
    }

    state parse_int_header {
        packet.extract(hdr.int_header);
        // If there is no INT metadata but the INT header (and corresponding shim header
        // and tail header) exists, default value of length field in shim header
        // should be INT_HEADER_LEN_WORD.
        local_metadata.int_meta.metadata_len = hdr.intl4_shim.len - INT_HEADER_LEN_WORD;
        transition select (local_metadata.int_meta.metadata_len) {
            0: parse_intl4_tail;
            default: parse_int_data;
        }
    }

    state parse_int_data {
        // Parse INT metadata, not INT header, INT shim header and INT tail header
        packet.extract(hdr.int_data, (bit<32>) ((hdr.intl4_shim.len - INT_HEADER_LEN_WORD) << 5));
        transition parse_intl4_tail;
    }

    state parse_intl4_tail {
        packet.extract(hdr.intl4_tail);
        transition accept;
    }
}

control int_deparser(
    packet_out packet,
    in headers_t hdr) {
    apply {
        packet.emit(hdr.packet_in);
        packet.emit(hdr.ethernet);
        packet.emit(hdr.ipv4);
        packet.emit(hdr.tcp);
        packet.emit(hdr.udp);
        packet.emit(hdr.intl4_shim);
        packet.emit(hdr.int_header);
        packet.emit(hdr.int_switch_id);
        packet.emit(hdr.int_port_ids);
        packet.emit(hdr.int_hop_latency);
        packet.emit(hdr.int_q_occupancy);
        packet.emit(hdr.int_ingress_tstamp);
        packet.emit(hdr.int_egress_tstamp);
        packet.emit(hdr.int_q_congestion);
        packet.emit(hdr.int_egress_tx_util);
        packet.emit(hdr.int_data);
        packet.emit(hdr.intl4_tail);
    }
}

#endif