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

#ifndef __HOST_METER_TABLE__
#define __HOST_METER_TABLE__

#include "headers.p4"
#include "defines.p4"

control host_meter_control(inout headers_t hdr,
                           inout local_metadata_t local_metadata,
                           inout standard_metadata_t standard_metadata) {
    MeterColor meter_tag = MeterColor_GREEN;
    direct_meter<MeterColor>(MeterType.bytes) host_meter;

    action read_meter() {
        host_meter.read(meter_tag);
    }

    table host_meter_table {
        key = {
            hdr.ethernet.src_addr   : lpm;
        }
        actions = {
            read_meter();
            NoAction;
        }
        meters = host_meter;
        default_action = NoAction();
    }

    apply {
        host_meter_table.apply();
        if (meter_tag == MeterColor_RED) {
            mark_to_drop(standard_metadata);
        }
     }
}

#endif
