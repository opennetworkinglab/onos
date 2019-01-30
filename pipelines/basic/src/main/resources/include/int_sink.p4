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
#ifndef __INT_SINK__
#define __INT_SINK__

control process_int_sink (
    inout headers_t hdr,
    inout local_metadata_t local_metadata,
    inout standard_metadata_t standard_metadata) {
    @hidden
    action restore_header () {
        hdr.ipv4.dscp = hdr.intl4_shim.dscp;
        // restore length fields of IPv4 header and UDP header
        bit<16> len_bytes = ((bit<16>)hdr.intl4_shim.len) << 2;
        hdr.ipv4.len = hdr.ipv4.len - len_bytes;
        hdr.udp.length_ = hdr.udp.length_ - len_bytes;
    }

    @hidden
    action int_sink() {
        // remove all the INT information from the packet
        hdr.int_header.setInvalid();
        hdr.int_data.setInvalid();
        hdr.intl4_shim.setInvalid();
        hdr.int_switch_id.setInvalid();
        hdr.int_level1_port_ids.setInvalid();
        hdr.int_hop_latency.setInvalid();
        hdr.int_q_occupancy.setInvalid();
        hdr.int_ingress_tstamp.setInvalid();
        hdr.int_egress_tstamp.setInvalid();
        hdr.int_level2_port_ids.setInvalid();
        hdr.int_egress_tx_util.setInvalid();
    }

    apply {
        restore_header();
        int_sink();
    }
}
#endif
