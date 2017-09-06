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

#include <core.p4>
#include <v1model.p4>
#include "include/defines.p4"
#include "include/headers.p4"

#define SELECTOR_WIDTH 64
const bit<SELECTOR_WIDTH> ONE = 64w1;

typedef bit<16> group_id_t;

struct wcmp_metadata_t {
    group_id_t group_id;
    bit<8>  numBits;
    bit<SELECTOR_WIDTH> selector;
}

struct metadata_t {
    wcmp_metadata_t wcmp_metadata;
    intrinsic_metadata_t intrinsic_metadata;
}

#include "include/parsers.p4"
#include "include/port_counters.p4"
#include "include/checksums.p4"
#include "include/actions.p4"
#include "include/packet_io.p4"

control ingress(inout headers_t hdr, inout metadata_t meta, inout standard_metadata_t standard_metadata) {

    direct_counter(CounterType.packets) table0_counter;
    direct_counter(CounterType.packets) wcmp_group_table_counter;

    action wcmp_group(group_id_t group_id) {
        meta.wcmp_metadata.group_id = group_id;
        hash(meta.wcmp_metadata.numBits, HashAlgorithm.crc16, (bit<64>)2,
        { hdr.ipv4.srcAddr, hdr.ipv4.dstAddr, hdr.ipv4.protocol, hdr.tcp.srcPort, hdr.tcp.dstPort, hdr.udp.srcPort,
            hdr.udp.dstPort },
        (bit<128>)62);
    }

    action wcmp_set_selector() {
        meta.wcmp_metadata.selector = ((ONE << meta.wcmp_metadata.numBits) - ONE) << (SELECTOR_WIDTH - meta.wcmp_metadata.numBits);
    }

    table table0 {
        support_timeout = true;
        actions = {
            set_egress_port(standard_metadata);
            wcmp_group;
            send_to_cpu(standard_metadata);
            _drop(standard_metadata);
        }
        key = {
            standard_metadata.ingress_port: ternary;
            hdr.ethernet.dstAddr          : ternary;
            hdr.ethernet.srcAddr          : ternary;
            hdr.ethernet.etherType        : ternary;
        }
        counters = table0_counter;
        default_action = _drop(standard_metadata);
    }

    table wcmp_group_table {
        actions = {
            set_egress_port(standard_metadata);
        }
        key = {
            meta.wcmp_metadata.group_id : exact;
            meta.wcmp_metadata.selector: lpm;
        }
        counters = wcmp_group_table_counter;
    }

    PortCountersControl() port_counters_control;
    PacketIoIngressControl() packet_io_ingress_control;

    apply {
        packet_io_ingress_control.apply(hdr, standard_metadata);
        if (!hdr.packet_out.isValid()) {
            switch (table0.apply().action_run) {
                wcmp_group: {
                    wcmp_set_selector();
                    wcmp_group_table.apply();
                }
            }
        }
        port_counters_control.apply(hdr, meta, standard_metadata);
    }
}

control egress(inout headers_t hdr, inout metadata_t meta, inout standard_metadata_t standard_metadata) {

    PacketIoEgressControl() packet_io_egress_control;
    apply {
        packet_io_egress_control.apply(hdr, standard_metadata);
    }
}

V1Switch(ParserImpl(), verifyChecksum(), ingress(), egress(), computeChecksum(), DeparserImpl()) main;
