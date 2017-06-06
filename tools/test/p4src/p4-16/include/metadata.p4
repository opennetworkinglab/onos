#ifndef METADATA
#define METADATA

struct ecmp_metadata_t {
    bit<16> groupId;
    bit<16> selector;
}

struct wcmp_meta_t {
    bit<16> groupId;
    bit<8>  numBits;
    bit<64> selector;
}

struct metadata {
    ecmp_metadata_t ecmp_metadata;
    wcmp_meta_t wcmp_meta;
    intrinsic_metadata_t intrinsic_metadata;
}
#endif
