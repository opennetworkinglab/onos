action set_egress_port(port) {
    modify_field(standard_metadata.egress_spec, port);
}

action drop() {
    modify_field(standard_metadata.egress_spec, DROP_PORT);
}

action send_to_cpu() {
    modify_field(standard_metadata.egress_spec, CPU_PORT);
}