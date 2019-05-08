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

#ifndef METERS
#define METERS
#include "defines.p4"

control port_meters_ingress(inout headers_t hdr,
                            inout standard_metadata_t standard_metadata) {
    meter(MAX_PORTS, MeterType.bytes) ingress_port_meter;
    MeterColor ingress_color = MeterColor_GREEN;

    apply {
        ingress_port_meter.execute_meter<MeterColor>((bit<32>)standard_metadata.ingress_port, ingress_color);
        if (ingress_color == MeterColor_RED) {
            mark_to_drop(standard_metadata);
        } 
    }
}

control port_meters_egress(inout headers_t hdr,
                           inout standard_metadata_t standard_metadata) {

    meter(MAX_PORTS, MeterType.bytes) egress_port_meter;
    MeterColor egress_color = MeterColor_GREEN;

    apply {
        egress_port_meter.execute_meter<MeterColor>((bit<32>)standard_metadata.egress_port, egress_color);
        if (egress_color == MeterColor_RED) {
            mark_to_drop(standard_metadata);
        } 
    }
}
#endif
