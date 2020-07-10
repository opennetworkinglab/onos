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
#ifndef __INT_REPORT__
#define __INT_REPORT__

control process_int_report (
    inout parsed_headers_t hdr,
    inout fabric_metadata_t fabric_metadata,
    inout standard_metadata_t standard_metadata) {

    @hidden
    action add_report_fixed_header() {
        /* Device should include its own INT metadata as embedded,
         * we'll not use fabric_report_header for this purpose.
         */
        hdr.report_fixed_header.setValid();
        hdr.report_fixed_header.ver = 0;
        /* only support for flow_watchlist */
        hdr.report_fixed_header.nproto = NPROTO_ETHERNET;
        hdr.report_fixed_header.d = 0;
        hdr.report_fixed_header.q = 0;
        hdr.report_fixed_header.f = 1;
        hdr.report_fixed_header.rsvd = 0;
        //TODO how to get information specific to the switch
        hdr.report_fixed_header.hw_id = HW_ID;
        // TODO how save a variable and increment
        hdr.report_fixed_header.seq_no = 0;
        //TODO how to get timestamp from ingress ns
        hdr.report_fixed_header.ingress_tstamp = (bit<32>) standard_metadata.enq_timestamp;
    }

    action do_report_encapsulation(mac_addr_t src_mac, mac_addr_t mon_mac, ipv4_addr_t src_ip,
                        ipv4_addr_t mon_ip, l4_port_t mon_port) {
        //Report Ethernet Header
        hdr.report_ethernet.setValid();
        hdr.report_ethernet.dst_addr = mon_mac;
        hdr.report_ethernet.src_addr = src_mac;
        hdr.report_eth_type.setValid();
        hdr.report_eth_type.value = ETHERTYPE_IPV4;

        //Report IPV4 Header
        hdr.report_ipv4.setValid();
        hdr.report_ipv4.version = 4w4;
        hdr.report_ipv4.ihl = 4w5;
        hdr.report_ipv4.dscp = 6w0;
        hdr.report_ipv4.ecn = 2w0;
        /* Total Len is report_ipv4_len + report_udp_len + report_fixed_hdr_len + ethernet_len + ipv4_totalLen */
        hdr.report_ipv4.total_len = (bit<16>) IPV4_MIN_HEAD_LEN + (bit<16>) UDP_HEADER_LEN +
                                (bit<16>) REPORT_FIXED_HEADER_LEN +  (bit<16>) ETH_HEADER_LEN + hdr.ipv4.total_len;
        /* Dont Fragment bit should be set */
        hdr.report_ipv4.identification = 0;
        hdr.report_ipv4.flags = 0;
        hdr.report_ipv4.frag_offset = 0;
        hdr.report_ipv4.ttl = 0xFF;
        hdr.report_ipv4.protocol = PROTO_UDP;
        hdr.report_ipv4.src_addr = src_ip;
        hdr.report_ipv4.dst_addr = mon_ip;

        //Report UDP Header
        hdr.report_udp.setValid();
        hdr.report_udp.sport = 0;
        hdr.report_udp.dport = mon_port;
        hdr.report_udp.len =  (bit<16>) UDP_HEADER_LEN + (bit<16>) REPORT_FIXED_HEADER_LEN +
                                    (bit<16>) ETH_HEADER_LEN + hdr.ipv4.total_len;

        add_report_fixed_header();
    }

    /* Cloned packet instance_type is PKT_INSTANCE_TYPE_INGRESS_CLONE=1
     * Packet is forwarded according to the mirroring_add command
     */
    table tb_generate_report {
        key = {
        }
        actions = {
            do_report_encapsulation;
            @defaultonly nop();
        }
        default_action = nop;
    }

    apply {
        tb_generate_report.apply();
    }
}
#endif
