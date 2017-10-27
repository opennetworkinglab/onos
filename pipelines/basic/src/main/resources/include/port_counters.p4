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

#ifndef __PORT_COUNTERS__
#define __PORT_COUNTERS__

#include "headers.p4"
#include "defines.p4"

control port_counters_ingress(inout headers_t hdr,
                              inout standard_metadata_t standard_metadata) {

    counter(MAX_PORTS, CounterType.packets) ingress_port_counter;

    apply {
        ingress_port_counter.count((bit<32>) standard_metadata.ingress_port);
    }
}

control port_counters_egress(inout headers_t hdr,
                             inout standard_metadata_t standard_metadata) {

    counter(MAX_PORTS, CounterType.packets) egress_port_counter;

    apply {
        egress_port_counter.count((bit<32>) standard_metadata.egress_port);
    }
}

#endif
