#ifndef PARSERS
#define PARSERS
#include "headers.p4"
#include "metadata.p4"

#define ETH_TYPE_IPV4 16w0x0800
#define IP_TYPE_TCP 8w6
#define IP_TYPE_UDP 8w17

parser ParserImpl(packet_in packet, out headers hdr, inout metadata meta, inout standard_metadata_t standard_metadata) {
    state parse_ethernet {
        packet.extract(hdr.ethernet);
        transition select(hdr.ethernet.etherType) {
            ETH_TYPE_IPV4: parse_ipv4;
            default: accept;
        }
    }

    state parse_ipv4 {
        packet.extract(hdr.ipv4);
        transition select(hdr.ipv4.protocol) {
            IP_TYPE_TCP: parse_tcp;
            IP_TYPE_UDP: parse_udp;
            default: accept;
        }
    }

    state parse_tcp {
        packet.extract(hdr.tcp);
        transition accept;
    }

    state parse_udp {
        packet.extract(hdr.udp);
        transition accept;
    }

    state start {
        transition parse_ethernet;
    }
}

control DeparserImpl(packet_out packet, in headers hdr) {
    apply {
        packet.emit(hdr.ethernet);
        packet.emit(hdr.ipv4);
        packet.emit(hdr.udp);
        packet.emit(hdr.tcp);
    }
}
#endif
