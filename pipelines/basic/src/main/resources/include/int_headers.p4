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
#ifndef __CUSTOM_HEADERS__
#define __CUSTOM_HEADERS__

#ifndef __INT_HEADERS__
#define __INT_HEADERS__
#include "telemetry_report_headers.p4"

// INT version 1.0

// INT header
header int_header_t {
    bit<4>  ver;
    bit<2>  rep;
    bit<1>  c;
    bit<1>  e;
    bit<1>  m;
    bit<7>  rsvd1;
    bit<3>  rsvd2;
    bit<5>  hop_metadata_len;
    bit<8>  remaining_hop_cnt;
    bit<4>  instruction_mask_0003; /* split the bits for lookup */
    bit<4>  instruction_mask_0407;
    bit<4>  instruction_mask_0811;
    bit<4>  instruction_mask_1215;
    bit<16> rsvd3;
}

// INT meta-value headers - different header for each value type
header int_switch_id_t {
    bit<32> switch_id;
}
header int_level1_port_ids_t {
    bit<16> ingress_port_id;
    bit<16> egress_port_id;
}
header int_hop_latency_t {
    bit<32> hop_latency;
}
header int_q_occupancy_t {
    bit<8> q_id;
    bit<24> q_occupancy;
}
header int_ingress_tstamp_t {
    bit<32> ingress_tstamp;
}
header int_egress_tstamp_t {
    bit<32> egress_tstamp;
}
header int_level2_port_ids_t {
    bit<32> ingress_port_id;
    bit<32> egress_port_id;
}
header int_egress_port_tx_util_t {
    bit<32> egress_port_tx_util;
}

header int_data_t {
    // Maximum int metadata stack size in bits:
    // (0x3F - 3) * 4 * 8 (excluding INT shim header and INT header)
    varbit<1920> data;
}

// INT shim header for TCP/UDP
header intl4_shim_t {
    bit<8> int_type;
    bit<8> rsvd1;
    bit<8> len;
    bit<6> dscp;
    bit<2> rsvd2;
}

struct int_metadata_t {
    switch_id_t switch_id;
    bit<16> new_bytes;
    bit<8>  new_words;
    _BOOL  source;
    _BOOL  sink;
    _BOOL  transit;
    bit<8> intl4_shim_len;
}

struct headers_t {
    packet_out_header_t packet_out;
    packet_in_header_t packet_in;
    // INT Report Encapsulation
    ethernet_t report_ethernet;
    ipv4_t report_ipv4;
    udp_t report_udp;
    // INT Report Headers
    report_fixed_header_t report_fixed_header;
    local_report_t report_local;
    // Original packet's headers
    ethernet_t ethernet;
    ipv4_t ipv4;
    tcp_t tcp;
    udp_t udp;
    // INT specific headers
    intl4_shim_t intl4_shim;
    int_header_t int_header;
    int_data_t int_data;
    int_switch_id_t int_switch_id;
    int_level1_port_ids_t int_level1_port_ids;
    int_hop_latency_t int_hop_latency;
    int_q_occupancy_t int_q_occupancy;
    int_ingress_tstamp_t int_ingress_tstamp;
    int_egress_tstamp_t int_egress_tstamp;
    int_level2_port_ids_t int_level2_port_ids;
    int_egress_port_tx_util_t int_egress_tx_util;
}

struct local_metadata_t {
    bit<16>       l4_src_port;
    bit<16>       l4_dst_port;
    next_hop_id_t next_hop_id;
    bit<16>       selector;
    int_metadata_t int_meta;
    bool compute_checksum;
}

#endif // __INT_HEADERS__
#endif // __CUSTOM_HEADERS__