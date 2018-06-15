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


control spgw_ingress(
        inout ipv4_t      gtpu_ipv4,
        inout udp_t       gtpu_udp,
        inout gtpu_t      gtpu,
        inout ipv4_t      ipv4,
        inout udp_t       udp,
        inout spgw_meta_t spgw_meta
    ) {

    direct_counter(CounterType.packets_and_bytes) ue_counter;

    action drop_now() {
        mark_to_drop();
        exit;
    }

    action gtpu_decap() {
        gtpu_ipv4.setInvalid();
        gtpu_udp.setInvalid();
        gtpu.setInvalid();
    }

    action set_dl_sess_info(bit<32> teid,
                            bit<32> s1u_enb_addr,
                            bit<32> s1u_sgw_addr) {
        spgw_meta.teid = teid;
        spgw_meta.s1u_enb_addr = s1u_enb_addr;
        spgw_meta.s1u_sgw_addr = s1u_sgw_addr;
    }

    action update_ue_cdr() {
        ue_counter.count();
    }

    table ue_filter_table {
        key = {
            // IP prefixes of the UEs managed by this switch (when downlink)
            ipv4.dst_addr : lpm;
        }
        actions = {
            NoAction();
        }
    }

    table s1u_filter_table {
        key = {
            // IP addresses of the S1U interfaces embodied by this switch.
            spgw_meta.s1u_sgw_addr : exact;
        }
        actions = {
            NoAction();
        }
    }

#ifdef WITH_SPGW_PCC_GATING
    action set_sdf_rule_id(sdf_rule_id_t id) {
        spgw_meta.sdf_rule_id = id;
    }

    action set_pcc_rule_id(pcc_rule_id_t id) {
        spgw_meta.pcc_rule_id = id;
    }

    action set_pcc_info(pcc_gate_status_t gate_status) {
        spgw_meta.pcc_gate_status = gate_status;
    }

    table sdf_rule_lookup {
        key = {
            spgw_meta.direction   : exact;
            ipv4.src_addr         : ternary;
            ipv4.dst_addr         : ternary;
            ipv4.protocol         : ternary;
            spgw_meta.l4_src_port : ternary;
            spgw_meta.l4_dst_port : ternary;
        }
        actions = {
            set_sdf_rule_id();
        }
        const default_action = set_sdf_rule_id(DEFAULT_SDF_RULE_ID);
    }

    table pcc_rule_lookup {
        key = {
            spgw_meta.sdf_rule_id : exact;
        }
        actions = {
            set_pcc_rule_id();
        }
        const default_action = set_pcc_rule_id(DEFAULT_PCC_RULE_ID);
    }

    table pcc_info_lookup {
        key = {
            spgw_meta.pcc_rule_id : exact;
        }
        actions = {
            set_pcc_info();
        }
        const default_action = set_pcc_info(PCC_GATE_OPEN);
    }
#endif // WITH_SPGW_PCC_GATING

    table dl_sess_lookup {
        key = {
            // UE addr for downlink
            ipv4.dst_addr : exact;
        }
        actions = {
            set_dl_sess_info();
        }
    }

    table ue_cdr_table {
        key = {
            // UE addr for downlink
            ipv4.dst_addr : exact;
        }
        actions = {
            update_ue_cdr();
        }
        counters = ue_counter;
    }

    apply {
        spgw_meta.do_spgw = _FALSE;
        if (gtpu.isValid()) {
            // If here, the parsed ipv4 header is the outer GTP one, but
            // fabric needs to forward on the inner one, i.e. this.
            // We store the outer values we need in the metadata, then replace
            // the ipv4 header extracted before with this one.
            spgw_meta.s1u_enb_addr = ipv4.src_addr;
            spgw_meta.s1u_sgw_addr = ipv4.dst_addr;
            ipv4 = gtpu_ipv4;
            udp = gtpu_udp;

            if (s1u_filter_table.apply().hit) {
                // TODO: check also that gtpu.msgtype == GTP_GPDU
                spgw_meta.do_spgw = _TRUE;
                spgw_meta.direction = DIR_UPLINK;
            }
        } else if (ue_filter_table.apply().hit) {
            spgw_meta.do_spgw = _TRUE;
            spgw_meta.direction = DIR_DOWNLINK;
        }

        if (spgw_meta.do_spgw == _FALSE) {
            // Exit this control block.
            return;
        }

        if (spgw_meta.direction == DIR_UPLINK) {
            gtpu_decap();
        }

#ifdef WITH_SPGW_PCC_GATING
        // Allow all traffic by default.
        spgw_meta.pcc_gate_status = PCC_GATE_OPEN;

        sdf_rule_lookup.apply();
        pcc_rule_lookup.apply();
        pcc_info_lookup.apply();

        if (spgw_meta.pcc_gate_status == PCC_GATE_CLOSED) {
            drop_now();
        }
#endif // WITH_SPGW_PCC_GATING

        if (spgw_meta.direction == DIR_DOWNLINK) {
            if (!dl_sess_lookup.apply().hit) {
                // We have no other choice than drop, as we miss the session
                // info necessary to properly GTPU encap the packet.
                drop_now();
            }
            ue_cdr_table.apply();
        }

        // Don't ask why... we'll need this later.
        spgw_meta.ipv4_len = ipv4.total_len;
    }
}


