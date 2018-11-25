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
    inout parsed_headers_t hdr,
    inout fabric_metadata_t fmeta,
    inout standard_metadata_t smeta) {

    action init_metadata(bit<32> switch_id) {
        fmeta.int_meta.transit = _TRUE;
#ifdef _INT_INIT_METADATA
        // Allow other targets to initialize INT metadata in their own way.
        _INT_INIT_METADATA
#else
        fmeta.int_meta.switch_id = switch_id;
#endif // _INT_INIT_METADATA
    }

#ifdef _INT_METADATA_ACTIONS
    _INT_METADATA_ACTIONS
#else
    // Switch ID.
    @hidden
    action int_set_header_0() {
        hdr.int_switch_id.setValid();
        hdr.int_switch_id.switch_id = fmeta.int_meta.switch_id;
    }
    // Port IDs.
    @hidden
    action int_set_header_1() {
        hdr.int_port_ids.setValid();
        hdr.int_port_ids.ingress_port_id = (bit<16>) smeta.ingress_port;
        hdr.int_port_ids.egress_port_id = (bit<16>) smeta.egress_port;
    }
    // Hop latency.
    @hidden
    action int_set_header_2() {
        hdr.int_hop_latency.setValid();
        hdr.int_hop_latency.hop_latency = (bit<32>) smeta.deq_timedelta;
    }
    // Queue occupancy.
    @hidden
    action int_set_header_3() {
        hdr.int_q_occupancy.setValid();
        // TODO: support queues in BMv2. ATM we assume only one.
        hdr.int_q_occupancy.q_id = 8w0;
        hdr.int_q_occupancy.q_occupancy = (bit<24>) smeta.deq_qdepth;
    }
    // Ingress timestamp.
    @hidden
    action int_set_header_4() {
        hdr.int_ingress_tstamp.setValid();
        hdr.int_ingress_tstamp.ingress_tstamp = (bit<32>) smeta.enq_timestamp;
    }
    // Egress timestamp.
    @hidden
    action int_set_header_5() {
        hdr.int_egress_tstamp.setValid();
        hdr.int_egress_tstamp.egress_tstamp = (bit<32>) smeta.enq_timestamp + (bit<32>) smeta.deq_timedelta;
    }
    // Queue congestion.
    @hidden
    action int_set_header_6() {
        hdr.int_q_congestion.setValid();
        // TODO: support queue congestion.
        hdr.int_q_congestion.q_id = 8w0;
        hdr.int_q_congestion.q_congestion = 24w0;
    }
    // Egress port utilization.
    @hidden
    action int_set_header_7() {
        hdr.int_egress_tx_util.setValid();
        // TODO: implement tx utilization support in BMv2.
        hdr.int_egress_tx_util.egress_port_tx_util = 32w0;
    }
#endif // _INT_METADATA_ACTIONS

    // Actions to keep track of the new metadata added.
    @hidden
    action add_1() {
        fmeta.int_meta.new_words = fmeta.int_meta.new_words + 1;
        fmeta.int_meta.new_bytes = fmeta.int_meta.new_bytes + 4;
    }

    @hidden
    action add_2() {
        fmeta.int_meta.new_words = fmeta.int_meta.new_words + 2;
        fmeta.int_meta.new_bytes = fmeta.int_meta.new_bytes + 8;
    }

    @hidden
    action add_3() {
        fmeta.int_meta.new_words = fmeta.int_meta.new_words + 3;
        fmeta.int_meta.new_bytes = fmeta.int_meta.new_bytes + 12;
    }

    @hidden
    action add_4() {
        fmeta.int_meta.new_words = fmeta.int_meta.new_words + 4;
        fmeta.int_meta.new_bytes = fmeta.int_meta.new_bytes + 16;
    }

    // Action function for bits 0-3 combinations, 0 is msb, 3 is lsb.
    // Each bit set indicates that corresponding INT header should be added.
    @hidden
    action int_set_header_0003_i0() {
    }
    @hidden
    action int_set_header_0003_i1() {
        int_set_header_3();
        add_1();
    }
    @hidden
    action int_set_header_0003_i2() {
        int_set_header_2();
        add_1();
    }
    @hidden
    action int_set_header_0003_i3() {
        int_set_header_3();
        int_set_header_2();
        add_2();
    }
    @hidden
    action int_set_header_0003_i4() {
        int_set_header_1();
        add_1();
    }
    @hidden
    action int_set_header_0003_i5() {
        int_set_header_3();
        int_set_header_1();
        add_2();
    }
    @hidden
    action int_set_header_0003_i6() {
        int_set_header_2();
        int_set_header_1();
        add_2();
    }
    @hidden
    action int_set_header_0003_i7() {
        int_set_header_3();
        int_set_header_2();
        int_set_header_1();
        add_3();
    }
    @hidden
    action int_set_header_0003_i8() {
        int_set_header_0();
        add_1();
    }
    @hidden
    action int_set_header_0003_i9() {
        int_set_header_3();
        int_set_header_0();
        add_2();
    }
    @hidden
    action int_set_header_0003_i10() {
        int_set_header_2();
        int_set_header_0();
        add_2();
    }
    @hidden
    action int_set_header_0003_i11() {
        int_set_header_3();
        int_set_header_2();
        int_set_header_0();
        add_3();
    }
    @hidden
    action int_set_header_0003_i12() {
        int_set_header_1();
        int_set_header_0();
        add_2();
    }
    @hidden
    action int_set_header_0003_i13() {
        int_set_header_3();
        int_set_header_1();
        int_set_header_0();
        add_3();
    }
    @hidden
    action int_set_header_0003_i14() {
        int_set_header_2();
        int_set_header_1();
        int_set_header_0();
        add_3();
    }
    @hidden
    action int_set_header_0003_i15() {
        int_set_header_3();
        int_set_header_2();
        int_set_header_1();
        int_set_header_0();
        add_4();
    }

    // Action function for bits 4-7 combinations, 4 is msb, 7 is lsb.
    @hidden
    action int_set_header_0407_i0() {
    }
    @hidden
    action int_set_header_0407_i1() {
        int_set_header_7();
        add_1();
    }
    @hidden
    action int_set_header_0407_i2() {
        int_set_header_6();
        add_1();
    }
    @hidden
    action int_set_header_0407_i3() {
        int_set_header_7();
        int_set_header_6();
        add_2();
    }
    @hidden
    action int_set_header_0407_i4() {
        int_set_header_5();
        add_1();
    }
    @hidden
    action int_set_header_0407_i5() {
        int_set_header_7();
        int_set_header_5();
        add_2();
    }
    @hidden
    action int_set_header_0407_i6() {
        int_set_header_6();
        int_set_header_5();
        add_2();
    }
    @hidden
    action int_set_header_0407_i7() {
        int_set_header_7();
        int_set_header_6();
        int_set_header_5();
        add_3();
    }
    @hidden
    action int_set_header_0407_i8() {
        int_set_header_4();
        add_1();
    }
    @hidden
    action int_set_header_0407_i9() {
        int_set_header_7();
        int_set_header_4();
        add_2();
    }
    @hidden
    action int_set_header_0407_i10() {
        int_set_header_6();
        int_set_header_4();
        add_2();
    }
    @hidden
    action int_set_header_0407_i11() {
        int_set_header_7();
        int_set_header_6();
        int_set_header_4();
        add_3();
    }
    @hidden
    action int_set_header_0407_i12() {
        int_set_header_5();
        int_set_header_4();
        add_2();
    }
    @hidden
    action int_set_header_0407_i13() {
        int_set_header_7();
        int_set_header_5();
        int_set_header_4();
        add_3();
    }
    @hidden
    action int_set_header_0407_i14() {
        int_set_header_6();
        int_set_header_5();
        int_set_header_4();
        add_3();
    }
    @hidden
    action int_set_header_0407_i15() {
        int_set_header_7();
        int_set_header_6();
        int_set_header_5();
        int_set_header_4();
        add_4();
    }

    // Default action used to set switch ID.
    table tb_int_insert {
        // We don't really need a key here, however we add a dummy one as a
        // workaround to ONOS inability to properly support default actions.
        key = {
            hdr.int_header.isValid(): exact @name("int_is_valid");
        }
        actions = {
            init_metadata;
            @defaultonly nop;
        }
        const default_action = nop();
        size = 1;
    }

    // Table to process instruction bits 0-3.
    @hidden
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
        const entries = {
            (0x0) : int_set_header_0003_i0();
            (0x1) : int_set_header_0003_i1();
            (0x2) : int_set_header_0003_i2();
            (0x3) : int_set_header_0003_i3();
            (0x4) : int_set_header_0003_i4();
            (0x5) : int_set_header_0003_i5();
            (0x6) : int_set_header_0003_i6();
            (0x7) : int_set_header_0003_i7();
            (0x8) : int_set_header_0003_i8();
            (0x9) : int_set_header_0003_i9();
            (0xA) : int_set_header_0003_i10();
            (0xB) : int_set_header_0003_i11();
            (0xC) : int_set_header_0003_i12();
            (0xD) : int_set_header_0003_i13();
            (0xE) : int_set_header_0003_i14();
            (0xF) : int_set_header_0003_i15();
        }
    }

    // Table to process instruction bits 4-7.
    @hidden
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
        const entries = {
            (0x0) : int_set_header_0407_i0();
            (0x1) : int_set_header_0407_i1();
            (0x2) : int_set_header_0407_i2();
            (0x3) : int_set_header_0407_i3();
            (0x4) : int_set_header_0407_i4();
            (0x5) : int_set_header_0407_i5();
            (0x6) : int_set_header_0407_i6();
            (0x7) : int_set_header_0407_i7();
            (0x8) : int_set_header_0407_i8();
            (0x9) : int_set_header_0407_i9();
            (0xA) : int_set_header_0407_i10();
            (0xB) : int_set_header_0407_i11();
            (0xC) : int_set_header_0407_i12();
            (0xD) : int_set_header_0407_i13();
            (0xE) : int_set_header_0407_i14();
            (0xF) : int_set_header_0407_i15();
        }
    }

    apply {
        tb_int_insert.apply();
        if (fmeta.int_meta.transit == _FALSE) {
            return;
        }
        tb_int_inst_0003.apply();
        tb_int_inst_0407.apply();
        // Increment hop cnt
        hdr.int_header.total_hop_cnt = hdr.int_header.total_hop_cnt + 1;
        // Update headers lengths.
        if (hdr.ipv4.isValid()) {
            hdr.ipv4.total_len = hdr.ipv4.total_len + fmeta.int_meta.new_bytes;
        }
        if (hdr.udp.isValid()) {
            hdr.udp.len = hdr.udp.len + fmeta.int_meta.new_bytes;
        }
        if (hdr.intl4_shim.isValid()) {
            hdr.intl4_shim.len_words = hdr.intl4_shim.len_words + fmeta.int_meta.new_words;
        }
    }
}

#endif
