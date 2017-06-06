#include <core.p4>
#include <v1model.p4>

struct dummy_t {
    bit<8> dummyField;
}

struct metadata {
    dummy_t dummy_metadata;
}

struct headers {
}

parser ParserImpl(packet_in packet, out headers hdr, inout metadata meta, inout standard_metadata_t standard_metadata) {
    state start {
        transition accept;
    }
}

control ingress(inout headers hdr, inout metadata meta, inout standard_metadata_t standard_metadata) {
    action dummy_action() {
        meta.dummy_metadata.dummyField = 8w1;
    }
    table table0 {
        actions = {
            dummy_action;
        }
        key = {
            meta.dummy_metadata.dummyField: exact;
        }
    }
    apply {
        table0.apply();
    }
}

control egress(inout headers hdr, inout metadata meta, inout standard_metadata_t standard_metadata) {
    apply {
        // Nothing to do
    }
}

control DeparserImpl(packet_out packet, in headers hdr) {
    apply {
        // Nothing to do
    }
}

control verifyChecksum(in headers hdr, inout metadata meta) {
    apply {
        // Nothing to do
    }
}

control computeChecksum(inout headers hdr, inout metadata meta) {
    apply {
        // Nothing to do
    }
}

V1Switch(ParserImpl(), verifyChecksum(), ingress(), egress(), computeChecksum(), DeparserImpl()) main;
