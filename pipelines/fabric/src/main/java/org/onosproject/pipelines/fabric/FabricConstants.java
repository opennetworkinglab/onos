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

package org.onosproject.pipelines.fabric;

import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiControlMetadataId;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiTableId;
/**
 * Constants for fabric pipeline.
 */
public final class FabricConstants {

    // hide default constructor
    private FabricConstants() {
    }

    // Header field IDs
    public static final PiMatchFieldId HDR_VLAN_TAG_VLAN_ID =
            PiMatchFieldId.of("hdr.vlan_tag.vlan_id");
    public static final PiMatchFieldId HDR_MPLS_LABEL =
            PiMatchFieldId.of("hdr.mpls.label");
    public static final PiMatchFieldId STANDARD_METADATA_EGRESS_PORT =
            PiMatchFieldId.of("standard_metadata.egress_port");
    public static final PiMatchFieldId STANDARD_METADATA_INGRESS_PORT =
            PiMatchFieldId.of("standard_metadata.ingress_port");
    public static final PiMatchFieldId HDR_VLAN_TAG_IS_VALID =
            PiMatchFieldId.of("hdr.vlan_tag.is_valid");
    public static final PiMatchFieldId HDR_ICMP_ICMP_CODE =
            PiMatchFieldId.of("hdr.icmp.icmp_code");
    public static final PiMatchFieldId HDR_INT_HEADER_IS_VALID =
            PiMatchFieldId.of("hdr.int_header.is_valid");
    public static final PiMatchFieldId HDR_ETHERNET_SRC_ADDR =
            PiMatchFieldId.of("hdr.ethernet.src_addr");
    public static final PiMatchFieldId HDR_ICMP_ICMP_TYPE =
            PiMatchFieldId.of("hdr.icmp.icmp_type");
    public static final PiMatchFieldId HDR_VLAN_TAG_ETHER_TYPE =
            PiMatchFieldId.of("hdr.vlan_tag.ether_type");
    public static final PiMatchFieldId HDR_IPV4_DST_ADDR =
            PiMatchFieldId.of("hdr.ipv4.dst_addr");
    public static final PiMatchFieldId HDR_INT_HEADER_INSTRUCTION_MASK_0003 =
            PiMatchFieldId.of("hdr.int_header.instruction_mask_0003");
    public static final PiMatchFieldId FABRIC_METADATA_L4_SRC_PORT =
            PiMatchFieldId.of("fabric_metadata.l4_src_port");
    public static final PiMatchFieldId FABRIC_METADATA_L4_DST_PORT =
            PiMatchFieldId.of("fabric_metadata.l4_dst_port");
    public static final PiMatchFieldId STANDARD_METADATA_EGRESS_SPEC =
            PiMatchFieldId.of("standard_metadata.egress_spec");
    public static final PiMatchFieldId GTPU_IPV4_DST_ADDR =
            PiMatchFieldId.of("gtpu_ipv4.dst_addr");
    public static final PiMatchFieldId FABRIC_METADATA_IP_PROTO =
            PiMatchFieldId.of("fabric_metadata.ip_proto");
    public static final PiMatchFieldId FABRIC_METADATA_NEXT_ID =
            PiMatchFieldId.of("fabric_metadata.next_id");
    public static final PiMatchFieldId HDR_IPV4_SRC_ADDR =
            PiMatchFieldId.of("hdr.ipv4.src_addr");
    public static final PiMatchFieldId HDR_INT_HEADER_INSTRUCTION_MASK_0407 =
            PiMatchFieldId.of("hdr.int_header.instruction_mask_0407");
    public static final PiMatchFieldId HDR_IPV6_DST_ADDR =
            PiMatchFieldId.of("hdr.ipv6.dst_addr");
    public static final PiMatchFieldId IPV4_DST_ADDR =
            PiMatchFieldId.of("ipv4.dst_addr");
    public static final PiMatchFieldId HDR_ETHERNET_DST_ADDR =
            PiMatchFieldId.of("hdr.ethernet.dst_addr");
    // Table IDs
    public static final PiTableId FABRIC_INGRESS_FORWARDING_ACL =
            PiTableId.of("FabricIngress.forwarding.acl");
    public static final PiTableId FABRIC_INGRESS_NEXT_HASHED =
            PiTableId.of("FabricIngress.next.hashed");
    public static final PiTableId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_SOURCE_TB_INT_SOURCE =
            PiTableId.of("FabricEgress.process_int_main.process_int_source.tb_int_source");
    public static final PiTableId FABRIC_INGRESS_FORWARDING_MPLS =
            PiTableId.of("FabricIngress.forwarding.mpls");
    public static final PiTableId FABRIC_INGRESS_PROCESS_SET_SOURCE_SINK_TB_SET_SINK =
            PiTableId.of("FabricIngress.process_set_source_sink.tb_set_sink");
    public static final PiTableId FABRIC_INGRESS_FORWARDING_ROUTING_V4 =
            PiTableId.of("FabricIngress.forwarding.routing_v4");
    public static final PiTableId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_TB_INT_INST_0407 =
            PiTableId.of("FabricEgress.process_int_main.process_int_transit.tb_int_inst_0407");
    public static final PiTableId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_TB_INT_INSERT =
            PiTableId.of("FabricEgress.process_int_main.process_int_transit.tb_int_insert");
    public static final PiTableId FABRIC_INGRESS_NEXT_SIMPLE =
            PiTableId.of("FabricIngress.next.simple");
    public static final PiTableId FABRIC_INGRESS_FILTERING_FWD_CLASSIFIER =
            PiTableId.of("FabricIngress.filtering.fwd_classifier");
    public static final PiTableId FABRIC_INGRESS_PROCESS_SET_SOURCE_SINK_TB_SET_SOURCE =
            PiTableId.of("FabricIngress.process_set_source_sink.tb_set_source");
    public static final PiTableId FABRIC_INGRESS_FORWARDING_BRIDGING =
            PiTableId.of("FabricIngress.forwarding.bridging");
    public static final PiTableId FABRIC_INGRESS_SPGW_INGRESS_S1U_FILTER_TABLE =
            PiTableId.of("FabricIngress.spgw_ingress.s1u_filter_table");
    public static final PiTableId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_TB_INT_INST_0003 =
            PiTableId.of("FabricEgress.process_int_main.process_int_transit.tb_int_inst_0003");
    public static final PiTableId FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN =
            PiTableId.of("FabricIngress.filtering.ingress_port_vlan");
    public static final PiTableId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_REPORT_TB_GENERATE_REPORT =
            PiTableId.of("FabricEgress.process_int_main.process_int_report.tb_generate_report");
    public static final PiTableId FABRIC_INGRESS_SPGW_INGRESS_DL_SESS_LOOKUP =
            PiTableId.of("FabricIngress.spgw_ingress.dl_sess_lookup");
    public static final PiTableId FABRIC_EGRESS_EGRESS_NEXT_EGRESS_VLAN =
            PiTableId.of("FabricEgress.egress_next.egress_vlan");
    public static final PiTableId FABRIC_INGRESS_NEXT_MULTICAST =
            PiTableId.of("FabricIngress.next.multicast");
    public static final PiTableId FABRIC_INGRESS_NEXT_VLAN_META =
            PiTableId.of("FabricIngress.next.vlan_meta");
    public static final PiTableId FABRIC_INGRESS_FORWARDING_ROUTING_V6 =
            PiTableId.of("FabricIngress.forwarding.routing_v6");
    // Indirect Counter IDs
    public static final PiCounterId FABRIC_INGRESS_PORT_COUNTERS_CONTROL_EGRESS_PORT_COUNTER =
            PiCounterId.of("FabricIngress.port_counters_control.egress_port_counter");
    public static final PiCounterId FABRIC_INGRESS_PORT_COUNTERS_CONTROL_INGRESS_PORT_COUNTER =
            PiCounterId.of("FabricIngress.port_counters_control.ingress_port_counter");
    // Direct Counter IDs
    public static final PiCounterId FABRIC_INGRESS_FORWARDING_ACL_COUNTER =
            PiCounterId.of("FabricIngress.forwarding.acl_counter");
    public static final PiCounterId FABRIC_INGRESS_NEXT_MULTICAST_COUNTER =
            PiCounterId.of("FabricIngress.next.multicast_counter");
    public static final PiCounterId FABRIC_INGRESS_NEXT_VLAN_META_COUNTER =
            PiCounterId.of("FabricIngress.next.vlan_meta_counter");
    public static final PiCounterId FABRIC_INGRESS_FILTERING_FWD_CLASSIFIER_COUNTER =
            PiCounterId.of("FabricIngress.filtering.fwd_classifier_counter");
    public static final PiCounterId FABRIC_INGRESS_FORWARDING_BRIDGING_COUNTER =
            PiCounterId.of("FabricIngress.forwarding.bridging_counter");
    public static final PiCounterId FABRIC_INGRESS_FORWARDING_ROUTING_V4_COUNTER =
            PiCounterId.of("FabricIngress.forwarding.routing_v4_counter");
    public static final PiCounterId FABRIC_INGRESS_PROCESS_SET_SOURCE_SINK_COUNTER_SET_SOURCE =
            PiCounterId.of("FabricIngress.process_set_source_sink.counter_set_source");
    public static final PiCounterId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_SOURCE_COUNTER_INT_SOURCE =
            PiCounterId.of("FabricEgress.process_int_main.process_int_source.counter_int_source");
    public static final PiCounterId FABRIC_INGRESS_SPGW_INGRESS_UE_COUNTER =
            PiCounterId.of("FabricIngress.spgw_ingress.ue_counter");
    public static final PiCounterId FABRIC_INGRESS_NEXT_SIMPLE_COUNTER =
            PiCounterId.of("FabricIngress.next.simple_counter");
    public static final PiCounterId FABRIC_INGRESS_PROCESS_SET_SOURCE_SINK_COUNTER_SET_SINK =
            PiCounterId.of("FabricIngress.process_set_source_sink.counter_set_sink");
    public static final PiCounterId FABRIC_EGRESS_EGRESS_NEXT_EGRESS_VLAN_COUNTER =
            PiCounterId.of("FabricEgress.egress_next.egress_vlan_counter");
    public static final PiCounterId FABRIC_INGRESS_FORWARDING_ROUTING_V6_COUNTER =
            PiCounterId.of("FabricIngress.forwarding.routing_v6_counter");
    public static final PiCounterId FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN_COUNTER =
            PiCounterId.of("FabricIngress.filtering.ingress_port_vlan_counter");
    public static final PiCounterId FABRIC_INGRESS_FORWARDING_MPLS_COUNTER =
            PiCounterId.of("FabricIngress.forwarding.mpls_counter");
    public static final PiCounterId FABRIC_INGRESS_NEXT_HASHED_COUNTER =
            PiCounterId.of("FabricIngress.next.hashed_counter");
    // Action IDs
    public static final PiActionId FABRIC_INGRESS_FORWARDING_PUNT_TO_CPU =
            PiActionId.of("FabricIngress.forwarding.punt_to_cpu");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0407_I5 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0407_i5");
    public static final PiActionId FABRIC_INGRESS_NEXT_MPLS_ROUTING_V6_SIMPLE =
            PiActionId.of("FabricIngress.next.mpls_routing_v6_simple");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0407_I12 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0407_i12");
    public static final PiActionId FABRIC_INGRESS_FORWARDING_NOP_ROUTING_V4 =
            PiActionId.of("FabricIngress.forwarding.nop_routing_v4");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0407_I10 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0407_i10");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0407_I11 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0407_i11");
    public static final PiActionId FABRIC_INGRESS_FILTERING_NOP_INGRESS_PORT_VLAN =
            PiActionId.of("FabricIngress.filtering.nop_ingress_port_vlan");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0407_I14 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0407_i14");
    public static final PiActionId FABRIC_INGRESS_FORWARDING_SET_NEXT_ID_BRIDGING =
            PiActionId.of("FabricIngress.forwarding.set_next_id_bridging");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_SOURCE_INT_SOURCE_DSCP =
            PiActionId.of("FabricEgress.process_int_main.process_int_source.int_source_dscp");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0407_I0 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0407_i0");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INIT_METADATA =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.init_metadata");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0003_I14 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0003_i14");
    public static final PiActionId FABRIC_EGRESS_EGRESS_NEXT_POP_VLAN =
            PiActionId.of("FabricEgress.egress_next.pop_vlan");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0003_I4 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0003_i4");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0407_I2 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0407_i2");
    public static final PiActionId FABRIC_INGRESS_NEXT_SET_VLAN =
            PiActionId.of("FabricIngress.next.set_vlan");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0407_I4 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0407_i4");
    public static final PiActionId FABRIC_EGRESS_SPGW_EGRESS_GTPU_ENCAP =
            PiActionId.of("FabricEgress.spgw_egress.gtpu_encap");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0003_I12 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0003_i12");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0003_I13 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0003_i13");
    public static final PiActionId FABRIC_INGRESS_FILTERING_SET_VLAN =
            PiActionId.of("FabricIngress.filtering.set_vlan");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0003_I11 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0003_i11");
    public static final PiActionId FABRIC_EGRESS_PKT_IO_EGRESS_POP_VLAN =
            PiActionId.of("FabricEgress.pkt_io_egress.pop_vlan");
    public static final PiActionId FABRIC_INGRESS_NEXT_L3_ROUTING_SIMPLE =
            PiActionId.of("FabricIngress.next.l3_routing_simple");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0003_I15 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0003_i15");
    public static final PiActionId FABRIC_INGRESS_NEXT_SET_MCAST_GROUP =
            PiActionId.of("FabricIngress.next.set_mcast_group");
    public static final PiActionId FABRIC_INGRESS_FORWARDING_SET_NEXT_ID_ROUTING_V4 =
            PiActionId.of("FabricIngress.forwarding.set_next_id_routing_v4");
    public static final PiActionId FABRIC_INGRESS_FORWARDING_SET_NEXT_ID_ROUTING_V6 =
            PiActionId.of("FabricIngress.forwarding.set_next_id_routing_v6");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0407_I13 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0407_i13");
    public static final PiActionId FABRIC_INGRESS_SPGW_INGRESS_SET_DL_SESS_INFO =
            PiActionId.of("FabricIngress.spgw_ingress.set_dl_sess_info");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0407_I7 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0407_i7");
    public static final PiActionId FABRIC_INGRESS_FILTERING_PUSH_INTERNAL_VLAN =
            PiActionId.of("FabricIngress.filtering.push_internal_vlan");
    public static final PiActionId FABRIC_INGRESS_FORWARDING_CLONE_TO_CPU =
            PiActionId.of("FabricIngress.forwarding.clone_to_cpu");
    public static final PiActionId FABRIC_INGRESS_SPGW_INGRESS_GTPU_DECAP =
            PiActionId.of("FabricIngress.spgw_ingress.gtpu_decap");
    public static final PiActionId FABRIC_INGRESS_FORWARDING_POP_MPLS_AND_NEXT =
            PiActionId.of("FabricIngress.forwarding.pop_mpls_and_next");
    public static final PiActionId DROP_NOW = PiActionId.of("drop_now");
    public static final PiActionId FABRIC_INGRESS_NEXT_L3_ROUTING_HASHED =
            PiActionId.of("FabricIngress.next.l3_routing_hashed");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0003_I10 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0003_i10");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0407_I8 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0407_i8");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0003_I0 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0003_i0");
    public static final PiActionId FABRIC_INGRESS_PROCESS_SET_SOURCE_SINK_INT_SET_SINK =
            PiActionId.of("FabricIngress.process_set_source_sink.int_set_sink");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_SINK_INT_SINK =
            PiActionId.of("FabricEgress.process_int_main.process_int_sink.int_sink");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0003_I1 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0003_i1");
    public static final PiActionId FABRIC_INGRESS_NEXT_MPLS_ROUTING_V4_HASHED =
            PiActionId.of("FabricIngress.next.mpls_routing_v4_hashed");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0407_I1 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0407_i1");
    public static final PiActionId FABRIC_INGRESS_PROCESS_SET_SOURCE_SINK_INT_SET_SOURCE =
            PiActionId.of("FabricIngress.process_set_source_sink.int_set_source");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0407_I3 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0407_i3");
    public static final PiActionId NOP = PiActionId.of("nop");
    public static final PiActionId FABRIC_INGRESS_FORWARDING_DROP =
            PiActionId.of("FabricIngress.forwarding.drop");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0407_I6 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0407_i6");
    public static final PiActionId FABRIC_INGRESS_NEXT_OUTPUT_SIMPLE =
            PiActionId.of("FabricIngress.next.output_simple");
    public static final PiActionId FABRIC_INGRESS_FILTERING_DROP =
            PiActionId.of("FabricIngress.filtering.drop");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_SINK_RESTORE_HEADER =
            PiActionId.of("FabricEgress.process_int_main.process_int_sink.restore_header");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0407_I9 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0407_i9");
    public static final PiActionId FABRIC_INGRESS_FILTERING_SET_FORWARDING_TYPE =
            PiActionId.of("FabricIngress.filtering.set_forwarding_type");
    public static final PiActionId FABRIC_INGRESS_NEXT_SET_VLAN_OUTPUT =
            PiActionId.of("FabricIngress.next.set_vlan_output");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_REPORT_DO_REPORT_ENCAPSULATION =
            PiActionId.of("FabricEgress.process_int_main.process_int_report.do_report_encapsulation");
    public static final PiActionId NO_ACTION = PiActionId.of("NoAction");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0003_I8 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0003_i8");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0003_I9 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0003_i9");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0407_I15 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0407_i15");
    public static final PiActionId FABRIC_INGRESS_NEXT_MPLS_ROUTING_V4_SIMPLE =
            PiActionId.of("FabricIngress.next.mpls_routing_v4_simple");
    public static final PiActionId FABRIC_INGRESS_FORWARDING_NOP_ACL =
            PiActionId.of("FabricIngress.forwarding.nop_acl");
    public static final PiActionId FABRIC_INGRESS_NEXT_MPLS_ROUTING_V6_HASHED =
            PiActionId.of("FabricIngress.next.mpls_routing_v6_hashed");
    public static final PiActionId FABRIC_INGRESS_NEXT_L3_ROUTING_VLAN =
            PiActionId.of("FabricIngress.next.l3_routing_vlan");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0003_I2 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0003_i2");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0003_I3 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0003_i3");
    public static final PiActionId FABRIC_INGRESS_FORWARDING_SET_NEXT_ID_ACL =
            PiActionId.of("FabricIngress.forwarding.set_next_id_acl");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0003_I5 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0003_i5");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0003_I6 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0003_i6");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INT_SET_HEADER_0003_I7 =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.int_set_header_0003_i7");
    // Action Param IDs
    public static final PiActionParamId DMAC = PiActionParamId.of("dmac");
    public static final PiActionParamId MON_IP = PiActionParamId.of("mon_ip");
    public static final PiActionParamId TEID = PiActionParamId.of("teid");
    public static final PiActionParamId INS_MASK0407 =
            PiActionParamId.of("ins_mask0407");
    public static final PiActionParamId INS_MASK0003 =
            PiActionParamId.of("ins_mask0003");
    public static final PiActionParamId S1U_ENB_ADDR =
            PiActionParamId.of("s1u_enb_addr");
    public static final PiActionParamId PORT_NUM =
            PiActionParamId.of("port_num");
    public static final PiActionParamId S1U_SGW_ADDR =
            PiActionParamId.of("s1u_sgw_addr");
    public static final PiActionParamId LABEL = PiActionParamId.of("label");
    public static final PiActionParamId SMAC = PiActionParamId.of("smac");
    public static final PiActionParamId MON_PORT =
            PiActionParamId.of("mon_port");
    public static final PiActionParamId GID = PiActionParamId.of("gid");
    public static final PiActionParamId NEW_VLAN_ID =
            PiActionParamId.of("new_vlan_id");
    public static final PiActionParamId FWD_TYPE =
            PiActionParamId.of("fwd_type");
    public static final PiActionParamId MON_MAC = PiActionParamId.of("mon_mac");
    public static final PiActionParamId SRC_MAC = PiActionParamId.of("src_mac");
    public static final PiActionParamId NEXT_ID = PiActionParamId.of("next_id");
    public static final PiActionParamId INS_CNT = PiActionParamId.of("ins_cnt");
    public static final PiActionParamId SWITCH_ID =
            PiActionParamId.of("switch_id");
    public static final PiActionParamId MAX_HOP = PiActionParamId.of("max_hop");
    public static final PiActionParamId SRC_IP = PiActionParamId.of("src_ip");
    // Action Profile IDs
    public static final PiActionProfileId FABRIC_INGRESS_NEXT_ECMP_SELECTOR =
            PiActionProfileId.of("FabricIngress.next.ecmp_selector");
    // Packet Metadata IDs
    public static final PiControlMetadataId INGRESS_PORT =
            PiControlMetadataId.of("ingress_port");
    public static final PiControlMetadataId EGRESS_PORT =
            PiControlMetadataId.of("egress_port");
}
