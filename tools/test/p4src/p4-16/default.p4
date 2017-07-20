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

    table table0 {
        support_timeout = true;
        actions = {
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
