#ifndef HEADERS
#define HEADERS

@controller_header("packet_in")
header packet_in_header_t {
    bit<9> ingress_port;
}

@controller_header("packet_out")
header packet_out_header_t {
    bit<9> egress_port;
}

struct intrinsic_metadata_t {
    bit<32> ingress_global_timestamp;
    bit<32> lf_field_list;
    bit<16> mcast_grp;
    bit<16> egress_rid;
}

header ethernet_t {
    bit<48> dstAddr;
    bit<48> srcAddr;
    bit<16> etherType;
}

header ipv4_t {
    bit<4>  version;
    bit<4>  ihl;
    bit<8>  diffserv;
    bit<16> totalLen;
    bit<16> identification;
    bit<3>  flags;
    bit<13> fragOffset;
    bit<8>  ttl;
    bit<8>  protocol;
    bit<16> hdrChecksum;
    bit<32> srcAddr;
    bit<32> dstAddr;
}

header tcp_t {
    bit<16> srcPort;
    bit<16> dstPort;
    bit<32> seqNo;
    bit<32> ackNo;
    bit<4>  dataOffset;
    bit<3>  res;
    bit<3>  ecn;
    bit<6>  ctrl;
    bit<16> window;
    bit<16> checksum;
    bit<16> urgentPtr;
}

header udp_t {
    bit<16> srcPort;
    bit<16> dstPort;
    bit<16> length_;
    bit<16> checksum;
}

struct headers_t {
    ethernet_t ethernet;
    ipv4_t ipv4;
    tcp_t tcp;
    udp_t udp;
    packet_out_header_t packet_out;
    packet_in_header_t packet_in;
}
#endif
