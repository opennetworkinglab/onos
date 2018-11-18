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

    action int_source(bit<8> max_hop, bit<5> ins_cnt, bit<4> ins_mask0003, bit<4> ins_mask0407) {
        // insert INT shim header
        hdr.intl4_shim.setValid();
        // int_type: Hop-by-hop type (1) , destination type (2)
        hdr.intl4_shim.int_type = 1;
        hdr.intl4_shim.len = INT_HEADER_LEN_WORD;

        // insert INT header
        hdr.int_header.setValid();
        hdr.int_header.ver = 0;
        hdr.int_header.rep = 0;
        hdr.int_header.c = 0;
        hdr.int_header.e = 0;
        hdr.int_header.rsvd1 = 0;
        hdr.int_header.ins_cnt = ins_cnt;
        hdr.int_header.max_hop_cnt = max_hop;
        hdr.int_header.total_hop_cnt = 0;
        hdr.int_header.instruction_mask_0003 = ins_mask0003;
        hdr.int_header.instruction_mask_0407 = ins_mask0407;
        hdr.int_header.instruction_mask_0811 = 0; // not supported
        hdr.int_header.instruction_mask_1215 = 0; // not supported

        // insert INT tail header
        hdr.intl4_tail.setValid();
        hdr.intl4_tail.next_proto = hdr.ipv4.protocol;
        hdr.intl4_tail.dest_port = local_metadata.l4_dst_port;
        hdr.intl4_tail.dscp = (bit<8>) hdr.ipv4.dscp;

        // add the header len (8 bytes) to total len
        hdr.ipv4.len = hdr.ipv4.len + 16;
        hdr.udp.length_ = hdr.udp.length_ + 16;
    }
    action int_source_dscp(bit<8> max_hop, bit<5> ins_cnt, bit<4> ins_mask0003, bit<4> ins_mask0407) {
        int_source(max_hop, ins_cnt, ins_mask0003, ins_mask0407);
        hdr.ipv4.dscp = INT_DSCP;
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
        }
        counters = counter_int_source;
        size = 1024;
    }

    apply {
        tb_int_source.apply();
    }
}

control process_set_source_sink (
    inout headers_t hdr,
    inout local_metadata_t local_metadata,
    inout standard_metadata_t standard_metadata) {

    direct_counter(CounterType.packets_and_bytes) counter_set_source;
    direct_counter(CounterType.packets_and_bytes) counter_set_sink;

    action int_set_source () {
        local_metadata.int_meta.source = 1;
    }

    action int_set_sink () {
        local_metadata.int_meta.sink = 1;
    }

    table tb_set_source {
        key = {
            standard_metadata.ingress_port: exact;
        }
        actions = {
            int_set_source;
        }
        counters = counter_set_source;
        size = 256;
    }
    table tb_set_sink {
        key = {
            standard_metadata.egress_spec: exact;
        }
        actions = {
            int_set_sink;
        }
        counters = counter_set_sink;
        size = 256;
    }

    apply {
        tb_set_source.apply();
        tb_set_sink.apply();
    }
}
#endif
