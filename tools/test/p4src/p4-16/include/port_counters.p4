#ifndef PORT_COUNTERS
#define PORT_COUNTERS
#include "defines.p4"

control PortCountersControl(inout headers hdr, inout metadata meta, inout standard_metadata_t standard_metadata) {
    counter(MAX_PORTS, CounterType.packets) egress_port_counter;
    counter(MAX_PORTS, CounterType.packets) ingress_port_counter;

    apply {
        if (standard_metadata.egress_spec < MAX_PORTS) {
            ingress_port_counter.count((bit<32>)standard_metadata.ingress_port);
            egress_port_counter.count((bit<32>)standard_metadata.egress_spec);
        }
    }
}
#endif
