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
#include "int/int_header.p4"

@controller_header("packet_in")
header packet_in_header_t {
    port_num_t ingress_port;
    bit<7> _pad;
}

_PKT_OUT_HDR_ANNOT
@controller_header("packet_out")
header packet_out_header_t {
    port_num_t egress_port;
    bit<1> do_forwarding;
    bit<6> _pad;
}

header ethernet_t {
    mac_addr_t dst_addr;
    mac_addr_t src_addr;
}

// NOTE: splitting the eth_type from the ethernet header helps to match on
//  the actual eth_type without checking validity bit of the VLAN tags.
header eth_type_t {
    bit<16> value;
}

header vlan_tag_t {
    bit<16> eth_type;
    bit<3> pri;
    bit<1> cfi;
    vlan_id_t vlan_id;
}

header mpls_t {
    bit<20> label;
    bit<3> tc;
    bit<1> bos;
    bit<8> ttl;
}

header pppoe_t {
    bit<4>  version;
    bit<4>  type_id;
    bit<8>  code;
    bit<16> session_id;
    bit<16> length;
    bit<16> protocol;
}

header ipv4_t {
    bit<4> version;
    bit<4> ihl;
    bit<6> dscp;
    bit<2> ecn;
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

header tcp_t {
    bit<16> sport;
    bit<16> dport;
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
    bit<16> sport;
    bit<16> dport;
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
    teid_t  teid;       /* tunnel endpoint id */
}

// Follows gtpu_t if any of ex_flag, seq_flag, or npdu_flag is 1.
header gtpu_options_t {
    bit<16> seq_num;   /* Sequence number */
    bit<8>  n_pdu_num; /* N-PDU number */
    bit<8>  next_ext;  /* Next extension header */
}

// GTPU extension: PDU Session Container (PSC) -- 3GPP TS 38.415 version 15.2.0
// https://www.etsi.org/deliver/etsi_ts/138400_138499/138415/15.02.00_60/ts_138415v150200p.pdf
header gtpu_ext_psc_t {
    bit<8> len;      /* Length in 4-octet units (common to all extensions) */
    bit<4> type;     /* Uplink or downlink */
    bit<4> spare0;   /* Reserved */
    bit<1> ppp;      /* Paging Policy Presence (UL only, not supported) */
    bit<1> rqi;      /* Reflective QoS Indicator (UL only) */
    qfi_t  qfi;      /* QoS Flow Identifier */
    bit<8> next_ext;
}

#ifdef WITH_SPGW
struct spgw_meta_t {
    bit<16>           ipv4_len;
    teid_t            teid;
    bit<16>           tunnel_src_port;
    bit<32>           tunnel_src_addr;
    bit<32>           tunnel_dst_addr;
    pdr_ctr_id_t      ctr_id;
    far_id_t          far_id;
    spgw_interface_t  src_iface;
    qfi_t             qfi;
    _BOOL             skip_spgw;
    _BOOL             notify_spgwc;
    _BOOL             needs_gtpu_encap;
    _BOOL             needs_gtpu_decap;
    _BOOL             skip_egress_pdr_ctr;
    _BOOL             needs_qfi_push;
}
#endif // WITH_SPGW

#ifdef WITH_BNG

typedef bit<2> bng_type_t;
const bng_type_t BNG_TYPE_INVALID = 2w0x0;
const bng_type_t BNG_TYPE_UPSTREAM = 2w0x1;
const bng_type_t BNG_TYPE_DOWNSTREAM = 2w0x2;;

struct bng_meta_t {
    bit<2>    type; // upstream or downstream
    bit<32>   line_id; // subscriber line
    bit<16>   pppoe_session_id;
    bit<32>   ds_meter_result; // for downstream metering
    vlan_id_t s_tag;
    vlan_id_t c_tag;
}
#endif // WITH_BNG

// Used for table lookup. Initialized with the parsed headers, or 0 if invalid.
// Never updated by the pipe. When both outer and inner IPv4 headers are valid,
// this should always carry the inner ones. The assumption is that we terminate
// GTP tunnels in the fabric, so we are more interested in observing/blocking
// the inner flows. We might revisit this decision in the future.
struct lookup_metadata_t {
    _BOOL                   is_ipv4;
    bit<32>                 ipv4_src;
    bit<32>                 ipv4_dst;
    bit<8>                  ip_proto;
    l4_port_t               l4_sport;
    l4_port_t               l4_dport;
    bit<8>                  icmp_type;
    bit<8>                  icmp_code;
}

//Custom metadata definition
struct fabric_metadata_t {
    lookup_metadata_t lkp;
    bit<16>       ip_eth_type;
    vlan_id_t     vlan_id;
    bit<3>        vlan_pri;
    bit<1>        vlan_cfi;
#ifdef WITH_DOUBLE_VLAN_TERMINATION
    _BOOL         push_double_vlan;
    vlan_id_t     inner_vlan_id;
    bit<3>        inner_vlan_pri;
    bit<1>        inner_vlan_cfi;
#endif // WITH_DOUBLE_VLAN_TERMINATION
    mpls_label_t  mpls_label;
    bit<8>        mpls_ttl;
    _BOOL         skip_forwarding;
    _BOOL         skip_next;
    fwd_type_t    fwd_type;
    next_id_t     next_id;
    _BOOL         is_multicast;
    _BOOL         is_controller_packet_out;
    bit<8>        ip_proto;
    bit<16>       l4_sport;
    bit<16>       l4_dport;
    bit<32>       ipv4_src_addr;
    bit<32>       ipv4_dst_addr;
    slice_id_t    slice_id;
    bit<2>        packet_color;
    tc_t          tc;
    bit<6>        dscp;
#ifdef WITH_SPGW
    bit<16>       inner_l4_sport;
    bit<16>       inner_l4_dport;
    spgw_meta_t   spgw;
#endif // WITH_SPGW
#ifdef WITH_BNG
    bng_meta_t    bng;
#endif // WITH_BNG
#ifdef WITH_INT
    int_metadata_t int_meta;
#endif // WITH_INT
    port_type_t   port_type;
}

struct parsed_headers_t {
    ethernet_t ethernet;
    vlan_tag_t vlan_tag;
#if defined(WITH_XCONNECT) || defined(WITH_DOUBLE_VLAN_TERMINATION)
    vlan_tag_t inner_vlan_tag;
#endif // WITH_XCONNECT || WITH_DOUBLE_VLAN_TERMINATION
    eth_type_t eth_type;
#ifdef WITH_BNG
    pppoe_t pppoe;
#endif // WITH_BNG
    mpls_t mpls;
#ifdef WITH_SPGW
    ipv4_t gtpu_ipv4;
    udp_t gtpu_udp;
    gtpu_t outer_gtpu;
    gtpu_options_t outer_gtpu_options;
    gtpu_ext_psc_t outer_gtpu_ext_psc;
#endif // WITH_SPGW
    gtpu_t gtpu;
    gtpu_options_t gtpu_options;
    gtpu_ext_psc_t gtpu_ext_psc;
    ipv4_t inner_ipv4;
    udp_t inner_udp;
    tcp_t inner_tcp;
    icmp_t inner_icmp;
    ipv4_t ipv4;
#ifdef WITH_IPV6
    ipv6_t ipv6;
#endif // WITH_IPV6
    tcp_t tcp;
    udp_t udp;
    icmp_t icmp;
    packet_out_header_t packet_out;
    packet_in_header_t packet_in;
#ifdef WITH_INT_SINK
    // INT Report encap
    ethernet_t report_ethernet;
    eth_type_t report_eth_type;
    ipv4_t report_ipv4;
    udp_t report_udp;
    // INT Report header (support only fixed)
    report_fixed_header_t report_fixed_header;
    // local_report_t report_local;
#endif // WITH_INT_SINK
#ifdef WITH_INT
    // INT specific headers
    intl4_shim_t intl4_shim;
    int_header_t int_header;
    int_switch_id_t int_switch_id;
    int_port_ids_t int_port_ids;
    int_hop_latency_t int_hop_latency;
    int_q_occupancy_t int_q_occupancy;
    int_ingress_tstamp_t int_ingress_tstamp;
    int_egress_tstamp_t int_egress_tstamp;
    int_q_congestion_t int_q_congestion;
    int_egress_port_tx_util_t int_egress_tx_util;
#ifdef WITH_INT_SINK
    int_data_t int_data;
#endif // WITH_INT_SINK
    intl4_tail_t intl4_tail;
#endif //WITH_INT
}

#endif
