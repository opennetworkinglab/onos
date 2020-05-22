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

#ifndef __SPGW__
#define __SPGW__

#define MAX_PDR_COUNTERS 1024
#define DEFAULT_PDR_CTR_ID 0
#define DEFAULT_FAR_ID 0

control spgw_normalizer(
        in    bool   is_gtpu_encapped,
        out   ipv4_t gtpu_ipv4,
        out   udp_t  gtpu_udp,
        inout ipv4_t ipv4,
        inout udp_t  udp,
        in    ipv4_t inner_ipv4,
        in    udp_t  inner_udp
    ) {
    apply {
        if (! is_gtpu_encapped) return;
        gtpu_ipv4 = ipv4;
        ipv4 = inner_ipv4;
        gtpu_udp = udp;
        if (inner_udp.isValid()) {
            udp = inner_udp;
        } else {
            udp.setInvalid();
        }
    }
}

control spgw_ingress(
        inout ipv4_t              gtpu_ipv4,
        inout udp_t               gtpu_udp,
        inout gtpu_t              gtpu,
        inout ipv4_t              ipv4,
        inout udp_t               udp,
        inout fabric_metadata_t   fabric_meta,
        inout standard_metadata_t standard_metadata
    ) {

    counter(MAX_PDR_COUNTERS, CounterType.packets_and_bytes) pdr_counter;

    @hidden
    action gtpu_decap() {
        // grab information from the tunnel that we'll need later
        fabric_meta.spgw.teid = gtpu.teid;
        fabric_meta.spgw.tunnel_dst_addr = gtpu_ipv4.dst_addr;
        // update metadata src and dst addresses with the inner packet 
        fabric_meta.ipv4_src_addr = ipv4.src_addr;
        fabric_meta.ipv4_dst_addr = ipv4.dst_addr;
        // decap
        gtpu_ipv4.setInvalid();
        gtpu_udp.setInvalid();
        gtpu.setInvalid();

    }

    table downlink_filter_table {
        key = {
            // UE addr pool for downlink
            ipv4.dst_addr : lpm @name("ipv4_prefix");
        }
        actions = {
            nop();
        }
        const default_action = nop();
    }

    table uplink_filter_table {
        key = {
            // IP addresses of the S1U interfaces of this SPGW-U instance (when uplink)
            gtpu_ipv4.dst_addr : exact @name("gtp_ipv4_dst");
        }
        actions = {
            nop();
        }
        const default_action = nop();
    }

    action set_pdr_attributes(ctr_id_t ctr_id,
                              far_id_t far_id) {
        fabric_meta.spgw.pdr_hit = _TRUE;
        fabric_meta.spgw.ctr_id = ctr_id;
        fabric_meta.spgw.far_id = far_id;
    }

    // These two tables scale well and cover the average case PDR
    table downlink_pdr_lookup {
        key = {
            ipv4.dst_addr : exact @name("ue_addr");
        }
        actions = {
            set_pdr_attributes;
        }
    }
    table uplink_pdr_lookup {
        key = {
            // tunnel_dst_addr will be static for Q2 target. Can remove if need more scaling
            fabric_meta.spgw.tunnel_dst_addr  : exact @name("tunnel_ipv4_dst");
            fabric_meta.spgw.teid          : exact @name("teid");
            ipv4.src_addr                  : exact @name("ue_addr");
        }
        actions = {
            set_pdr_attributes;
        }
    }
    // This table scales poorly and covers uncommon PDRs
    table flexible_pdr_lookup {
        key = {
            // Direction. Eventually change to interface
            fabric_meta.spgw.direction    : ternary @name("spgw_direction");
            // F-TEID
            fabric_meta.spgw.tunnel_dst_addr : ternary @name("tunnel_ipv4_dst");
            fabric_meta.spgw.teid            : ternary @name("teid");
            // SDF (5-tuple)
            ipv4.src_addr                 : ternary @name("ipv4_src");
            ipv4.dst_addr                 : ternary @name("ipv4_dst");
            ipv4.protocol                 : ternary @name("ip_proto");
            fabric_meta.l4_sport          : ternary @name("l4_sport");
            fabric_meta.l4_dport          : ternary @name("l4_dport");
        }
        actions = {
            set_pdr_attributes;
        }
        const default_action = set_pdr_attributes(DEFAULT_PDR_CTR_ID, DEFAULT_FAR_ID);
    }

    action load_normal_far_attributes(bit<1> drop,
                                      bit<1> notify_cp) {
        // general far attributes
        fabric_meta.spgw.far_dropped = (_BOOL)drop;
        fabric_meta.spgw.notify_cp   = (_BOOL)notify_cp;
    }
    action load_tunnel_far_attributes(bit<1>         drop,
                                      bit<1>         notify_cp,
                                      ipv4_addr_t    tunnel_src_addr,
                                      ipv4_addr_t    tunnel_dst_addr,
                                      teid_t         teid) {
        // general far attributes
        fabric_meta.spgw.far_dropped = (_BOOL)drop;
        fabric_meta.spgw.notify_cp = (_BOOL)notify_cp;
        // GTP tunnel attributes
        fabric_meta.spgw.outer_header_creation = _TRUE;
        fabric_meta.spgw.teid = teid;
        fabric_meta.spgw.tunnel_src_addr = tunnel_src_addr;
        fabric_meta.spgw.tunnel_dst_addr = tunnel_dst_addr;
        // update metadata IP addresses for correct routing/hashing
        fabric_meta.ipv4_src_addr = tunnel_src_addr;
        fabric_meta.ipv4_dst_addr = tunnel_dst_addr;
    }


    table far_lookup {
        key = {
            fabric_meta.spgw.far_id : exact @name("far_id");
        }
        actions = {
            load_normal_far_attributes;
            load_tunnel_far_attributes;
        }
        // default is drop and don't notify CP
        const default_action = load_normal_far_attributes(1w1, 1w0);
    }

    apply {
        if (gtpu.isValid()) {
            // If here, pkt has outer IP dst on
            // S1U_SGW_PREFIX/S1U_SGW_PREFIX_LEN subnet.
            // TODO: check also that gtpu.msgtype == GTP_GPDU
            if (!uplink_filter_table.apply().hit) {
                // Should this be changed to a forwarding/next skip instead of a drop?
                mark_to_drop(standard_metadata);
            }
            fabric_meta.spgw.direction = SPGW_DIR_UPLINK;
            gtpu_decap();
        } else if (downlink_filter_table.apply().hit) {
            fabric_meta.spgw.direction = SPGW_DIR_DOWNLINK;
        } else {
            fabric_meta.spgw.direction = SPGW_DIR_UNKNOWN;
            // No SPGW processing needed.
            return;
        }

        // Try the efficient PDR tables first (This PDR partitioning only works
        // if the PDRs do not overlap. Will need fixing later.)
        if (fabric_meta.spgw.direction == SPGW_DIR_UPLINK) {
            uplink_pdr_lookup.apply();
        } else if (fabric_meta.spgw.direction == SPGW_DIR_DOWNLINK) {
            downlink_pdr_lookup.apply();
        } else { // SPGW_DIR_UNKNOWN
            return;
        }
        // If those fail to find a match, use the wildcard tables
        if (fabric_meta.spgw.pdr_hit == _FALSE) {
            flexible_pdr_lookup.apply();
        }

        pdr_counter.count(fabric_meta.spgw.ctr_id);
        // Load FAR info
        far_lookup.apply();

        if (fabric_meta.spgw.notify_cp == _TRUE) {
            // TODO: cpu clone session here
        }
        if (fabric_meta.spgw.far_dropped == _TRUE) {
            // Do dropping in the same way as fabric's filtering.p4, so we can traverse
            // the ACL table, which is good for cases like DHCP.
            fabric_meta.skip_forwarding = _TRUE;
            fabric_meta.skip_next = _TRUE;
        }

        // Nothing to be done immediately for forwarding or encapsulation.
        // Forwarding is done by other parts of fabric.p4, and
        // encapsulation is done in the egress

        // Needed for correct GTPU encapsulation in egress
        fabric_meta.spgw.ipv4_len = ipv4.total_len;
    }
}


