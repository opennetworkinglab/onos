#ifndef CHECKSUMS
#define CHECKSUMS
#include "headers.p4"
#include "metadata.p4"

control verifyChecksum(in headers_t hdr, inout metadata_t meta) {
    apply {
        // Nothing to do
    }
}

control computeChecksum(inout headers_t hdr, inout metadata_t meta) {
    apply {
        // Nothing to do
    }
}

#endif
