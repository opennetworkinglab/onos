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

#ifndef __PORT_COUNTER__
#define __PORT_COUNTER__
#include "../define.p4"
#include "../header.p4"

control PortCountersControl(inout parsed_headers_t hdr,
                            inout fabric_metadata_t fabric_metadata,
                            inout standard_metadata_t standard_metadata) {

    counter(MAX_PORTS, CounterType.packets_and_bytes) egress_port_counter;
    counter(MAX_PORTS, CounterType.packets_and_bytes) ingress_port_counter;

    apply {
        if (standard_metadata.egress_spec < MAX_PORTS) {
            egress_port_counter.count((bit<32>)standard_metadata.egress_spec);
        }
        if (standard_metadata.ingress_port < MAX_PORTS) {
            ingress_port_counter.count((bit<32>)standard_metadata.ingress_port);
        }
    }
}
#endif