control spgw_egress(
        in    ipv4_t              ipv4,
        inout ipv4_t              gtpu_ipv4,
        inout udp_t               gtpu_udp,
        inout gtpu_t              gtpu,
        in    fabric_metadata_t   fabric_meta,
        in    standard_metadata_t std_meta
    ) {

    counter(MAX_PDR_COUNTERS, CounterType.packets_and_bytes) pdr_counter;


    @hidden
    action gtpu_encap() {
        gtpu_ipv4.setValid();
        gtpu_ipv4.version = IP_VERSION_4;
        gtpu_ipv4.ihl = IPV4_MIN_IHL;
        gtpu_ipv4.dscp = 0;
        gtpu_ipv4.ecn = 0;
        gtpu_ipv4.total_len = ipv4.total_len
                + (IPV4_HDR_SIZE + UDP_HDR_SIZE + GTP_HDR_SIZE);
        gtpu_ipv4.identification = 0x1513; /* From NGIC. TODO: Needs to be dynamic */
        gtpu_ipv4.flags = 0;
        gtpu_ipv4.frag_offset = 0;
        gtpu_ipv4.ttl = DEFAULT_IPV4_TTL;
        gtpu_ipv4.protocol = PROTO_UDP;
        gtpu_ipv4.src_addr = fabric_meta.spgw.tunnel_src_addr;
        gtpu_ipv4.dst_addr = fabric_meta.spgw.tunnel_dst_addr;
        gtpu_ipv4.hdr_checksum = 0; // Updated later

        gtpu_udp.setValid();
        gtpu_udp.sport = UDP_PORT_GTPU; // TODO: make this dynamic per 3GPP specs
        gtpu_udp.dport = UDP_PORT_GTPU;
        gtpu_udp.len = fabric_meta.spgw.ipv4_len
                + (UDP_HDR_SIZE + GTP_HDR_SIZE);
        gtpu_udp.checksum = 0; // Updated later, if WITH_SPGW_UDP_CSUM_UPDATE


        gtpu.setValid();
        gtpu.version = GTPU_VERSION;
        gtpu.pt = GTP_PROTOCOL_TYPE_GTP;
        gtpu.spare = 0;
        gtpu.ex_flag = 0;
        gtpu.seq_flag = 0;
        gtpu.npdu_flag = 0;
        gtpu.msgtype = GTP_GPDU;
        gtpu.msglen = fabric_meta.spgw.ipv4_len;
        gtpu.teid = fabric_meta.spgw.teid;
    }

    apply {
        pdr_counter.count(fabric_meta.spgw.ctr_id);

        if (fabric_meta.spgw.outer_header_creation == _TRUE) {
            gtpu_encap();
        }
    }
}


