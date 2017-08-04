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

struct metadata_t {
    intrinsic_metadata_t intrinsic_metadata;
}

#include "include/parsers.p4"
#include "include/port_counters.p4"
#include "include/checksums.p4"
#include "include/actions.p4"
#include "include/packet_io.p4"

control ingress(inout headers_t hdr, inout metadata_t meta, inout standard_metadata_t standard_metadata) {

    /*
    FIXME:
    It seems that with BMv2 it is not possible to use the same indirect table (like table0 with the
    implementation attribute enabled), with table entries that use direct actions (e.g. send_to_cpu()).
    A separate table for ECMP should be created.
    */

    direct_counter(CounterType.packets) table0_counter;
    // action_selector(HashAlgorithm.crc16, 32w64, 32w16) ecmp_selector;

    table table0 {
        /*
        Disabling timeout here as P4runtime doesn't allow setting timeouts.
        This way the FlowRuleTranslator will produce instances of PiTableEntry without timeout.
        */
        support_timeout = false;
        key = {
            standard_metadata.ingress_port : ternary;
            hdr.ethernet.dstAddr           : ternary;
            hdr.ethernet.srcAddr           : ternary;
            hdr.ethernet.etherType         : ternary;
            // Not for matching.
            // Inputs to the hash function of the action selector.
            /* hdr.ipv4.srcAddr               : selector;
            hdr.ipv4.dstAddr               : selector;
            hdr.ipv4.protocol              : selector;
            hdr.tcp.srcPort                : selector;
            hdr.tcp.dstPort                : selector;
            hdr.udp.srcPort                : selector;
            hdr.udp.dstPort                : selector; */
        }
        actions = {
            set_egress_port(standard_metadata);
            send_to_cpu(standard_metadata);
            drop(standard_metadata);
        }
        counters = table0_counter;
        // implementation = ecmp_selector;
    }

    PacketIoIngressControl() packet_io_ingress_control;
    PortCountersControl() port_counters_control;

    apply {
        packet_io_ingress_control.apply(hdr, standard_metadata);
        if (!hdr.packet_out.isValid()) {
            table0.apply();
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
