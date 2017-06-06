#include "include/defines.p4"
#include "include/headers.p4"
#include "include/parser.p4"
#include "include/actions.p4"
#include "include/port_counters.p4"

header_type ecmp_metadata_t {
    fields {
        groupId : 16;
        selector : 16;
    }
}

metadata ecmp_metadata_t ecmp_metadata;

field_list ecmp_hash_fields {
    ipv4.srcAddr;
    ipv4.dstAddr;
    ipv4.protocol;
    tcp.srcPort;
    tcp.dstPort;
    udp.srcPort;
    udp.dstPort;
}

field_list_calculation ecmp_hash {
    input {
        ecmp_hash_fields;
    }
    algorithm : bmv2_hash;
    output_width : 64;
}

action ecmp_group(groupId, groupSize) {
    modify_field(ecmp_metadata.groupId, groupId);
    modify_field_with_hash_based_offset(ecmp_metadata.selector, 0, ecmp_hash, groupSize);
}

table table0 {
    reads {
        standard_metadata.ingress_port : ternary;
        ethernet.dstAddr : ternary;
        ethernet.srcAddr : ternary;
        ethernet.etherType : ternary;
    }
    actions {
        set_egress_port;
        ecmp_group;
        send_to_cpu;
        _drop;
    }
    support_timeout: true;
}

table ecmp_group_table {
    reads {
        ecmp_metadata.groupId : exact;
        ecmp_metadata.selector : exact;
    }
    actions {
        set_egress_port;
    }
}

counter table0_counter {
    type: packets;
    direct: table0;
    min_width : 32;
}

counter ecmp_group_table_counter {
    type: packets;
    direct: ecmp_group_table;
    min_width : 32;
}

control ingress {
    apply(table0) {
        ecmp_group {
            apply(ecmp_group_table);
        }
    }
    process_port_counters();
}