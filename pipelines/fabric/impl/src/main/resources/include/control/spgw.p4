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

#define DEFAULT_PDR_CTR_ID 0
#define DEFAULT_FAR_ID 0

#ifndef MAX_UES
#define MAX_UES        1024
#endif // MAX_UES
#define MAX_INTERFACES 128

#define MAX_UPLINK_PDRS   MAX_UES
#define MAX_DOWNLINK_PDRS MAX_UES
#define MAX_PDR_COUNTERS  2 * MAX_UES
#define MAX_FARS          2 * MAX_UES



control SpgwIngress(inout parsed_headers_t hdr,
                    inout fabric_metadata_t fabric_md,
                    inout standard_metadata_t standard_metadata) {




    //=============================//
    //===== Interface Tables ======//
    //=============================//

    action set_source_iface(spgw_interface_t src_iface, direction_t direction,
                            bit<1> skip_spgw) {
        // Interface type can be access, core, n6_lan, etc (see InterfaceType enum)
        // If interface is from the control plane, direction can be either up or down
        fabric_md.spgw.src_iface = src_iface;
        fabric_md.spgw.direction = direction;
        fabric_md.spgw.skip_spgw = (_BOOL)skip_spgw;
    }
    // TODO: check also that gtpu.msgtype == GTP_GPDU... somewhere
    table interface_lookup {
        key = {
            hdr.ipv4.dst_addr  : lpm    @name("ipv4_dst_addr");  // outermost header
            hdr.gtpu.isValid() : exact  @name("gtpu_is_valid");
        }
        actions = {
            set_source_iface;
        }
        const default_action = set_source_iface(SPGW_IFACE_UNKNOWN, SPGW_DIR_UNKNOWN, 1);
        size = MAX_INTERFACES;
    }


    //=============================//
    //===== PDR Tables ======//
    //=============================//

    action set_pdr_attributes(pdr_ctr_id_t ctr_id,
                              far_id_t far_id,
                              bit<1> needs_gtpu_decap) {
        fabric_md.spgw.pdr_hit = _TRUE;
        fabric_md.spgw.ctr_id = ctr_id;
        fabric_md.spgw.far_id = far_id;
        fabric_md.spgw.needs_gtpu_decap = (_BOOL)needs_gtpu_decap;
    }

    // These two tables scale well and cover the average case PDR
    table downlink_pdr_lookup {
        key = {
            // only available ipv4 header
            hdr.ipv4.dst_addr : exact @name("ue_addr");
        }
        actions = {
            set_pdr_attributes;
        }
        const default_action = set_pdr_attributes(DEFAULT_PDR_CTR_ID, DEFAULT_FAR_ID, 0);
        size = MAX_DOWNLINK_PDRS;
    }
    table uplink_pdr_lookup {
        key = {
            // tunnel_dst_addr will be static for Q2 target. Can remove if need more scaling
            hdr.ipv4.dst_addr           : exact @name("tunnel_ipv4_dst");
            hdr.gtpu.teid               : exact @name("teid");
            hdr.inner_ipv4.src_addr     : exact @name("ue_addr");
        }
        actions = {
            set_pdr_attributes;
        }
        const default_action = set_pdr_attributes(DEFAULT_PDR_CTR_ID, DEFAULT_FAR_ID, 0);
        size = MAX_UPLINK_PDRS;
    }
    // This table scales poorly and covers uncommon PDRs
    table flexible_pdr_lookup {
        key = {
            fabric_md.spgw.src_iface    : ternary @name("src_iface");
            // GTPU
            hdr.gtpu.isValid()          : ternary @name("gtpu_is_valid");
            hdr.gtpu.teid               : ternary @name("teid");
            // SDF
            // outer 5-tuple
            hdr.ipv4.src_addr           : ternary @name("ipv4_src");
            hdr.ipv4.dst_addr           : ternary @name("ipv4_dst");
            hdr.ipv4.protocol           : ternary @name("ip_proto");
            fabric_md.l4_sport          : ternary @name("l4_sport");
            fabric_md.l4_dport          : ternary @name("l4_dport");
            // inner 5-tuple
            hdr.inner_ipv4.src_addr     : ternary @name("inner_ipv4_src");
            hdr.inner_ipv4.dst_addr     : ternary @name("inner_ipv4_dst");
            hdr.inner_ipv4.protocol     : ternary @name("inner_ip_proto");
            fabric_md.inner_l4_sport    : ternary @name("inner_l4_sport");
            fabric_md.inner_l4_dport    : ternary @name("inner_l4_dport");
        }
        actions = {
            set_pdr_attributes;
        }
        const default_action = set_pdr_attributes(DEFAULT_PDR_CTR_ID, DEFAULT_FAR_ID, 0);
    }

    //=============================//
    //===== FAR Tables ======//
    //=============================//

    action load_normal_far_attributes(bit<1> drop,
                                      bit<1> notify_cp) {
        // general far attributes
        fabric_md.spgw.far_dropped  = (_BOOL)drop;
        fabric_md.spgw.notify_spgwc = (_BOOL)notify_cp;
    }
    action load_tunnel_far_attributes(bit<1>      drop,
                                      bit<1>      notify_cp,
                                      bit<16>   tunnel_src_port,
                                      bit<32>   tunnel_src_addr,
                                      bit<32>   tunnel_dst_addr,
                                      teid_t    teid) {
        // general far attributes
        fabric_md.spgw.far_dropped  = (_BOOL)drop;
        fabric_md.spgw.notify_spgwc = (_BOOL)notify_cp;
        // GTP tunnel attributes
        fabric_md.spgw.needs_gtpu_encap = _TRUE;
        fabric_md.spgw.teid = teid;
        fabric_md.spgw.tunnel_src_port = tunnel_src_port;
        fabric_md.spgw.tunnel_src_addr = tunnel_src_addr;
        fabric_md.spgw.tunnel_dst_addr = tunnel_dst_addr;
        // update metadata for correct routing/hashing
        fabric_md.ipv4_src_addr = tunnel_src_addr;
        fabric_md.ipv4_dst_addr = tunnel_dst_addr;
        fabric_md.l4_sport = tunnel_src_port;
        fabric_md.l4_dport = UDP_PORT_GTPU;
    }

    table far_lookup {
        key = {
            fabric_md.spgw.far_id : exact @name("far_id");
        }
        actions = {
            load_normal_far_attributes;
            load_tunnel_far_attributes;
        }
        // default is drop and don't notify CP
        const default_action = load_normal_far_attributes(1, 1);
        size = MAX_FARS;
    }

    //=============================//
    //===== Misc Things ======//
    //=============================//
    
    counter(MAX_PDR_COUNTERS, CounterType.packets_and_bytes) pdr_counter;


    @hidden
    action decap_inner_common() {
        // Correct parser-set metadata to use the inner header values
        fabric_md.ip_eth_type   = ETHERTYPE_IPV4;
        fabric_md.ip_proto      = hdr.inner_ipv4.protocol;
        fabric_md.ipv4_src_addr = hdr.inner_ipv4.src_addr;
        fabric_md.ipv4_dst_addr = hdr.inner_ipv4.dst_addr;
        fabric_md.l4_sport      = fabric_md.inner_l4_sport;
        fabric_md.l4_dport      = fabric_md.inner_l4_dport;
        // Move GTPU and inner L3 headers out
        hdr.ipv4 = hdr.inner_ipv4;
        hdr.inner_ipv4.setInvalid();
        hdr.gtpu.setInvalid();
    }
    action decap_inner_tcp() {
        decap_inner_common();
        hdr.udp.setInvalid();
        hdr.tcp = hdr.inner_tcp;
        hdr.inner_tcp.setInvalid();
    }
    action decap_inner_udp() {
        decap_inner_common();
        hdr.udp = hdr.inner_udp;
        hdr.inner_udp.setInvalid();
    }
    action decap_inner_icmp() {
        decap_inner_common();
        hdr.udp.setInvalid();
        hdr.icmp = hdr.inner_icmp;
        hdr.inner_icmp.setInvalid();
    }
    action decap_inner_unknown() {
        decap_inner_common();
        hdr.udp.setInvalid();
    }
    @hidden
    table decap_gtpu {
        key = {
            hdr.inner_tcp.isValid()     : exact;
            hdr.inner_udp.isValid()     : exact;
            hdr.inner_icmp.isValid()    : exact;
        }
        actions = {
            decap_inner_tcp;
            decap_inner_udp;
            decap_inner_icmp;
            decap_inner_unknown;
        }
        const default_action = decap_inner_unknown;
        const entries = {
            (true,  false, false) : decap_inner_tcp();
            (false, true,  false) : decap_inner_udp();
            (false, false, true)  : decap_inner_icmp();
        }
    }


    //=============================//
    //===== Apply Block ======//
    //=============================//
    apply {

        // Interfaces
        interface_lookup.apply();

        // If interface table missed, or the interface skips PDRs/FARs (TODO: is that a thing?)
        if (fabric_md.spgw.skip_spgw == _TRUE) return;

        // PDRs
        // Currently only best-case PDR tables to make v1model-to-tofino compiler happy
        if (hdr.gtpu.isValid()) {
            uplink_pdr_lookup.apply();
        } else {
            downlink_pdr_lookup.apply();
        }
        // Inefficient PDR table if efficient tables missed
        // Removed to make v1model-to-tofino compiler happy. Not removed in TNA port
        //if (fabric_md.spgw.pdr_hit == _FALSE) {
        //    flexible_pdr_lookup.apply();
        //}
        pdr_counter.count(fabric_md.spgw.ctr_id);

        // GTPU Decapsulate
        if (fabric_md.spgw.needs_gtpu_decap == _TRUE) { 
            decap_gtpu.apply();
        }

        // FARs
        // Load FAR info
        far_lookup.apply();

        if (fabric_md.spgw.notify_spgwc == _TRUE) {
            // TODO: cpu clone action here
        }
        if (fabric_md.spgw.far_dropped == _TRUE) {
            // Do dropping in the same way as fabric's filtering.p4, so we can traverse
            // the ACL table, which is good for cases like DHCP.
            fabric_md.skip_forwarding = _TRUE;
            fabric_md.skip_next = _TRUE;
        }

        // Nothing to be done immediately for forwarding or encapsulation.
        // Forwarding is done by other parts of fabric.p4, and
        // encapsulation is done in the egress

        // Needed for correct GTPU encapsulation in egress
        fabric_md.spgw.ipv4_len = hdr.ipv4.total_len;
    }
}


