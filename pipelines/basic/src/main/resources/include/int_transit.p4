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
#ifndef __INT_TRANSIT__
#define __INT_TRANSIT__
control process_int_transit (
    inout headers_t hdr,
    inout local_metadata_t local_metadata,
    inout standard_metadata_t standard_metadata) {

    direct_counter(CounterType.packets_and_bytes) counter_int_insert;
    direct_counter(CounterType.packets_and_bytes) counter_int_inst_0003;
    direct_counter(CounterType.packets_and_bytes) counter_int_inst_0407;

    action int_update_total_hop_cnt() {
        hdr.int_header.total_hop_cnt = hdr.int_header.total_hop_cnt + 1;
    }

    action int_transit(switch_id_t switch_id) {
        local_metadata.int_meta.switch_id = switch_id;
        local_metadata.int_meta.insert_byte_cnt = (bit<16>) hdr.int_header.ins_cnt << 2;
    }

    /* Instr Bit 0 */
    action int_set_header_0() { //switch_id
        hdr.int_switch_id.setValid();
        hdr.int_switch_id.switch_id = local_metadata.int_meta.switch_id;
    }
    action int_set_header_1() { //port_ids
        hdr.int_port_ids.setValid();
        hdr.int_port_ids.ingress_port_id =
        (bit<16>) standard_metadata.ingress_port;
        hdr.int_port_ids.egress_port_id =
        (bit<16>) standard_metadata.egress_port;
    }
    action int_set_header_2() { //hop_latency
        hdr.int_hop_latency.setValid();
        hdr.int_hop_latency.hop_latency =
        (bit<32>) standard_metadata.deq_timedelta;
    }
    action int_set_header_3() { //q_occupancy
        // TODO: Support egress queue ID
        hdr.int_q_occupancy.setValid();
        hdr.int_q_occupancy.q_id =
        0;
        // (bit<8>) standard_metadata.egress_qid;
        hdr.int_q_occupancy.q_occupancy =
        (bit<24>) standard_metadata.deq_qdepth;
    }
    action int_set_header_4() { //ingress_tstamp
        hdr.int_ingress_tstamp.setValid();
        hdr.int_ingress_tstamp.ingress_tstamp =
        (bit<32>) standard_metadata.enq_timestamp;
    }
    action int_set_header_5() { //egress_timestamp
        hdr.int_egress_tstamp.setValid();
        hdr.int_egress_tstamp.egress_tstamp =
        (bit<32>) standard_metadata.enq_timestamp +
        (bit<32>) standard_metadata.deq_timedelta;
    }
    action int_set_header_6() { //q_congestion
        // TODO: implement queue congestion support in BMv2
        // TODO: update egress queue ID
        hdr.int_q_congestion.setValid();
        hdr.int_q_congestion.q_id =
        0;
        // (bit<8>) standard_metadata.egress_qid;
        hdr.int_q_congestion.q_congestion =
        // (bit<24>) queueing_metadata.deq_congestion;
        0;
    }
    action int_set_header_7() { //egress_port_tx_utilization
        // TODO: implement tx utilization support in BMv2
        hdr.int_egress_tx_util.setValid();
        hdr.int_egress_tx_util.egress_port_tx_util =
        // (bit<32>) queueing_metadata.tx_utilization;
        0;
    }

    /* action function for bits 0-3 combinations, 0 is msb, 3 is lsb */
    /* Each bit set indicates that corresponding INT header should be added */
    action int_set_header_0003_i0() {
    }
    action int_set_header_0003_i1() {
        int_set_header_3();
    }
    action int_set_header_0003_i2() {
        int_set_header_2();
    }
    action int_set_header_0003_i3() {
        int_set_header_3();
        int_set_header_2();
    }
    action int_set_header_0003_i4() {
        int_set_header_1();
    }
    action int_set_header_0003_i5() {
        int_set_header_3();
        int_set_header_1();
    }
    action int_set_header_0003_i6() {
        int_set_header_2();
        int_set_header_1();
    }
    action int_set_header_0003_i7() {
        int_set_header_3();
        int_set_header_2();
        int_set_header_1();
    }
    action int_set_header_0003_i8() {
        int_set_header_0();
    }
    action int_set_header_0003_i9() {
        int_set_header_3();
        int_set_header_0();
    }
    action int_set_header_0003_i10() {
        int_set_header_2();
        int_set_header_0();
    }
    action int_set_header_0003_i11() {
        int_set_header_3();
        int_set_header_2();
        int_set_header_0();
    }
    action int_set_header_0003_i12() {
        int_set_header_1();
        int_set_header_0();
    }
    action int_set_header_0003_i13() {
        int_set_header_3();
        int_set_header_1();
        int_set_header_0();
    }
    action int_set_header_0003_i14() {
        int_set_header_2();
        int_set_header_1();
        int_set_header_0();
    }
    action int_set_header_0003_i15() {
        int_set_header_3();
        int_set_header_2();
        int_set_header_1();
        int_set_header_0();
    }

    /* action function for bits 4-7 combinations, 4 is msb, 7 is lsb */
    action int_set_header_0407_i0() {
    }
    action int_set_header_0407_i1() {
        int_set_header_7();
    }
    action int_set_header_0407_i2() {
        int_set_header_6();
    }
    action int_set_header_0407_i3() {
        int_set_header_7();
        int_set_header_6();
    }
    action int_set_header_0407_i4() {
        int_set_header_5();
    }
    action int_set_header_0407_i5() {
        int_set_header_7();
        int_set_header_5();
    }
    action int_set_header_0407_i6() {
        int_set_header_6();
        int_set_header_5();
    }
    action int_set_header_0407_i7() {
        int_set_header_7();
        int_set_header_6();
        int_set_header_5();
    }
    action int_set_header_0407_i8() {
        int_set_header_4();
    }
    action int_set_header_0407_i9() {
        int_set_header_7();
        int_set_header_4();
    }
    action int_set_header_0407_i10() {
        int_set_header_6();
        int_set_header_4();
    }
    action int_set_header_0407_i11() {
        int_set_header_7();
        int_set_header_6();
        int_set_header_4();
    }
    action int_set_header_0407_i12() {
        int_set_header_5();
        int_set_header_4();
    }
    action int_set_header_0407_i13() {
        int_set_header_7();
        int_set_header_5();
        int_set_header_4();
    }
    action int_set_header_0407_i14() {
        int_set_header_6();
        int_set_header_5();
        int_set_header_4();
    }
    action int_set_header_0407_i15() {
        int_set_header_7();
        int_set_header_6();
        int_set_header_5();
        int_set_header_4();
    }

    table tb_int_insert {
        key = {}
        actions = {
            int_transit;
        }
        counters = counter_int_insert;
        size = 2;
    }

    /* Table to process instruction bits 0-3 */
    table tb_int_inst_0003 {
        key = {
            hdr.int_header.instruction_mask_0003 : exact;
        }
        actions = {
            int_set_header_0003_i0;
            int_set_header_0003_i1;
            int_set_header_0003_i2;
            int_set_header_0003_i3;
            int_set_header_0003_i4;
            int_set_header_0003_i5;
            int_set_header_0003_i6;
            int_set_header_0003_i7;
            int_set_header_0003_i8;
            int_set_header_0003_i9;
            int_set_header_0003_i10;
            int_set_header_0003_i11;
            int_set_header_0003_i12;
            int_set_header_0003_i13;
            int_set_header_0003_i14;
            int_set_header_0003_i15;
        }
        counters = counter_int_inst_0003;
        size = 16;
    }

    /* Table to process instruction bits 4-7 */
    table tb_int_inst_0407 {
        key = {
            hdr.int_header.instruction_mask_0407 : exact;
        }
        actions = {
            int_set_header_0407_i0;
            int_set_header_0407_i1;
            int_set_header_0407_i2;
            int_set_header_0407_i3;
            int_set_header_0407_i4;
            int_set_header_0407_i5;
            int_set_header_0407_i6;
            int_set_header_0407_i7;
            int_set_header_0407_i8;
            int_set_header_0407_i9;
            int_set_header_0407_i10;
            int_set_header_0407_i11;
            int_set_header_0407_i12;
            int_set_header_0407_i13;
            int_set_header_0407_i14;
            int_set_header_0407_i15;
        }
        counters = counter_int_inst_0407;
        size = 16;
    }

    apply {
        tb_int_insert.apply();
        tb_int_inst_0003.apply();
        tb_int_inst_0407.apply();
        int_update_total_hop_cnt();
    }
}

control process_int_outer_encap (
    inout headers_t hdr,
    inout local_metadata_t local_metadata,
    inout standard_metadata_t standard_metadata) {

    action int_update_ipv4() {
        hdr.ipv4.len = hdr.ipv4.len + local_metadata.int_meta.insert_byte_cnt;
    }
    action int_update_udp() {
        hdr.udp.length_ = hdr.udp.length_ + local_metadata.int_meta.insert_byte_cnt;
    }
    action int_update_shim() {
        hdr.intl4_shim.len = hdr.intl4_shim.len + (bit<8>)hdr.int_header.ins_cnt;
    }

    apply {
        if (hdr.ipv4.isValid()) {
            int_update_ipv4();
        }
        if (hdr.udp.isValid()) {
            int_update_udp();
        }
        if (hdr.intl4_shim.isValid()) {
            int_update_shim();
        }
    }
}

#endif
