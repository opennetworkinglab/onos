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

#ifndef __DEFINE__
#define __DEFINE__

#define MAX_PORTS 511

#ifndef _BOOL
#define _BOOL bool
#endif
#ifndef _TRUE
#define _TRUE true
#endif
#ifndef _FALSE
#define _FALSE false
#endif

#ifndef _PKT_OUT_HDR_ANNOT
#define _PKT_OUT_HDR_ANNOT
#endif

#ifndef IP_VER_LENGTH
#define IP_VER_LENGTH 4
#endif
#ifndef IP_VERSION_4
#define IP_VERSION_4 4
#endif
#ifndef IP_VERSION_6
#define IP_VERSION_6 6
#endif

#define ETH_HDR_SIZE 14
#define IPV4_HDR_SIZE 20
#define UDP_HDR_SIZE 8
#define GTP_HDR_SIZE 8

#define UDP_PORT_GTPU 2152
#define GTP_GPDU 0xff
#define GTPU_VERSION 0x01
#define GTP_PROTOCOL_TYPE_GTP 0x01

typedef bit<3>  fwd_type_t;
typedef bit<32> next_id_t;
typedef bit<20> mpls_label_t;
typedef bit<9>  port_num_t;
typedef bit<48> mac_addr_t;
typedef bit<16> group_id_t;
typedef bit<12> vlan_id_t;

const bit<16> ETHERTYPE_QINQ = 0x88A8;
const bit<16> ETHERTYPE_QINQ_NON_STD = 0x9100;
const bit<16> ETHERTYPE_VLAN = 0x8100;
const bit<16> ETHERTYPE_MPLS = 0x8847;
const bit<16> ETHERTYPE_MPLS_MULTICAST =0x8848;
const bit<16> ETHERTYPE_IPV4 = 0x0800;
const bit<16> ETHERTYPE_IPV6 = 0x86dd;
const bit<16> ETHERTYPE_ARP  = 0x0806;

const bit<8> PROTO_ICMP = 1;
const bit<8> PROTO_TCP = 6;
const bit<8> PROTO_UDP = 17;
const bit<8> PROTO_ICMPV6 = 58;

const bit<4> IPV4_MIN_IHL = 5;

const fwd_type_t FWD_BRIDGING = 0;
const fwd_type_t FWD_MPLS = 1;
const fwd_type_t FWD_IPV4_UNICAST = 2;
const fwd_type_t FWD_IPV4_MULTICAST = 3;
const fwd_type_t FWD_IPV6_UNICAST = 4;
const fwd_type_t FWD_IPV6_MULTICAST = 5;

const bit<8> DEFAULT_MPLS_TTL = 64;
const bit<8> DEFAULT_IPV4_TTL = 64;

typedef bit direction_t;
typedef bit pcc_gate_status_t;
typedef bit<32> sdf_rule_id_t;
typedef bit<32> pcc_rule_id_t;

const sdf_rule_id_t DEFAULT_SDF_RULE_ID = 0;
const pcc_rule_id_t DEFAULT_PCC_RULE_ID = 0;

const direction_t DIR_UPLINK = 1w0;
const direction_t DIR_DOWNLINK = 1w1;

const pcc_gate_status_t PCC_GATE_OPEN = 1w0;
const pcc_gate_status_t PCC_GATE_CLOSED = 1w1;

#endif
