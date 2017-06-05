counter ingress_port_counter {
    type : packets; // bmv2 always counts both bytes and packets 
    instance_count : MAX_PORTS;
    min_width : 32;
}

counter egress_port_counter {
    type: packets;
    instance_count : MAX_PORTS;
    min_width : 32;
}

table port_count_table {
    actions {
        count_packet;
    }
}

action count_packet() {
    count(ingress_port_counter, standard_metadata.ingress_port);
    count(egress_port_counter, standard_metadata.egress_spec);
}

control process_port_counters {
	// Avoid counting logical ports, such as drop and cpu
	if (standard_metadata.egress_spec < MAX_PORTS) {
		apply(port_count_table);
	}
}