#include <core.p4>
#include <v1model.p4>
#include "include/defines.p4"
#include "include/headers.p4"
#include "include/parsers.p4"
#include "include/port_counters.p4"
#include "include/checksums.p4"
#include "include/actions.p4"
#include "include/packet_io.p4"


control ingress(inout headers_t hdr, inout metadata_t meta, inout standard_metadata_t standard_metadata) {

    direct_counter(CounterType.packets) ecmp_group_table_counter;
    direct_counter(CounterType.packets) table0_counter;

    action ecmp_group(group_id_t group_id, group_size_t groupSize) {
        meta.ecmp_metadata.group_id = group_id;
        hash(meta.ecmp_metadata.selector, HashAlgorithm.crc16, (bit<64>)0,
        { hdr.ipv4.srcAddr, hdr.ipv4.dstAddr, hdr.ipv4.protocol, hdr.tcp.srcPort, hdr.tcp.dstPort, hdr.udp.srcPort,
            hdr.udp.dstPort },
        (bit<128>)groupSize);
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
