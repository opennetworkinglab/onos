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

/* INT headers */
header int_header_t {
    bit<2>  ver;
    bit<2>  rep;
    bit<1>  c;
    bit<1>  e;
    bit<5>  rsvd1;
    bit<5>  ins_cnt;
    bit<8>  max_hop_cnt;
    bit<8>  total_hop_cnt;
    bit<4>  instruction_mask_0003; /* split the bits for lookup */
    bit<4>  instruction_mask_0407;
    bit<4>  instruction_mask_0811;
    bit<4>  instruction_mask_1215;
    bit<16> rsvd2;
}

// INT meta-value headers - different header for each value type
header int_switch_id_t {
    bit<32> switch_id;
}
header int_port_ids_t {
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
header int_q_congestion_t {
    bit<8> q_id;
    bit<24> q_congestion;
}
header int_egress_port_tx_util_t {
    bit<32> egress_port_tx_util;
}

header int_data_t {
    // Maximum int metadata stack size in bits:
    // (0xFF -4) * 32 (excluding INT shim header, tail header and INT header)
    varbit<8032> data;
}

/* INT shim header for TCP/UDP */
header intl4_shim_t {
    bit<8> int_type;
    bit<8> rsvd1;
    bit<8> len;
    bit<8> rsvd2;
}
/* INT tail header for TCP/UDP */
header intl4_tail_t {
    bit<8> next_proto;
    bit<16> dest_port;
    bit<8> dscp;
}

struct int_metadata_t {
    switch_id_t switch_id;
    bit<16> insert_byte_cnt;
    bit<1>  source;
    bit<1>  sink;
    bit<8>  mirror_id;
    bit<16> flow_id;
    bit<8>  metadata_len;
}

struct headers_t {
    packet_out_header_t packet_out;
    packet_in_header_t packet_in;
    ethernet_t ethernet;
    ipv4_t ipv4;
    tcp_t tcp;
    udp_t udp;

    // INT specific headers
    intl4_shim_t intl4_shim;
    int_header_t int_header;
    int_data_t int_data;
    int_switch_id_t int_switch_id;
    int_port_ids_t int_port_ids;
    int_hop_latency_t int_hop_latency;
    int_q_occupancy_t int_q_occupancy;
    int_ingress_tstamp_t int_ingress_tstamp;
    int_egress_tstamp_t int_egress_tstamp;
    int_q_congestion_t int_q_congestion;
    int_egress_port_tx_util_t int_egress_tx_util;
    intl4_tail_t intl4_tail;
}

struct local_metadata_t {
    bit<16>       l4_src_port;
    bit<16>       l4_dst_port;
    next_hop_id_t next_hop_id;
    bit<16>       selector;
    int_metadata_t int_meta;
}

#endif