control update_gtpu_checksum(
        inout ipv4_t gtpu_ipv4,
        inout udp_t  gtpu_udp,
        in    gtpu_t gtpu,
        in    ipv4_t ipv4,
        in    udp_t  udp
    ) {
    apply {
        // Compute outer IPv4 checksum.
        update_checksum(gtpu_ipv4.isValid(),
            {
                gtpu_ipv4.version,
                gtpu_ipv4.ihl,
                gtpu_ipv4.dscp,
                gtpu_ipv4.ecn,
                gtpu_ipv4.total_len,
                gtpu_ipv4.identification,
                gtpu_ipv4.flags,
                gtpu_ipv4.frag_offset,
                gtpu_ipv4.ttl,
                gtpu_ipv4.protocol,
                gtpu_ipv4.src_addr,
                gtpu_ipv4.dst_addr
            },
            gtpu_ipv4.hdr_checksum,
            HashAlgorithm.csum16
        );

#ifdef WITH_SPGW_UDP_CSUM_UPDATE
        // Compute outer UDP checksum.
        update_checksum_with_payload(gtpu_udp.isValid(),
            {
                gtpu_ipv4.src_addr,
                gtpu_ipv4.dst_addr,
                8w0,
                gtpu_ipv4.protocol,
                gtpu_udp.len,
                gtpu_udp.sport,
                gtpu_udp.dport,
                gtpu_udp.len,
                gtpu,
                ipv4,
                // FIXME: we are assuming only UDP for downlink packets
                // How to conditionally switch between UDP/TCP/ICMP?
                udp
            },
            gtpu_udp.checksum,
            HashAlgorithm.csum16
        );
#endif // WITH_SPGW_UDP_CSUM_UPDATE
    }
}

#endif
