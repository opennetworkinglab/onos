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
        inout ipv4_t            gtpu_ipv4,
        inout udp_t             gtpu_udp,
        inout gtpu_t            gtpu,
        inout ipv4_t            ipv4,
        inout udp_t             udp,
        inout fabric_metadata_t fabric_meta
    ) {

    direct_counter(CounterType.packets_and_bytes) ue_counter;

    @hidden
    action gtpu_decap() {
        gtpu_ipv4.setInvalid();
        gtpu_udp.setInvalid();
        gtpu.setInvalid();
    }

    action set_dl_sess_info(bit<32> teid,
                            bit<32> s1u_enb_addr,
                            bit<32> s1u_sgw_addr) {
        fabric_meta.spgw.teid = teid;
        fabric_meta.spgw.s1u_enb_addr = s1u_enb_addr;
        fabric_meta.spgw.s1u_sgw_addr = s1u_sgw_addr;
        ue_counter.count();
    }

    table dl_sess_lookup {
        key = {
            // UE addr for downlink
            ipv4.dst_addr : exact @name("ipv4_dst");
        }
        actions = {
            set_dl_sess_info();
            @defaultonly nop();
        }
        const default_action = nop();
        counters = ue_counter;
    }

    table s1u_filter_table {
        key = {
            // IP addresses of the S1U interfaces of this SPGW-U instance (when uplink)
            gtpu_ipv4.dst_addr : exact @name("gtp_ipv4_dst");
        }
        actions = {
            nop();
        }
        const default_action = nop();
    }

#ifdef WITH_SPGW_PCC_GATING
    action set_sdf_rule_id(sdf_rule_id_t id) {
        fabric_meta.spgw.sdf_rule_id = id;
    }

    action set_pcc_rule_id(pcc_rule_id_t id) {
        fabric_meta.spgw.pcc_rule_id = id;
    }

    action set_pcc_info(pcc_gate_status_t gate_status) {
        fabric_meta.spgw.pcc_gate_status = gate_status;
    }

    table sdf_rule_lookup {
        key = {
            fabric_meta.spgw.direction   : exact @name("spgw_direction");
            ipv4.src_addr                : ternary @name("ipv4_src");
            ipv4.dst_addr                : ternary @name("ipv4_dst");
            ipv4.protocol                : ternary @name("ip_proto");
            fabric_meta.l4_sport         : ternary @name("l4_sport");
            fabric_meta.l4_dport         : ternary @name("l4_dport");
        }
        actions = {
            set_sdf_rule_id();
        }
        const default_action = set_sdf_rule_id(DEFAULT_SDF_RULE_ID);
    }

    table pcc_rule_lookup {
        key = {
            fabric_meta.spgw.sdf_rule_id : exact @name("sdf_rule_id");
        }
        actions = {
            set_pcc_rule_id();
        }
        const default_action = set_pcc_rule_id(DEFAULT_PCC_RULE_ID);
    }

    table pcc_info_lookup {
        key = {
            fabric_meta.spgw.pcc_rule_id : exact @name("pcc_rule_id");
        }
        actions = {
            set_pcc_info();
        }
        const default_action = set_pcc_info(PCC_GATE_OPEN);
    }
#endif // WITH_SPGW_PCC_GATING

    apply {
        if (gtpu.isValid()) {
            // If here, pkt has outer IP dst on
            // S1U_SGW_PREFIX/S1U_SGW_PREFIX_LEN subnet.
            // TODO: check also that gtpu.msgtype == GTP_GPDU
            if (!s1u_filter_table.apply().hit) {
                mark_to_drop();
            }
            fabric_meta.spgw.direction = SPGW_DIR_UPLINK;
            gtpu_decap();
        } else if (dl_sess_lookup.apply().hit) {
            fabric_meta.spgw.direction = SPGW_DIR_DOWNLINK;
        } else {
            fabric_meta.spgw.direction = SPGW_DIR_UNKNOWN;
            // No SPGW processing needed.
            return;
        }

#ifdef WITH_SPGW_PCC_GATING
        // Allow all traffic by default.
        fabric_meta.spgw.pcc_gate_status = PCC_GATE_OPEN;

        sdf_rule_lookup.apply();
        pcc_rule_lookup.apply();
        pcc_info_lookup.apply();

        if (fabric_meta.spgw.pcc_gate_status == PCC_GATE_CLOSED) {
            mark_to_drop();
        }
#endif // WITH_SPGW_PCC_GATING

        // Don't ask why... we'll need this later.
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

    @hidden
    action gtpu_encap() {
        gtpu_ipv4.setValid();
        gtpu_ipv4.version = IP_VERSION_4;
        gtpu_ipv4.ihl = IPV4_MIN_IHL;
        gtpu_ipv4.dscp = 0;
        gtpu_ipv4.ecn = 0;
        gtpu_ipv4.total_len = ipv4.total_len
                + (IPV4_HDR_SIZE + UDP_HDR_SIZE + GTP_HDR_SIZE);
        gtpu_ipv4.identification = 0x1513; /* From NGIC */
        gtpu_ipv4.flags = 0;
        gtpu_ipv4.frag_offset = 0;
        gtpu_ipv4.ttl = DEFAULT_IPV4_TTL;
        gtpu_ipv4.protocol = PROTO_UDP;
        gtpu_ipv4.dst_addr = fabric_meta.spgw.s1u_enb_addr;
        gtpu_ipv4.src_addr = fabric_meta.spgw.s1u_sgw_addr;
        gtpu_ipv4.hdr_checksum = 0; // Updated later

        gtpu_udp.setValid();
        gtpu_udp.sport = UDP_PORT_GTPU;
        gtpu_udp.dport = UDP_PORT_GTPU;
        gtpu_udp.len = fabric_meta.spgw.ipv4_len
                + (UDP_HDR_SIZE + GTP_HDR_SIZE);
        gtpu_udp.checksum = 0; // Updated later

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
        if (fabric_meta.spgw.direction == SPGW_DIR_DOWNLINK) {
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
