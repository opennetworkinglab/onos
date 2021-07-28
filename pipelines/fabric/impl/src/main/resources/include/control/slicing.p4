/*
 * Copyright 2021-present Open Networking Foundation
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

#ifndef __SLICING__
#define __SLICING__

// ACL-like classification, maps lookup metadata to slice_id and tc. For UE
// traffic, values can be overriden later by the SPGW PDR tables.
// To apply the same slicing and QoS treatment end-to-end, we use the IPv4 DSCP
// field to piggyback slice_id and tc (see EgressDscpRewriter). This is
// especially important for UE traffic, where classification based on PDRs can
// only happen at the ingress leaf switch (implementing the UPF function).
// As such, for traffic coming from selected ports, we allow trusting the
// slice_id and tc values carried in the dscp.
control IngressSliceTcClassifier (in parsed_headers_t hdr,
                                  inout fabric_metadata_t fabric_md,
                                  in standard_metadata_t standard_metadata) {

    direct_counter(CounterType.packets) classifier_stats;

    action set_slice_id_tc(slice_id_t slice_id, tc_t tc) {
        fabric_md.slice_id = slice_id;
        fabric_md.tc = tc;
        classifier_stats.count();
    }

    // Should be used only for infrastructure ports (leaf-leaf, or leaf-spine),
    // or ports facing servers that implement early classification based on the
    // SDFAB DSCP encoding (slice_id++tc).
    action trust_dscp() {
        fabric_md.slice_id = hdr.ipv4.dscp[SLICE_ID_WIDTH+TC_WIDTH-1:TC_WIDTH];
        fabric_md.tc = hdr.ipv4.dscp[TC_WIDTH-1:0];
        classifier_stats.count();
    }

    table classifier {
        key = {
            standard_metadata.ingress_port  : ternary @name("ig_port");
            fabric_md.lkp.ipv4_src          : ternary @name("ipv4_src");
            fabric_md.lkp.ipv4_dst          : ternary @name("ipv4_dst");
            fabric_md.lkp.ip_proto          : ternary @name("ip_proto");
            fabric_md.lkp.l4_sport          : ternary @name("l4_sport");
            fabric_md.lkp.l4_dport          : ternary @name("l4_dport");
        }
        actions = {
            set_slice_id_tc;
            trust_dscp;
        }
        const default_action = set_slice_id_tc(DEFAULT_SLICE_ID, DEFAULT_TC);
        counters = classifier_stats;
        size = QOS_CLASSIFIER_TABLE_SIZE;
    }

    apply {
        classifier.apply();
    }
}

// Provides metering and mapping to queues based on slice_id and tc. Should be
// applied after any other block writing slice_id and tc.
control IngressQos (inout fabric_metadata_t fabric_md,
                    inout standard_metadata_t standard_metadata) {

    // One meter per tc per slice. The index should be slice_id++tc.
    meter(1 << SLICE_TC_WIDTH, MeterType.bytes) slice_tc_meter;

    direct_counter(CounterType.packets) queues_stats;

    action set_queue(qid_t qid) {
        // We can't set the queue id in bmv2.
        queues_stats.count();
    }

    // For policing.
    action meter_drop() {
        mark_to_drop(standard_metadata);
        queues_stats.count();
    }

    table queues {
        key = {
            fabric_md.slice_id:     exact   @name("slice_id");
            fabric_md.tc:           exact   @name("tc");
            fabric_md.packet_color: ternary @name("color"); // 0=GREEN, 1=YELLOW, 2=RED
        }
        actions = {
            set_queue;
            meter_drop;
        }
        const default_action = set_queue(0); // 0 = Best Effort
        counters = queues_stats;
        // Two times the number of tcs for all slices, because we might need to
        // match on different colors for the same slice and tc.
        size = 1 << (SLICE_TC_WIDTH + 1);
    }

    slice_tc_t slice_tc = fabric_md.slice_id++fabric_md.tc;

    apply {
        // Meter index should be 0 for all packets with default slice_id and tc.
        slice_tc_meter.execute_meter((bit<32>) slice_tc, fabric_md.packet_color);
        fabric_md.dscp = slice_tc;
        queues.apply();
    }
}

// Allows per-egress port rewriting of the outermost IPv4 DSCP field to
// piggyback slice_id and tc across the fabric.
control EgressDscpRewriter (inout parsed_headers_t hdr,
                            in fabric_metadata_t fabric_md,
                            in standard_metadata_t standard_metadata) {

    bit<6> tmp_dscp = fabric_md.dscp;

    action rewrite() {
        // Do nothing, tmp_dscp is already initialized.
    }

    // Sets the DSCP field to zero. Should be used for edge ports facing devices
    // that do not support the SDFAB DSCP encoding.
    action clear() {
        tmp_dscp = 0;
    }

    table rewriter {
        key = {
            standard_metadata.egress_port : exact @name("eg_port");
        }
        actions = {
            rewrite;
            clear;
            @defaultonly nop;
        }
        const default_action = nop;
        size = DSCP_REWRITER_TABLE_SIZE;
    }

    apply {
        if (rewriter.apply().hit) {
#ifdef WITH_SPGW
            if (hdr.gtpu_ipv4.isValid()) {
                hdr.ipv4.dscp = tmp_dscp;
            } else
#endif // WITH_SPGW
            if (hdr.ipv4.isValid()) {
                hdr.inner_ipv4.dscp = tmp_dscp;
            }
        }
    }
}

#endif
