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

typedef bit<16> group_id_t;

/*
    Expected number of ports of an ECMP group.
    This value is fixed, .i.e. we do not support ECMP over port groups of different size.
    Due to hardware limitations, this value must be constant and a power of 2.
*/
#define ECMP_GROUP_SIZE 128w2

struct ecmp_metadata_t {
    group_id_t group_id;
    bit<16> selector;
}

struct metadata_t {
    ecmp_metadata_t ecmp_metadata;
    intrinsic_metadata_t intrinsic_metadata;
}

#include "include/parsers.p4"
#include "include/port_counters.p4"
#include "include/checksums.p4"
#include "include/actions.p4"
#include "include/packet_io.p4"


control ingress(inout headers_t hdr, inout metadata_t meta, inout standard_metadata_t standard_metadata) {

    direct_counter(CounterType.packets) ecmp_group_table_counter;
    direct_counter(CounterType.packets) table0_counter;

    action ecmp_group(group_id_t group_id) {
        meta.ecmp_metadata.group_id = group_id;
        hash(meta.ecmp_metadata.selector, HashAlgorithm.crc32, (bit<64>)0,
        { hdr.ipv4.srcAddr, hdr.ipv4.dstAddr, hdr.ipv4.protocol, hdr.tcp.srcPort, hdr.tcp.dstPort, hdr.udp.srcPort,
            hdr.udp.dstPort }, ECMP_GROUP_SIZE);
    }

    table ecmp_group_table {
        actions = {
            set_egress_port(standard_metadata);
        }
        key = {
            meta.ecmp_metadata.group_id : exact;
            meta.ecmp_metadata.selector: exact;
        }
        counters = ecmp_group_table_counter;
    }

    table table0 {
        support_timeout = true;
        actions = {
            ecmp_group;
            set_egress_port(standard_metadata);
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

    PortCountersControl() port_counters_control;
    PacketIoIngressControl() packet_io_ingress_control;

    apply {
        packet_io_ingress_control.apply(hdr, standard_metadata);
        if (!hdr.packet_out.isValid()) {
            switch (table0.apply().action_run) {
                ecmp_group: {
                    ecmp_group_table.apply();
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
