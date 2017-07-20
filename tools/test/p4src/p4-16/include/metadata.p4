#ifndef METADATA
#define METADATA

struct ecmp_metadata_t {
    bit<16> group_id;
    bit<16> selector;
}

struct wcmp_metadata_t {
    bit<16> group_id;
    bit<8>  numBits;
    bit<64> selector;
}

struct metadata_t {
    ecmp_metadata_t ecmp_metadata;
    wcmp_metadata_t wcmp_meta;
    intrinsic_metadata_t intrinsic_metadata;
}
#endif
