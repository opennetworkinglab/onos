#ifndef PACKET_IO
#define PACKET_IO

control PacketIoIngressControl(inout headers_t hdr, inout standard_metadata_t standard_metadata) {
    apply {
        if (hdr.packet_out.isValid()) {
            standard_metadata.egress_spec = hdr.packet_out.egress_port;
        }
    }
}

control PacketIoEgressControl(inout headers_t hdr, inout standard_metadata_t standard_metadata) {
    apply {
        hdr.packet_out.setInvalid();
        if (standard_metadata.egress_spec == CPU_PORT) {
            hdr.packet_in.setValid();
            hdr.packet_in.ingress_port = standard_metadata.ingress_port;
        }
    }
}

#endif