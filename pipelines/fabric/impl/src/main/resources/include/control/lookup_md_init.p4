/*
 * Copyright 2021-present Open Networking Foundation
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

#ifndef __LOOKUP__
#define __LOOKUP__

control LookupMdInit (in parsed_headers_t hdr,
                      out lookup_metadata_t lkp_md) {
    apply {
        lkp_md.is_ipv4 = _FALSE;
        lkp_md.ipv4_src = 0;
        lkp_md.ipv4_dst = 0;
        lkp_md.ip_proto = 0;
        lkp_md.l4_sport = 0;
        lkp_md.l4_dport = 0;
        lkp_md.icmp_type = 0;
        lkp_md.icmp_code = 0;
        if (hdr.inner_ipv4.isValid()) {
            lkp_md.is_ipv4 = true;
            lkp_md.ipv4_src = hdr.inner_ipv4.src_addr;
            lkp_md.ipv4_dst = hdr.inner_ipv4.dst_addr;
            lkp_md.ip_proto = hdr.inner_ipv4.protocol;
            if (hdr.inner_tcp.isValid()) {
                lkp_md.l4_sport = hdr.inner_tcp.sport;
                lkp_md.l4_dport = hdr.inner_tcp.dport;
            } else if (hdr.inner_udp.isValid()) {
                lkp_md.l4_sport = hdr.inner_udp.sport;
                lkp_md.l4_dport = hdr.inner_udp.dport;
            } else if (hdr.inner_icmp.isValid()) {
                lkp_md.icmp_type = hdr.inner_icmp.icmp_type;
                lkp_md.icmp_code = hdr.inner_icmp.icmp_code;
            }
        } else if (hdr.ipv4.isValid()) {
            lkp_md.is_ipv4 = true;
            lkp_md.ipv4_src = hdr.ipv4.src_addr;
            lkp_md.ipv4_dst = hdr.ipv4.dst_addr;
            lkp_md.ip_proto = hdr.ipv4.protocol;
            if (hdr.tcp.isValid()) {
                lkp_md.l4_sport = hdr.tcp.sport;
                lkp_md.l4_dport = hdr.tcp.dport;
            } else if (hdr.udp.isValid()) {
                lkp_md.l4_sport = hdr.udp.sport;
                lkp_md.l4_dport = hdr.udp.dport;
            } else if (hdr.icmp.isValid()) {
                lkp_md.icmp_type = hdr.icmp.icmp_type;
                lkp_md.icmp_code = hdr.icmp.icmp_code;
            }
        }
    }
}

#endif

