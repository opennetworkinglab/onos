#include "include/defines.p4"
#include "include/headers.p4"
#include "include/parser.p4"
#include "include/actions.p4"
#include "include/port_counters.p4"

table table0 {
    reads {
        standard_metadata.ingress_port : ternary;
        ethernet.dstAddr : ternary;
        ethernet.srcAddr : ternary;
        ethernet.etherType : ternary;
    }
    actions {
        set_egress_port;
        send_to_cpu;
        _drop;
    }
    support_timeout: true;
}

counter table0_counter {
    type: packets;
    direct: table0;
    min_width : 32;
}

control ingress {
    apply(table0);
    process_port_counters();
}