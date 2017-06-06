header_type dummy_t {
    fields {
        dummyField : 8;
    }
}

metadata dummy_t dummy_metadata;

parser start {
    return ingress;
}

table table0 {
    reads {
        dummy_metadata.dummyField : exact;
    }
    actions {
    	dummy_action;
    }
}

action dummy_action() {
    modify_field(dummy_metadata.dummyField, 1);
}

control ingress {
	apply(table0);
}