control spgw_egress(
        in ipv4_t               ipv4,
        out ipv4_t              gtpu_ipv4,
        out udp_t               gtpu_udp,
        out gtpu_t              gtpu,
        in  spgw_meta_t         spgw_meta,
        in  standard_metadata_t std_meta
    ) {

    action gtpu_encap() {
        gtpu_ipv4.setValid();
        gtpu_ipv4.version = IP_VERSION_4;
        gtpu_ipv4.ihl = IPV4_MIN_IHL;
        gtpu_ipv4.diffserv = 0;
        gtpu_ipv4.total_len = spgw_meta.ipv4_len
                + (IPV4_HDR_SIZE + UDP_HDR_SIZE + GTP_HDR_SIZE);
        gtpu_ipv4.identification = 0x1513; /* From NGIC */
        gtpu_ipv4.flags = 0;
        gtpu_ipv4.frag_offset = 0;
        gtpu_ipv4.ttl = DEFAULT_IPV4_TTL;
        gtpu_ipv4.protocol = PROTO_UDP;
        gtpu_ipv4.dst_addr = spgw_meta.s1u_enb_addr;
        gtpu_ipv4.src_addr = spgw_meta.s1u_sgw_addr;
        gtpu_ipv4.hdr_checksum = 0; // Updated later

        gtpu_udp.setValid();
        gtpu_udp.src_port = UDP_PORT_GTPU;
        gtpu_udp.dst_port = UDP_PORT_GTPU;
        gtpu_udp.len = spgw_meta.ipv4_len
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
        gtpu.msglen = spgw_meta.ipv4_len;
        gtpu.teid = spgw_meta.teid;
    }

    apply {
        if (spgw_meta.do_spgw == _TRUE && spgw_meta.direction == DIR_DOWNLINK) {
            gtpu_encap();
        }
    }
}


control verify_gtpu_checksum(
        inout ipv4_t gtpu_ipv4
    ) {
    apply {
        // TODO: re-enable gtpu_ipv4 verification
        // with the current parser logic, gtpu_ip4 contains values of
        // the inner header, which is already verified by include/checksum.p4.
        // We need to modify the parser to copy the outer header somewhere
        // else, and verify that here.

        // verify_checksum(gtpu_ipv4.isValid(),
        //     {
        //         gtpu_ipv4.version,
        //         gtpu_ipv4.ihl,
        //         gtpu_ipv4.diffserv,
        //         gtpu_ipv4.total_len,
        //         gtpu_ipv4.identification,
        //         gtpu_ipv4.flags,
        //         gtpu_ipv4.frag_offset,
        //         gtpu_ipv4.ttl,
        //         gtpu_ipv4.protocol,
        //         gtpu_ipv4.src_addr,
        //         gtpu_ipv4.dst_addr
        //     },
        //     gtpu_ipv4.hdr_checksum,
        //     HashAlgorithm.csum16
        // );
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
                gtpu_ipv4.diffserv,
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
                gtpu_udp.src_port,
                gtpu_udp.dst_port,
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
