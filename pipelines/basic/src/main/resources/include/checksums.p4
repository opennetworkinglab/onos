/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef __CHECKSUMS__
#define __CHECKSUMS__

#include "headers.p4"

control verify_checksum_control(inout headers_t hdr,
                                inout local_metadata_t local_metadata) {
    apply {
        // Assume checksum is always correct.
    }
}

control compute_checksum_control(inout headers_t hdr,
                                 inout local_metadata_t local_metadata) {
    apply {
        // No need to recompute.
    }
}

#endif
