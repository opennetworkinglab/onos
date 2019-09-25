/*
 * Copyright 2018-present Open Networking Foundation
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

/* -*- P4_16 -*- */
#ifndef __INT_MAIN__
#define __INT_MAIN__

#ifdef WITH_INT_SOURCE
#include "int_source.p4"
#endif // WITH_INT_SOURCE

#ifdef WITH_INT_TRANSIT
#include "int_transit.p4"
#endif // WITH_INT_TRANSIT

#ifdef WITH_INT_SINK
#include "int_sink.p4"
#include "int_report.p4"
#endif // WITH_INT_SINK

control process_set_source_sink (
    inout parsed_headers_t hdr,
    inout fabric_metadata_t fabric_metadata,
    inout standard_metadata_t standard_metadata) {

    direct_counter(CounterType.packets_and_bytes) counter_set_source;

    action int_set_source () {
        fabric_metadata.int_meta.source = _TRUE;
        counter_set_source.count();
    }

    table tb_set_source {
        key = {
            standard_metadata.ingress_port: exact @name("ig_port");
        }
        actions = {
            int_set_source;
            @defaultonly nop();
        }
        const default_action = nop();
        counters = counter_set_source;
        size = MAX_PORTS;
    }

#ifdef WITH_INT_SINK
    direct_counter(CounterType.packets_and_bytes) counter_set_sink;

    action int_set_sink () {
        fabric_metadata.int_meta.sink = _TRUE;
        counter_set_sink.count();
    }

    table tb_set_sink {
        key = {
            standard_metadata.egress_spec: exact @name("eg_spec");
        }
        actions = {
            int_set_sink;
            @defaultonly nop();
        }
        const default_action = nop();
        counters = counter_set_sink;
        size = MAX_PORTS;
    }
#endif // WITH_INT_SINK

    apply {
        tb_set_source.apply();

#ifdef WITH_INT_SINK
        tb_set_sink.apply();
        if(fabric_metadata.int_meta.sink == _TRUE) {
            // FIXME: this works only on BMv2
            #ifdef __TARGET_BMV2__
            clone(CloneType.I2E, REPORT_MIRROR_SESSION_ID);
            #endif
        }
#endif // WITH_INT_SINK
    }
}

control process_int_main (
    inout parsed_headers_t hdr,
    inout fabric_metadata_t fabric_metadata,
    inout standard_metadata_t standard_metadata) {

    apply {
        if (standard_metadata.ingress_port != CPU_PORT &&
            standard_metadata.egress_port != CPU_PORT &&
            (hdr.udp.isValid() || hdr.tcp.isValid())) {
#ifdef WITH_INT_SOURCE
            if (fabric_metadata.int_meta.source == _TRUE) {
                process_int_source.apply(hdr, fabric_metadata, standard_metadata);
            }
#endif // WITH_INT_SOURCE
            if(hdr.int_header.isValid()) {
#ifdef WITH_INT_TRANSIT
                process_int_transit.apply(hdr, fabric_metadata, standard_metadata);
#endif // WITH_INT_TRANSIT
#ifdef WITH_INT_SINK
                if (standard_metadata.instance_type == PKT_INSTANCE_TYPE_INGRESS_CLONE) {
                    /* send int report */
                    process_int_report.apply(hdr, fabric_metadata, standard_metadata);
                }
                if (fabric_metadata.int_meta.sink == _TRUE) {
                    // int sink
                    process_int_sink.apply(hdr, fabric_metadata);
                }
#endif // WITH_INT_SINK
            }
        }
    }
}
#endif
