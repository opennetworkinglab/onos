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

package org.onosproject.pipelines.basic;

import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiPacketMetadataId;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiTableId;
/**
 * Constants for INT pipeline.
 */
public final class IntConstants {

    // hide default constructor
    private IntConstants() {
    }

    // Header field IDs
    public static final PiMatchFieldId HDR_HDR_IPV4_PROTOCOL =
            PiMatchFieldId.of("hdr.ipv4.protocol");
    public static final PiMatchFieldId HDR_STANDARD_METADATA_EGRESS_SPEC =
            PiMatchFieldId.of("standard_metadata.egress_spec");
    public static final PiMatchFieldId HDR_HDR_IPV4_SRC_ADDR =
            PiMatchFieldId.of("hdr.ipv4.src_addr");
    public static final PiMatchFieldId HDR_HDR_ETHERNET_ETHER_TYPE =
            PiMatchFieldId.of("hdr.ethernet.ether_type");
    public static final PiMatchFieldId HDR_HDR_ETHERNET_SRC_ADDR =
            PiMatchFieldId.of("hdr.ethernet.src_addr");
    public static final PiMatchFieldId HDR_LOCAL_METADATA_L4_DST_PORT =
            PiMatchFieldId.of("local_metadata.l4_dst_port");
    public static final PiMatchFieldId HDR_LOCAL_METADATA_L4_SRC_PORT =
            PiMatchFieldId.of("local_metadata.l4_src_port");
    public static final PiMatchFieldId HDR_STANDARD_METADATA_INGRESS_PORT =
            PiMatchFieldId.of("standard_metadata.ingress_port");
    public static final PiMatchFieldId HDR_INT_IS_VALID =
            PiMatchFieldId.of("int_is_valid");
    public static final PiMatchFieldId HDR_HDR_IPV4_DST_ADDR =
            PiMatchFieldId.of("hdr.ipv4.dst_addr");
    public static final PiMatchFieldId HDR_HDR_ETHERNET_DST_ADDR =
            PiMatchFieldId.of("hdr.ethernet.dst_addr");
    // Table IDs
    public static final PiTableId INGRESS_PROCESS_INT_SOURCE_TB_INT_SOURCE =
            PiTableId.of("ingress.process_int_source.tb_int_source");
    public static final PiTableId EGRESS_PROCESS_INT_REPORT_TB_GENERATE_REPORT =
            PiTableId.of("egress.process_int_report.tb_generate_report");
    public static final PiTableId INGRESS_TABLE0_CONTROL_TABLE0 =
            PiTableId.of("ingress.table0_control.table0");
    public static final PiTableId EGRESS_PROCESS_INT_TRANSIT_TB_INT_INSERT =
            PiTableId.of("egress.process_int_transit.tb_int_insert");
    public static final PiTableId INGRESS_PROCESS_INT_SOURCE_SINK_TB_SET_SINK =
            PiTableId.of("ingress.process_int_source_sink.tb_set_sink");
    public static final PiTableId INGRESS_PROCESS_INT_SOURCE_SINK_TB_SET_SOURCE =
            PiTableId.of("ingress.process_int_source_sink.tb_set_source");
    // Indirect Counter IDs
    public static final PiCounterId EGRESS_PORT_COUNTERS_EGRESS_EGRESS_PORT_COUNTER =
            PiCounterId.of("egress.port_counters_egress.egress_port_counter");
    public static final PiCounterId INGRESS_PORT_COUNTERS_INGRESS_INGRESS_PORT_COUNTER =
            PiCounterId.of("ingress.port_counters_ingress.ingress_port_counter");
    // Direct Counter IDs
    public static final PiCounterId INGRESS_PROCESS_INT_SOURCE_SINK_COUNTER_SET_SINK =
            PiCounterId.of("ingress.process_int_source_sink.counter_set_sink");
    public static final PiCounterId INGRESS_TABLE0_CONTROL_TABLE0_COUNTER =
            PiCounterId.of("ingress.table0_control.table0_counter");
    public static final PiCounterId INGRESS_PROCESS_INT_SOURCE_COUNTER_INT_SOURCE =
            PiCounterId.of("ingress.process_int_source.counter_int_source");
    public static final PiCounterId INGRESS_PROCESS_INT_SOURCE_SINK_COUNTER_SET_SOURCE =
            PiCounterId.of("ingress.process_int_source_sink.counter_set_source");
    // Action IDs
    public static final PiActionId INGRESS_PROCESS_INT_SOURCE_INT_SOURCE_DSCP =
            PiActionId.of("ingress.process_int_source.int_source_dscp");
    public static final PiActionId EGRESS_PROCESS_INT_REPORT_DO_REPORT_ENCAPSULATION =
            PiActionId.of("egress.process_int_report.do_report_encapsulation");
    public static final PiActionId INGRESS_TABLE0_CONTROL_SEND_TO_CPU =
            PiActionId.of("ingress.table0_control.send_to_cpu");
    public static final PiActionId INGRESS_PROCESS_INT_SOURCE_SINK_INT_SET_SOURCE =
            PiActionId.of("ingress.process_int_source_sink.int_set_source");
    public static final PiActionId NO_ACTION = PiActionId.of("NoAction");
    public static final PiActionId INGRESS_PROCESS_INT_SOURCE_SINK_INT_SET_SINK =
            PiActionId.of("ingress.process_int_source_sink.int_set_sink");
    public static final PiActionId INGRESS_TABLE0_CONTROL_SET_NEXT_HOP_ID =
            PiActionId.of("ingress.table0_control.set_next_hop_id");
    public static final PiActionId INGRESS_TABLE0_CONTROL_SET_EGRESS_PORT =
            PiActionId.of("ingress.table0_control.set_egress_port");
    public static final PiActionId EGRESS_PROCESS_INT_TRANSIT_INIT_METADATA =
            PiActionId.of("egress.process_int_transit.init_metadata");
    public static final PiActionId NOP = PiActionId.of("nop");
    // Action Param IDs
    public static final PiActionParamId INS_MASK0407 =
            PiActionParamId.of("ins_mask0407");
    public static final PiActionParamId NEXT_HOP_ID =
            PiActionParamId.of("next_hop_id");
    public static final PiActionParamId MON_PORT =
            PiActionParamId.of("mon_port");
    public static final PiActionParamId MON_MAC = PiActionParamId.of("mon_mac");
    public static final PiActionParamId MON_IP = PiActionParamId.of("mon_ip");
    public static final PiActionParamId SWITCH_ID =
            PiActionParamId.of("switch_id");
    public static final PiActionParamId SRC_MAC = PiActionParamId.of("src_mac");
    public static final PiActionParamId INS_MASK0003 =
            PiActionParamId.of("ins_mask0003");
    public static final PiActionParamId REMAINING_HOP_CNT =
            PiActionParamId.of("remaining_hop_cnt");
    public static final PiActionParamId HOP_METADATA_LEN =
            PiActionParamId.of("hop_metadata_len");
    public static final PiActionParamId SRC_IP = PiActionParamId.of("src_ip");
    public static final PiActionParamId PORT = PiActionParamId.of("port");
    // Packet Metadata IDs
    public static final PiPacketMetadataId INGRESS_PORT =
            PiPacketMetadataId.of("ingress_port");
    public static final PiPacketMetadataId EGRESS_PORT =
            PiPacketMetadataId.of("egress_port");
}
