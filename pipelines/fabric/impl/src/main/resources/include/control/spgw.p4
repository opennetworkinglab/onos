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

control DecapGtpu(inout parsed_headers_t hdr,
                  inout fabric_metadata_t fabric_md) {
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
    @hidden
    action decap_inner_tcp() {
        decap_inner_common();
        hdr.udp.setInvalid();
        hdr.tcp = hdr.inner_tcp;
        hdr.inner_tcp.setInvalid();
    }
    @hidden
    action decap_inner_udp() {
        decap_inner_common();
        hdr.udp = hdr.inner_udp;
        hdr.inner_udp.setInvalid();
    }
    @hidden
    action decap_inner_icmp() {
        decap_inner_common();
        hdr.udp.setInvalid();
        hdr.icmp = hdr.inner_icmp;
        hdr.inner_icmp.setInvalid();
    }
    @hidden
    action decap_inner_unknown() {
        decap_inner_common();
        hdr.udp.setInvalid();
    }
    @hidden
    table decap_gtpu {
        key = {
            hdr.inner_tcp.isValid()  : exact;
            hdr.inner_udp.isValid()  : exact;
            hdr.inner_icmp.isValid() : exact;
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
        size = 4;
    }
    apply {
        decap_gtpu.apply();
    }
}


control SpgwIngress(inout parsed_headers_t hdr,
                    inout fabric_metadata_t fabric_md,
                    inout standard_metadata_t standard_metadata) {

    //=============================//
    //===== Misc Things ======//
    //=============================//
    
    counter(MAX_PDR_COUNTERS, CounterType.packets_and_bytes) pdr_counter;

    DecapGtpu() decap_gtpu_from_dbuf;
    DecapGtpu() decap_gtpu;


    //=============================//
    //===== Interface Tables ======//
    //=============================//

    action load_iface(spgw_interface_t src_iface) {
        // Interface type can be access, core, from_dbuf (see InterfaceType enum)
        fabric_md.spgw.src_iface = src_iface;
        fabric_md.spgw.skip_spgw = _FALSE;
    }
    action iface_miss() {
        fabric_md.spgw.src_iface = SPGW_IFACE_UNKNOWN;
        fabric_md.spgw.skip_spgw = _TRUE;
    }

    // TODO: check also that gtpu.msgtype == GTP_GPDU... somewhere
    table interfaces {
        key = {
            hdr.ipv4.dst_addr  : lpm    @name("ipv4_dst_addr");  // outermost header
            hdr.gtpu.isValid() : exact  @name("gtpu_is_valid");
        }
        actions = {
            load_iface;
            @defaultonly iface_miss;
        }
        const default_action = iface_miss();
        size = MAX_INTERFACES;
    }


    //=============================//
    //===== PDR Tables ======//
    //=============================//

    action load_pdr(pdr_ctr_id_t ctr_id,
                    far_id_t far_id,
                    bit<1> needs_gtpu_decap) {
        fabric_md.spgw.ctr_id = ctr_id;
        fabric_md.spgw.far_id = far_id;
        fabric_md.spgw.needs_gtpu_decap = (_BOOL)needs_gtpu_decap;
    }

    // These two tables scale well and cover the average case PDR
    table downlink_pdrs {
        key = {
            // only available ipv4 header
            hdr.ipv4.dst_addr : exact @name("ue_addr");
        }
        actions = {
            load_pdr;
        }
        size = MAX_DOWNLINK_PDRS;
    }

    table uplink_pdrs {
        key = {
            hdr.ipv4.dst_addr           : exact @name("tunnel_ipv4_dst");
            hdr.gtpu.teid               : exact @name("teid");
        }
        actions = {
            load_pdr;
        }
        size = MAX_UPLINK_PDRS;
    }

    //=============================//
    //===== FAR Tables ======//
    //=============================//

    action load_normal_far(bit<1> drop,
                           bit<1> notify_cp) {
        // general far attributes
        fabric_md.skip_forwarding = (_BOOL)drop;
        fabric_md.skip_next = (_BOOL)drop;
        fabric_md.spgw.notify_spgwc = (_BOOL)notify_cp;
    }
    action load_tunnel_far(bit<1>      drop,
                           bit<1>      notify_cp,
                           bit<16>   tunnel_src_port,
                           bit<32>   tunnel_src_addr,
                           bit<32>   tunnel_dst_addr,
                           teid_t    teid) {
        // general far attributes
        fabric_md.skip_forwarding = (_BOOL)drop;
        fabric_md.skip_next = (_BOOL)drop;
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

    action load_dbuf_far(bit<1>    drop,
                         bit<1>    notify_cp,
                         bit<16>   tunnel_src_port,
                         bit<32>   tunnel_src_addr,
                         bit<32>   tunnel_dst_addr,
                         teid_t    teid) {
        load_tunnel_far(drop, notify_cp, tunnel_src_port,
                                   tunnel_src_addr, tunnel_dst_addr, teid);
        fabric_md.spgw.skip_egress_pdr_ctr = _TRUE;
    }

    table fars {
        key = {
            fabric_md.spgw.far_id : exact @name("far_id");
        }
        actions = {
            load_normal_far;
            load_tunnel_far;
            load_dbuf_far;
        }
        // default is drop and don't notify CP
        const default_action = load_normal_far(1, 0);
        size = MAX_FARS;
    }


    //=============================//
    //===== Apply Block ======//
    //=============================//
    apply {

        // Interfaces
        if (interfaces.apply().hit) {
            if (fabric_md.spgw.src_iface == SPGW_IFACE_FROM_DBUF) {
                decap_gtpu_from_dbuf.apply(hdr, fabric_md);
            }
            // PDRs
            if (hdr.gtpu.isValid()) {
                uplink_pdrs.apply();
            } else {
                downlink_pdrs.apply();
            }
            if (fabric_md.spgw.src_iface != SPGW_IFACE_FROM_DBUF) {
                pdr_counter.count(fabric_md.spgw.ctr_id);
            }

            // GTPU Decapsulate
            if (fabric_md.spgw.needs_gtpu_decap == _TRUE) { 
                decap_gtpu.apply(hdr, fabric_md);
            }

            // FARs
            // Load FAR info
            fars.apply();

            // Nothing to be done immediately for forwarding or encapsulation.
            // Forwarding is done by other parts of fabric.p4, and
            // encapsulation is done in the egress

            // Needed for correct GTPU encapsulation in egress
            fabric_md.spgw.ipv4_len = hdr.ipv4.total_len;
        }
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
        if (fabric_md.spgw.skip_spgw == _FALSE) {
            if (fabric_md.spgw.needs_gtpu_encap == _TRUE) {
                gtpu_encap();
            }
            if (fabric_md.spgw.skip_egress_pdr_ctr == _FALSE) {
                pdr_counter.count(fabric_md.spgw.ctr_id);
            }
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