//====================================//
//============== Egress ==============//
//====================================//
control SpgwEgress(
        inout parsed_headers_t hdr,
        inout fabric_metadata_t fabric_md) {

    counter(MAX_PDR_COUNTERS, CounterType.packets_and_bytes) pdr_counter;


    @hidden
    action gtpu_encap() {
        hdr.gtpu_ipv4.setValid();
        hdr.gtpu_ipv4.version = IP_VERSION_4;
        hdr.gtpu_ipv4.ihl = IPV4_MIN_IHL;
        hdr.gtpu_ipv4.dscp = 0;
        hdr.gtpu_ipv4.ecn = 0;
        hdr.gtpu_ipv4.total_len = hdr.ipv4.total_len
                + (IPV4_HDR_SIZE + UDP_HDR_SIZE + GTP_HDR_SIZE);
        hdr.gtpu_ipv4.identification = 0x1513; /* From NGIC. TODO: Needs to be dynamic */
        hdr.gtpu_ipv4.flags = 0;
        hdr.gtpu_ipv4.frag_offset = 0;
        hdr.gtpu_ipv4.ttl = DEFAULT_IPV4_TTL;
        hdr.gtpu_ipv4.protocol = PROTO_UDP;
        hdr.gtpu_ipv4.src_addr = fabric_md.spgw.tunnel_src_addr;
        hdr.gtpu_ipv4.dst_addr = fabric_md.spgw.tunnel_dst_addr;
        hdr.gtpu_ipv4.hdr_checksum = 0; // Updated later

        hdr.gtpu_udp.setValid();
        hdr.gtpu_udp.sport = fabric_md.spgw.tunnel_src_port; 
        hdr.gtpu_udp.dport = UDP_PORT_GTPU;
        hdr.gtpu_udp.len = fabric_md.spgw.ipv4_len
                + (UDP_HDR_SIZE + GTP_HDR_SIZE);
        hdr.gtpu_udp.checksum = 0; // Updated later, if WITH_SPGW_UDP_CSUM_UPDATE


        hdr.outer_gtpu.setValid();
        hdr.outer_gtpu.version = GTPU_VERSION;
        hdr.outer_gtpu.pt = GTP_PROTOCOL_TYPE_GTP;
        hdr.outer_gtpu.spare = 0;
        hdr.outer_gtpu.ex_flag = 0;
        hdr.outer_gtpu.seq_flag = 0;
        hdr.outer_gtpu.npdu_flag = 0;
        hdr.outer_gtpu.msgtype = GTP_GPDU;
        hdr.outer_gtpu.msglen = fabric_md.spgw.ipv4_len;
        hdr.outer_gtpu.teid = fabric_md.spgw.teid;
    }

    apply {
        if (fabric_md.spgw.skip_spgw == _TRUE) return;
        pdr_counter.count(fabric_md.spgw.ctr_id);

        if (fabric_md.spgw.needs_gtpu_encap == _TRUE) {
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
