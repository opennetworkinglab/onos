#include <core.p4>
#include <v1model.p4>
#include "include/defines.p4"
#include "include/headers.p4"
#include "include/parsers.p4"
#include "include/port_counters.p4"
#include "include/checksums.p4"
#include "include/actions.p4"

#define SELECTOR_WIDTH 64
const bit<SELECTOR_WIDTH> ONE = 64w1;

control ingress(inout headers hdr, inout metadata meta, inout standard_metadata_t standard_metadata) {
    direct_counter(CounterType.packets) table0_counter;
    direct_counter(CounterType.packets) wcmp_group_table_counter;

    action wcmp_group(GroupId groupId) {
        meta.wcmp_meta.groupId = groupId;
        hash(meta.wcmp_meta.numBits, HashAlgorithm.crc16, (bit<64>)2, { hdr.ipv4.srcAddr, hdr.ipv4.dstAddr, hdr.ipv4.protocol, hdr.tcp.srcPort, hdr.tcp.dstPort, hdr.udp.srcPort, hdr.udp.dstPort }, (bit<128>)62);
    }

    action wcmp_set_selector() {
        meta.wcmp_meta.selector = ((ONE << meta.wcmp_meta.numBits) - ONE) << (SELECTOR_WIDTH - meta.wcmp_meta.numBits);
    }

    table table0 {
        support_timeout = true;
        actions = {
            set_egress_port(standard_metadata);
            wcmp_group;
            send_to_cpu(standard_metadata);
            drop(standard_metadata);
        }
        key = {
            standard_metadata.ingress_port: ternary;
            hdr.ethernet.dstAddr          : ternary;
            hdr.ethernet.srcAddr          : ternary;
            hdr.ethernet.etherType        : ternary;
        }
        counters = table0_counter;
    }

    table wcmp_group_table {
        actions = {
            set_egress_port(standard_metadata);
        }
        key = {
            meta.wcmp_meta.groupId : exact;
            meta.wcmp_meta.selector: lpm;
        }
        counters = wcmp_group_table_counter;
    }

    PortCountersControl() port_counters_control;
    apply {
        switch (table0.apply().action_run) {
            wcmp_group: {
                wcmp_set_selector();
                wcmp_group_table.apply();
            }
        }
        port_counters_control.apply(hdr, meta, standard_metadata);
    }
}

control egress(inout headers hdr, inout metadata meta, inout standard_metadata_t standard_metadata) {
    apply {
        // Nothing to do
    }
}

V1Switch(ParserImpl(), verifyChecksum(), ingress(), egress(), computeChecksum(), DeparserImpl()) main;
