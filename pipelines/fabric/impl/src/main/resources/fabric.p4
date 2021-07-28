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

#include <core.p4>
#include <v1model.p4>

#include "include/size.p4"
#include "include/control/filtering.p4"
#include "include/control/forwarding.p4"
#include "include/control/pre_next.p4"
#include "include/control/acl.p4"
#include "include/control/next.p4"
#include "include/control/packetio.p4"
#include "include/control/lookup_md_init.p4"
#include "include/control/slicing.p4"
#include "include/header.p4"
#include "include/checksum.p4"
#include "include/parser.p4"

#ifdef WITH_PORT_COUNTER
#include "include/control/port_counter.p4"
#endif // WITH_PORT_COUNTER

#ifdef WITH_SPGW
#include "include/control/spgw.p4"
#endif // WITH_SPGW

#ifdef WITH_BNG
#include "include/bng.p4"
#endif // WITH_BNG

#ifdef WITH_INT
#include "include/int/int_main.p4"
#endif // WITH_INT

control FabricIngress (inout parsed_headers_t hdr,
                       inout fabric_metadata_t fabric_metadata,
                       inout standard_metadata_t standard_metadata) {

    LookupMdInit() lkp_md_init;
    PacketIoIngress() pkt_io_ingress;
    Filtering() filtering;
    Forwarding() forwarding;
    PreNext() pre_next;
    Acl() acl;
    Next() next;
    IngressSliceTcClassifier() slice_tc_classifier;
    IngressQos() qos;
#ifdef WITH_PORT_COUNTER
    PortCountersControl() port_counters_control;
#endif // WITH_PORT_COUNTER
#ifdef WITH_SPGW
    SpgwIngress() spgw;
#endif // WITH_SPGW

    apply {
        _PRE_INGRESS
        lkp_md_init.apply(hdr, fabric_metadata.lkp);
        pkt_io_ingress.apply(hdr, fabric_metadata, standard_metadata);
        slice_tc_classifier.apply(hdr, fabric_metadata, standard_metadata);
        filtering.apply(hdr, fabric_metadata, standard_metadata);
#ifdef WITH_SPGW
        if (fabric_metadata.skip_forwarding == _FALSE) {
            spgw.apply(hdr, fabric_metadata, standard_metadata);
        }
#endif // WITH_SPGW
        if (fabric_metadata.skip_forwarding == _FALSE) {
            forwarding.apply(hdr, fabric_metadata, standard_metadata);
        }
        if (fabric_metadata.skip_next == _FALSE) {
            pre_next.apply(hdr, fabric_metadata);
        }
        acl.apply(hdr, fabric_metadata, standard_metadata);
        if (fabric_metadata.skip_next == _FALSE) {
            next.apply(hdr, fabric_metadata, standard_metadata);
#ifdef WITH_PORT_COUNTER
            // FIXME: we're not counting pkts punted to cpu or forwarded via
            // multicast groups. Remove when gNMI support will be there.
            port_counters_control.apply(hdr, fabric_metadata, standard_metadata);
#endif // WITH_PORT_COUNTER
#if defined(WITH_INT_SOURCE) || defined(WITH_INT_SINK)
            process_set_source_sink.apply(hdr, fabric_metadata, standard_metadata);
#endif
        }
#ifdef WITH_BNG
        bng_ingress.apply(hdr, fabric_metadata, standard_metadata);
#endif // WITH_BNG
        qos.apply(fabric_metadata, standard_metadata);
    }
}

control FabricEgress (inout parsed_headers_t hdr,
                      inout fabric_metadata_t fabric_metadata,
                      inout standard_metadata_t standard_metadata) {

    PacketIoEgress() pkt_io_egress;
    EgressNextControl() egress_next;
    EgressDscpRewriter() dscp_rewriter;
#ifdef WITH_SPGW
    SpgwEgress() spgw;
#endif // WITH_SPGW

    apply {
        _PRE_EGRESS
        pkt_io_egress.apply(hdr, fabric_metadata, standard_metadata);
        egress_next.apply(hdr, fabric_metadata, standard_metadata);
#ifdef WITH_SPGW
        spgw.apply(hdr, fabric_metadata);
#endif // WITH_SPGW
#ifdef WITH_BNG
        bng_egress.apply(hdr, fabric_metadata, standard_metadata);
#endif // WITH_BNG
#ifdef WITH_INT
        process_int_main.apply(hdr, fabric_metadata, standard_metadata);
#endif
    dscp_rewriter.apply(hdr, fabric_metadata, standard_metadata);
    }
}

V1Switch(
    FabricParser(),
    FabricVerifyChecksum(),
    FabricIngress(),
    FabricEgress(),
    FabricComputeChecksum(),
    FabricDeparser()
) main;
