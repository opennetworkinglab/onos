#ifndef CHECK_SUMS
#define CHECK_SUMS
#include "headers.p4"
#include "metadata.p4"

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
#endif
