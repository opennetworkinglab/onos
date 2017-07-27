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

    direct_counter(CounterType.packets) table0_counter;
    action_selector(HashAlgorithm.crc16, 32w64, 32w16) ecmp_selector;

    table table0 {
        support_timeout = true;
        key = {
            standard_metadata.ingress_port : ternary;
            hdr.ethernet.dstAddr           : ternary;
            hdr.ethernet.srcAddr           : ternary;
            hdr.ethernet.etherType         : ternary;
            // Not for matching.
            // Inputs to the hash function of the action selector.
            hdr.ipv4.srcAddr               : selector;
            hdr.ipv4.dstAddr               : selector;
            hdr.ipv4.protocol              : selector;
            hdr.tcp.srcPort                : selector;
            hdr.tcp.dstPort                : selector;
            hdr.udp.srcPort                : selector;
            hdr.udp.dstPort                : selector;
        }
        actions = {
            set_egress_port(standard_metadata);
            send_to_cpu(standard_metadata);
            drop(standard_metadata);
        }
        counters = table0_counter;
        implementation = ecmp_selector;
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
