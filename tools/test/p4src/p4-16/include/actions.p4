#include "defines.p4"
#include "headers.p4"

action send_to_cpu(inout standard_metadata_t standard_metadata) {
    standard_metadata.egress_spec = CPU_PORT;
}

action set_egress_port(inout standard_metadata_t standard_metadata, Port port) {
    standard_metadata.egress_spec = port;
}

action drop(inout standard_metadata_t standard_metadata) {
    standard_metadata.egress_spec = DROP_PORT;
}
