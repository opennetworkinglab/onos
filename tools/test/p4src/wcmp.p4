#include "include/defines.p4"
#include "include/headers.p4"
#include "include/parser.p4"
#include "include/actions.p4"
#include "include/port_counters.p4"

#define SELECTOR_WIDTH 64

header_type wcmp_meta_t {
    fields {
        groupId : 16;
        numBits: 8;
        selector : SELECTOR_WIDTH;
    }
}

metadata wcmp_meta_t wcmp_meta;

field_list wcmp_hash_fields {
    ipv4.srcAddr;
    ipv4.dstAddr;
    ipv4.protocol;
    tcp.srcPort;
    tcp.dstPort;
    udp.srcPort;
    udp.dstPort;
}

field_list_calculation wcmp_hash {
    input {
        wcmp_hash_fields;
    }
    algorithm : bmv2_hash;
    output_width : 64;
}

action wcmp_group(groupId) {
    modify_field(wcmp_meta.groupId, groupId);
    modify_field_with_hash_based_offset(wcmp_meta.numBits, 2, wcmp_hash, (SELECTOR_WIDTH - 2));
}

action wcmp_set_selector() {
    modify_field(wcmp_meta.selector,
                 (((1 << wcmp_meta.numBits) - 1) << (SELECTOR_WIDTH - wcmp_meta.numBits)));
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
        wcmp_group;
        send_to_cpu;
        _drop;
    }
    support_timeout: true;
}

table wcmp_set_selector_table {
    actions {
        wcmp_set_selector;
    }
}

table wcmp_group_table {
    reads {
        wcmp_meta.groupId : exact;
        wcmp_meta.selector : lpm;
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

counter wcmp_group_table_counter {
    type: packets;
    direct: wcmp_group_table;
    min_width : 32;
}

control ingress {
    apply(table0) {
        wcmp_group {
            apply(wcmp_set_selector_table) {
                wcmp_set_selector {
                    apply(wcmp_group_table);
                }
            }
        }
    }
    process_port_counters();
}