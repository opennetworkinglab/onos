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
#ifndef __INT_SOURCE__
#define __INT_SOURCE__

// Insert INT header to the packet
control process_int_source (
    inout headers_t hdr,
    inout local_metadata_t local_metadata,
    inout standard_metadata_t standard_metadata) {

    direct_counter(CounterType.packets_and_bytes) counter_int_source;

    action int_source(bit<5> hop_metadata_len, bit<8> remaining_hop_cnt, bit<4> ins_mask0003, bit<4> ins_mask0407) {
        // insert INT shim header
        hdr.intl4_shim.setValid();
        // int_type: Hop-by-hop type (1) , destination type (2)
        hdr.intl4_shim.int_type = 1;
        hdr.intl4_shim.len = INT_HEADER_LEN_WORD;
        hdr.intl4_shim.dscp = hdr.ipv4.dscp;

        // insert INT header
        hdr.int_header.setValid();
        hdr.int_header.ver = 0;
        hdr.int_header.rep = 0;
        hdr.int_header.c = 0;
        hdr.int_header.e = 0;
        hdr.int_header.m = 0;
        hdr.int_header.rsvd1 = 0;
        hdr.int_header.rsvd2 = 0;
        hdr.int_header.hop_metadata_len = hop_metadata_len;
        hdr.int_header.remaining_hop_cnt = remaining_hop_cnt;
        hdr.int_header.instruction_mask_0003 = ins_mask0003;
        hdr.int_header.instruction_mask_0407 = ins_mask0407;
        hdr.int_header.instruction_mask_0811 = 0; // not supported
        hdr.int_header.instruction_mask_1215 = 0; // not supported

        // add the header len (3 words) to total len
        hdr.ipv4.len = hdr.ipv4.len + INT_HEADER_SIZE + INT_SHIM_HEADER_SIZE;
        hdr.udp.length_ = hdr.udp.length_ + INT_HEADER_SIZE + INT_SHIM_HEADER_SIZE;
    }
    action int_source_dscp(bit<5> hop_metadata_len, bit<8> remaining_hop_cnt, bit<4> ins_mask0003, bit<4> ins_mask0407) {
        int_source(hop_metadata_len, remaining_hop_cnt, ins_mask0003, ins_mask0407);
        hdr.ipv4.dscp = DSCP_INT;
        counter_int_source.count();
    }

    table tb_int_source {
        key = {
            hdr.ipv4.src_addr: ternary;
            hdr.ipv4.dst_addr: ternary;
            local_metadata.l4_src_port: ternary;
            local_metadata.l4_dst_port: ternary;
        }
        actions = {
            int_source_dscp;
            @defaultonly nop();
        }
        counters = counter_int_source;
        const default_action = nop();
    }

    apply {
        tb_int_source.apply();
    }
}

control process_int_source_sink (
    inout headers_t hdr,
    inout local_metadata_t local_metadata,
    inout standard_metadata_t standard_metadata) {

    direct_counter(CounterType.packets_and_bytes) counter_set_source;
    direct_counter(CounterType.packets_and_bytes) counter_set_sink;

    action int_set_source () {
        local_metadata.int_meta.source = _TRUE;
        counter_set_source.count();
    }

    action int_set_sink () {
        local_metadata.int_meta.sink = _TRUE;
        counter_set_sink.count();
    }

    table tb_set_source {
        key = {
            standard_metadata.ingress_port: exact;
        }
        actions = {
            int_set_source;
            @defaultonly nop();
        }
        counters = counter_set_source;
        const default_action = nop();
        size = MAX_PORTS;
    }
    table tb_set_sink {
        key = {
            standard_metadata.egress_spec: exact;
        }
        actions = {
            int_set_sink;
            @defaultonly nop();
        }
        counters = counter_set_sink;
        const default_action = nop();
        size = MAX_PORTS;
    }

    apply {
        tb_set_source.apply();
        tb_set_sink.apply();
    }
}
#endif
