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

#ifndef __HEADER__
#define __HEADER__

#include "define.p4"

@controller_header("packet_in")
header packet_in_header_t {
    port_num_t ingress_port;
    bit<7> _pad;
}

_PKT_OUT_HDR_ANNOT
@controller_header("packet_out")
header packet_out_header_t {
    port_num_t egress_port;
    bit<7> _pad;
}

header ethernet_t {
    mac_addr_t dst_addr;
    mac_addr_t src_addr;
    bit<16> ether_type;
}

header vlan_tag_t {
    bit<3> pri;
    bit<1> cfi;
    vlan_id_t vlan_id;
    bit<16> ether_type;
}

header mpls_t {
    bit<20> label;
    bit<3> tc;
    bit<1> bos;
    bit<8> ttl;
}

header ipv4_t {
    bit<4> version;
    bit<4> ihl;
    bit<8> diffserv;
    bit<16> total_len;
    bit<16> identification;
    bit<3> flags;
    bit<13> frag_offset;
    bit<8> ttl;
    bit<8> protocol;
    bit<16> hdr_checksum;
    bit<32> src_addr;
    bit<32> dst_addr;
}

header ipv6_t {
    bit<4> version;
    bit<8> traffic_class;
    bit<20> flow_label;
    bit<16> payload_len;
    bit<8> next_hdr;
    bit<8> hop_limit;
    bit<128> src_addr;
    bit<128> dst_addr;
}

header arp_t {
    bit<16> hw_type;
    bit<16> proto_type;
    bit<8> hw_addr_len;
    bit<8> proto_addr_len;
    bit<16> opcode;
}

header tcp_t {
    bit<16> src_port;
    bit<16> dst_port;
    bit<32> seq_no;
    bit<32> ack_no;
    bit<4>  data_offset;
    bit<3>  res;
    bit<3>  ecn;
    bit<6>  ctrl;
    bit<16> window;
    bit<16> checksum;
    bit<16> urgent_ptr;
}

header udp_t {
    bit<16> src_port;
    bit<16> dst_port;
    bit<16> len;
    bit<16> checksum;
}

header icmp_t {
    bit<8> icmp_type;
    bit<8> icmp_code;
    bit<16> checksum;
    bit<16> identifier;
    bit<16> sequence_number;
    bit<64> timestamp;
}

#ifdef WITH_SPGW
// GTPU v1
header gtpu_t {
    bit<3>  version;    /* version */
    bit<1>  pt;         /* protocol type */
    bit<1>  spare;      /* reserved */
    bit<1>  ex_flag;    /* next extension hdr present? */
    bit<1>  seq_flag;   /* sequence no. */
    bit<1>  npdu_flag;  /* n-pdn number present ? */
    bit<8>  msgtype;    /* message type */
    bit<16> msglen;     /* message length */
    bit<32> teid;       /* tunnel endpoint id */
}

struct spgw_meta_t {
    _BOOL             do_spgw;
    direction_t       direction;
    bit<16>           ipv4_len;
    bit<32>           teid;
    bit<32>           s1u_enb_addr;
    bit<32>           s1u_sgw_addr;
#ifdef WITH_SPGW_PCC_GATING
    bit<16>           l4_src_port;
    bit<16>           l4_dst_port;
    pcc_gate_status_t pcc_gate_status;
    sdf_rule_id_t     sdf_rule_id;
    pcc_rule_id_t     pcc_rule_id;
#endif // WITH_SPGW_PCC_GATING
}
#endif // WITH_SPGW

//Custom metadata definition
struct fabric_metadata_t {
    fwd_type_t fwd_type;
    next_id_t next_id;
    _BOOL pop_vlan_when_packet_in;
    bit<8> ip_proto;
    bit<16> l4_src_port;
    bit<16> l4_dst_port;
    bit<16> original_ether_type;
#ifdef WITH_SPGW
    spgw_meta_t spgw;
#endif // WITH_SPGW
}

struct parsed_headers_t {
    ethernet_t ethernet;
    vlan_tag_t vlan_tag;
    mpls_t mpls;
#ifdef WITH_SPGW
    ipv4_t gtpu_ipv4;
    udp_t gtpu_udp;
    gtpu_t gtpu;
#endif // WITH_SPGW
    ipv4_t ipv4;
#ifdef WITH_IPV6
    ipv6_t ipv6;
#endif // WITH_IPV6
    arp_t arp;
    tcp_t tcp;
    udp_t udp;
    icmp_t icmp;
    packet_out_header_t packet_out;
    packet_in_header_t packet_in;
}

#endif
