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

package org.onosproject.pipelines.fabric.impl.behaviour;

import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiPacketMetadataId;
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
    public static final PiMatchFieldId HDR_IG_PORT =
            PiMatchFieldId.of("ig_port");
    public static final PiMatchFieldId HDR_VLAN_IS_VALID =
            PiMatchFieldId.of("vlan_is_valid");
    public static final PiMatchFieldId HDR_IPV6_SRC_NET_ID =
            PiMatchFieldId.of("ipv6_src_net_id");
    public static final PiMatchFieldId HDR_C_TAG = PiMatchFieldId.of("c_tag");
    public static final PiMatchFieldId HDR_IPV4_SRC =
            PiMatchFieldId.of("ipv4_src");
    public static final PiMatchFieldId HDR_IPV6_DST =
            PiMatchFieldId.of("ipv6_dst");
    public static final PiMatchFieldId HDR_L4_DPORT =
            PiMatchFieldId.of("l4_dport");
    public static final PiMatchFieldId HDR_PPPOE_CODE =
            PiMatchFieldId.of("pppoe_code");
    public static final PiMatchFieldId HDR_IPV6_SRC =
            PiMatchFieldId.of("ipv6_src");
    public static final PiMatchFieldId HDR_ETH_SRC =
            PiMatchFieldId.of("eth_src");
    public static final PiMatchFieldId HDR_S_TAG = PiMatchFieldId.of("s_tag");
    public static final PiMatchFieldId HDR_VLAN_ID =
            PiMatchFieldId.of("vlan_id");
    public static final PiMatchFieldId HDR_ETH_DST =
            PiMatchFieldId.of("eth_dst");
    public static final PiMatchFieldId HDR_ICMP_TYPE =
            PiMatchFieldId.of("icmp_type");
    public static final PiMatchFieldId HDR_IPV4_DST =
            PiMatchFieldId.of("ipv4_dst");
    public static final PiMatchFieldId HDR_IPV6_TRAFFIC_CLASS =
            PiMatchFieldId.of("ipv6_traffic_class");
    public static final PiMatchFieldId HDR_TUNNEL_IPV4_DST =
            PiMatchFieldId.of("tunnel_ipv4_dst");
    public static final PiMatchFieldId HDR_ETH_TYPE =
            PiMatchFieldId.of("eth_type");
    public static final PiMatchFieldId HDR_GTPU_IS_VALID =
            PiMatchFieldId.of("gtpu_is_valid");
    public static final PiMatchFieldId HDR_NEXT_ID =
            PiMatchFieldId.of("next_id");
    public static final PiMatchFieldId HDR_IP_ETH_TYPE =
            PiMatchFieldId.of("ip_eth_type");
    public static final PiMatchFieldId HDR_L4_SPORT =
            PiMatchFieldId.of("l4_sport");
    public static final PiMatchFieldId HDR_ICMP_CODE =
            PiMatchFieldId.of("icmp_code");
    public static final PiMatchFieldId HDR_INNER_VLAN_ID =
            PiMatchFieldId.of("inner_vlan_id");
    public static final PiMatchFieldId HDR_IPV4_ECN =
            PiMatchFieldId.of("ipv4_ecn");
    public static final PiMatchFieldId HDR_PPPOE_SESSION_ID =
            PiMatchFieldId.of("pppoe_session_id");
    public static final PiMatchFieldId HDR_EG_SPEC =
            PiMatchFieldId.of("eg_spec");
    public static final PiMatchFieldId HDR_IPV4_DST_ADDR =
            PiMatchFieldId.of("ipv4_dst_addr");
    public static final PiMatchFieldId HDR_LINE_ID =
            PiMatchFieldId.of("line_id");
    public static final PiMatchFieldId HDR_IPV4_DSCP =
            PiMatchFieldId.of("ipv4_dscp");
    public static final PiMatchFieldId HDR_FAR_ID = PiMatchFieldId.of("far_id");
    public static final PiMatchFieldId HDR_EG_PORT =
            PiMatchFieldId.of("eg_port");
    public static final PiMatchFieldId HDR_UE_ADDR =
            PiMatchFieldId.of("ue_addr");
    public static final PiMatchFieldId HDR_TEID = PiMatchFieldId.of("teid");
    public static final PiMatchFieldId HDR_INT_IS_VALID =
            PiMatchFieldId.of("int_is_valid");
    public static final PiMatchFieldId HDR_MPLS_LABEL =
            PiMatchFieldId.of("mpls_label");
    public static final PiMatchFieldId HDR_IP_PROTO =
            PiMatchFieldId.of("ip_proto");
    public static final PiMatchFieldId HDR_PPPOE_PROTOCOL =
            PiMatchFieldId.of("pppoe_protocol");
    // Table IDs
    public static final PiTableId FABRIC_INGRESS_NEXT_HASHED =
            PiTableId.of("FabricIngress.next.hashed");
    public static final PiTableId FABRIC_INGRESS_BNG_INGRESS_T_LINE_MAP =
            PiTableId.of("FabricIngress.bng_ingress.t_line_map");
    public static final PiTableId FABRIC_INGRESS_SPGW_INGRESS_FAR_LOOKUP =
            PiTableId.of("FabricIngress.spgw_ingress.far_lookup");
    public static final PiTableId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_TB_INT_INSERT =
            PiTableId.of("FabricEgress.process_int_main.process_int_transit.tb_int_insert");
    public static final PiTableId FABRIC_INGRESS_FILTERING_FWD_CLASSIFIER =
            PiTableId.of("FabricIngress.filtering.fwd_classifier");
    public static final PiTableId FABRIC_INGRESS_NEXT_XCONNECT =
            PiTableId.of("FabricIngress.next.xconnect");
    public static final PiTableId FABRIC_INGRESS_NEXT_NEXT_VLAN =
            PiTableId.of("FabricIngress.next.next_vlan");
    public static final PiTableId FABRIC_INGRESS_SPGW_INGRESS_DOWNLINK_PDR_LOOKUP =
            PiTableId.of("FabricIngress.spgw_ingress.downlink_pdr_lookup");
    public static final PiTableId FABRIC_INGRESS_NEXT_SIMPLE =
            PiTableId.of("FabricIngress.next.simple");
    public static final PiTableId FABRIC_INGRESS_NEXT_MULTICAST =
            PiTableId.of("FabricIngress.next.multicast");
    public static final PiTableId FABRIC_INGRESS_SPGW_INGRESS_UPLINK_PDR_LOOKUP =
            PiTableId.of("FabricIngress.spgw_ingress.uplink_pdr_lookup");
    public static final PiTableId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_SOURCE_TB_INT_SOURCE =
            PiTableId.of("FabricEgress.process_int_main.process_int_source.tb_int_source");
    public static final PiTableId FABRIC_INGRESS_FORWARDING_ROUTING_V6 =
            PiTableId.of("FabricIngress.forwarding.routing_v6");
    public static final PiTableId FABRIC_INGRESS_FORWARDING_MPLS =
            PiTableId.of("FabricIngress.forwarding.mpls");
    public static final PiTableId FABRIC_INGRESS_FORWARDING_ROUTING_V4 =
            PiTableId.of("FabricIngress.forwarding.routing_v4");
    public static final PiTableId FABRIC_INGRESS_ACL_ACL =
            PiTableId.of("FabricIngress.acl.acl");
    public static final PiTableId FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN =
            PiTableId.of("FabricIngress.filtering.ingress_port_vlan");
    public static final PiTableId FABRIC_INGRESS_BNG_INGRESS_UPSTREAM_T_PPPOE_CP =
            PiTableId.of("FabricIngress.bng_ingress.upstream.t_pppoe_cp");
    public static final PiTableId FABRIC_INGRESS_BNG_INGRESS_UPSTREAM_T_PPPOE_TERM_V4 =
            PiTableId.of("FabricIngress.bng_ingress.upstream.t_pppoe_term_v4");
    public static final PiTableId FABRIC_INGRESS_BNG_INGRESS_UPSTREAM_T_PPPOE_TERM_V6 =
            PiTableId.of("FabricIngress.bng_ingress.upstream.t_pppoe_term_v6");
    public static final PiTableId FABRIC_INGRESS_FORWARDING_BRIDGING =
            PiTableId.of("FabricIngress.forwarding.bridging");
    public static final PiTableId FABRIC_INGRESS_BNG_INGRESS_DOWNSTREAM_T_LINE_SESSION_MAP =
            PiTableId.of("FabricIngress.bng_ingress.downstream.t_line_session_map");
    public static final PiTableId FABRIC_INGRESS_SPGW_INGRESS_INTERFACE_LOOKUP =
            PiTableId.of("FabricIngress.spgw_ingress.interface_lookup");
    public static final PiTableId FABRIC_EGRESS_EGRESS_NEXT_EGRESS_VLAN =
            PiTableId.of("FabricEgress.egress_next.egress_vlan");
    public static final PiTableId FABRIC_INGRESS_PROCESS_SET_SOURCE_SINK_TB_SET_SINK =
            PiTableId.of("FabricIngress.process_set_source_sink.tb_set_sink");
    public static final PiTableId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_REPORT_TB_GENERATE_REPORT =
            PiTableId.of("FabricEgress.process_int_main.process_int_report.tb_generate_report");
    public static final PiTableId FABRIC_INGRESS_PROCESS_SET_SOURCE_SINK_TB_SET_SOURCE =
            PiTableId.of("FabricIngress.process_set_source_sink.tb_set_source");
    public static final PiTableId FABRIC_INGRESS_BNG_INGRESS_DOWNSTREAM_T_QOS_V6 =
            PiTableId.of("FabricIngress.bng_ingress.downstream.t_qos_v6");
    public static final PiTableId FABRIC_INGRESS_BNG_INGRESS_DOWNSTREAM_T_QOS_V4 =
            PiTableId.of("FabricIngress.bng_ingress.downstream.t_qos_v4");
    // Indirect Counter IDs
    public static final PiCounterId FABRIC_EGRESS_SPGW_EGRESS_PDR_COUNTER =
            PiCounterId.of("FabricEgress.spgw_egress.pdr_counter");
    public static final PiCounterId FABRIC_INGRESS_SPGW_INGRESS_PDR_COUNTER =
            PiCounterId.of("FabricIngress.spgw_ingress.pdr_counter");
    public static final PiCounterId FABRIC_EGRESS_BNG_EGRESS_DOWNSTREAM_C_LINE_TX =
            PiCounterId.of("FabricEgress.bng_egress.downstream.c_line_tx");
    public static final PiCounterId FABRIC_INGRESS_PORT_COUNTERS_CONTROL_EGRESS_PORT_COUNTER =
            PiCounterId.of("FabricIngress.port_counters_control.egress_port_counter");
    public static final PiCounterId FABRIC_INGRESS_BNG_INGRESS_UPSTREAM_C_DROPPED =
            PiCounterId.of("FabricIngress.bng_ingress.upstream.c_dropped");
    public static final PiCounterId FABRIC_INGRESS_BNG_INGRESS_UPSTREAM_C_CONTROL =
            PiCounterId.of("FabricIngress.bng_ingress.upstream.c_control");
    public static final PiCounterId FABRIC_INGRESS_BNG_INGRESS_UPSTREAM_C_TERMINATED =
            PiCounterId.of("FabricIngress.bng_ingress.upstream.c_terminated");
    public static final PiCounterId FABRIC_INGRESS_BNG_INGRESS_DOWNSTREAM_C_LINE_RX =
            PiCounterId.of("FabricIngress.bng_ingress.downstream.c_line_rx");
    public static final PiCounterId FABRIC_INGRESS_PORT_COUNTERS_CONTROL_INGRESS_PORT_COUNTER =
            PiCounterId.of("FabricIngress.port_counters_control.ingress_port_counter");
    // Direct Counter IDs
    public static final PiCounterId FABRIC_INGRESS_NEXT_MULTICAST_COUNTER =
            PiCounterId.of("FabricIngress.next.multicast_counter");
    public static final PiCounterId FABRIC_INGRESS_NEXT_SIMPLE_COUNTER =
            PiCounterId.of("FabricIngress.next.simple_counter");
    public static final PiCounterId FABRIC_INGRESS_FILTERING_FWD_CLASSIFIER_COUNTER =
            PiCounterId.of("FabricIngress.filtering.fwd_classifier_counter");
    public static final PiCounterId FABRIC_INGRESS_FORWARDING_BRIDGING_COUNTER =
            PiCounterId.of("FabricIngress.forwarding.bridging_counter");
    public static final PiCounterId FABRIC_INGRESS_NEXT_HASHED_COUNTER =
            PiCounterId.of("FabricIngress.next.hashed_counter");
    public static final PiCounterId FABRIC_INGRESS_PROCESS_SET_SOURCE_SINK_COUNTER_SET_SOURCE =
            PiCounterId.of("FabricIngress.process_set_source_sink.counter_set_source");
    public static final PiCounterId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_SOURCE_COUNTER_INT_SOURCE =
            PiCounterId.of("FabricEgress.process_int_main.process_int_source.counter_int_source");
    public static final PiCounterId FABRIC_INGRESS_PROCESS_SET_SOURCE_SINK_COUNTER_SET_SINK =
            PiCounterId.of("FabricIngress.process_set_source_sink.counter_set_sink");
    public static final PiCounterId FABRIC_EGRESS_EGRESS_NEXT_EGRESS_VLAN_COUNTER =
            PiCounterId.of("FabricEgress.egress_next.egress_vlan_counter");
    public static final PiCounterId FABRIC_INGRESS_ACL_ACL_COUNTER =
            PiCounterId.of("FabricIngress.acl.acl_counter");
    public static final PiCounterId FABRIC_INGRESS_NEXT_XCONNECT_COUNTER =
            PiCounterId.of("FabricIngress.next.xconnect_counter");
    public static final PiCounterId FABRIC_INGRESS_NEXT_NEXT_VLAN_COUNTER =
            PiCounterId.of("FabricIngress.next.next_vlan_counter");
    public static final PiCounterId FABRIC_INGRESS_FORWARDING_ROUTING_V6_COUNTER =
            PiCounterId.of("FabricIngress.forwarding.routing_v6_counter");
    public static final PiCounterId FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN_COUNTER =
            PiCounterId.of("FabricIngress.filtering.ingress_port_vlan_counter");
    public static final PiCounterId FABRIC_INGRESS_FORWARDING_MPLS_COUNTER =
            PiCounterId.of("FabricIngress.forwarding.mpls_counter");
    // Action IDs
    public static final PiActionId FABRIC_EGRESS_BNG_EGRESS_DOWNSTREAM_ENCAP_V4 =
            PiActionId.of("FabricEgress.bng_egress.downstream.encap_v4");
    public static final PiActionId FABRIC_INGRESS_NEXT_SET_NEXT_ID_XCONNECT =
            PiActionId.of("FabricIngress.next.set_next_id_xconnect");
    public static final PiActionId FABRIC_INGRESS_FORWARDING_NOP_ROUTING_V4 =
            PiActionId.of("FabricIngress.forwarding.nop_routing_v4");
    public static final PiActionId FABRIC_INGRESS_BNG_INGRESS_DOWNSTREAM_QOS_BESTEFF =
            PiActionId.of("FabricIngress.bng_ingress.downstream.qos_besteff");
    public static final PiActionId FABRIC_INGRESS_FILTERING_PERMIT_WITH_INTERNAL_VLAN =
            PiActionId.of("FabricIngress.filtering.permit_with_internal_vlan");
    public static final PiActionId FABRIC_INGRESS_NEXT_ROUTING_HASHED =
            PiActionId.of("FabricIngress.next.routing_hashed");
    public static final PiActionId FABRIC_INGRESS_FORWARDING_SET_NEXT_ID_BRIDGING =
            PiActionId.of("FabricIngress.forwarding.set_next_id_bridging");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_SOURCE_INT_SOURCE_DSCP =
            PiActionId.of("FabricEgress.process_int_main.process_int_source.int_source_dscp");
    public static final PiActionId FABRIC_INGRESS_NEXT_SET_DOUBLE_VLAN =
            PiActionId.of("FabricIngress.next.set_double_vlan");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_TRANSIT_INIT_METADATA =
            PiActionId.of("FabricEgress.process_int_main.process_int_transit.init_metadata");
    public static final PiActionId FABRIC_INGRESS_ACL_DROP =
            PiActionId.of("FabricIngress.acl.drop");
    public static final PiActionId FABRIC_INGRESS_ACL_SET_CLONE_SESSION_ID =
            PiActionId.of("FabricIngress.acl.set_clone_session_id");
    public static final PiActionId FABRIC_INGRESS_BNG_INGRESS_UPSTREAM_PUNT_TO_CPU =
            PiActionId.of("FabricIngress.bng_ingress.upstream.punt_to_cpu");
    public static final PiActionId FABRIC_INGRESS_BNG_INGRESS_DOWNSTREAM_DROP =
            PiActionId.of("FabricIngress.bng_ingress.downstream.drop");
    public static final PiActionId FABRIC_INGRESS_SPGW_INGRESS_SET_PDR_ATTRIBUTES =
            PiActionId.of("FabricIngress.spgw_ingress.set_pdr_attributes");
    public static final PiActionId FABRIC_INGRESS_NEXT_SET_VLAN =
            PiActionId.of("FabricIngress.next.set_vlan");
    public static final PiActionId FABRIC_INGRESS_ACL_NOP_ACL =
            PiActionId.of("FabricIngress.acl.nop_acl");
    public static final PiActionId FABRIC_INGRESS_SPGW_INGRESS_LOAD_NORMAL_FAR_ATTRIBUTES =
            PiActionId.of("FabricIngress.spgw_ingress.load_normal_far_attributes");
    public static final PiActionId FABRIC_INGRESS_BNG_INGRESS_SET_LINE =
            PiActionId.of("FabricIngress.bng_ingress.set_line");
    public static final PiActionId FABRIC_INGRESS_BNG_INGRESS_UPSTREAM_TERM_DISABLED =
            PiActionId.of("FabricIngress.bng_ingress.upstream.term_disabled");
    public static final PiActionId FABRIC_INGRESS_ACL_SET_NEXT_ID_ACL =
            PiActionId.of("FabricIngress.acl.set_next_id_acl");
    public static final PiActionId FABRIC_INGRESS_FILTERING_PERMIT =
            PiActionId.of("FabricIngress.filtering.permit");
    public static final PiActionId FABRIC_INGRESS_FORWARDING_SET_NEXT_ID_ROUTING_V4 =
            PiActionId.of("FabricIngress.forwarding.set_next_id_routing_v4");
    public static final PiActionId FABRIC_INGRESS_FORWARDING_SET_NEXT_ID_ROUTING_V6 =
            PiActionId.of("FabricIngress.forwarding.set_next_id_routing_v6");
    public static final PiActionId FABRIC_INGRESS_NEXT_ROUTING_SIMPLE =
            PiActionId.of("FabricIngress.next.routing_simple");
    public static final PiActionId FABRIC_INGRESS_SPGW_INGRESS_DECAP_INNER_TCP =
            PiActionId.of("FabricIngress.spgw_ingress.decap_inner_tcp");
    public static final PiActionId FABRIC_INGRESS_SPGW_INGRESS_DECAP_INNER_ICMP =
            PiActionId.of("FabricIngress.spgw_ingress.decap_inner_icmp");
    public static final PiActionId FABRIC_INGRESS_NEXT_OUTPUT_HASHED =
            PiActionId.of("FabricIngress.next.output_hashed");
    public static final PiActionId FABRIC_INGRESS_SPGW_INGRESS_DECAP_INNER_UDP =
            PiActionId.of("FabricIngress.spgw_ingress.decap_inner_udp");
    public static final PiActionId FABRIC_INGRESS_FORWARDING_POP_MPLS_AND_NEXT =
            PiActionId.of("FabricIngress.forwarding.pop_mpls_and_next");
    public static final PiActionId FABRIC_EGRESS_BNG_EGRESS_DOWNSTREAM_ENCAP_V6 =
            PiActionId.of("FabricEgress.bng_egress.downstream.encap_v6");
    public static final PiActionId FABRIC_INGRESS_NEXT_MPLS_ROUTING_SIMPLE =
            PiActionId.of("FabricIngress.next.mpls_routing_simple");
    public static final PiActionId FABRIC_INGRESS_ACL_PUNT_TO_CPU =
            PiActionId.of("FabricIngress.acl.punt_to_cpu");
    public static final PiActionId FABRIC_INGRESS_BNG_INGRESS_DOWNSTREAM_QOS_PRIO =
            PiActionId.of("FabricIngress.bng_ingress.downstream.qos_prio");
    public static final PiActionId FABRIC_EGRESS_EGRESS_NEXT_POP_VLAN =
            PiActionId.of("FabricEgress.egress_next.pop_vlan");
    public static final PiActionId FABRIC_INGRESS_PROCESS_SET_SOURCE_SINK_INT_SET_SINK =
            PiActionId.of("FabricIngress.process_set_source_sink.int_set_sink");
    public static final PiActionId FABRIC_INGRESS_NEXT_MPLS_ROUTING_HASHED =
            PiActionId.of("FabricIngress.next.mpls_routing_hashed");
    public static final PiActionId FABRIC_INGRESS_BNG_INGRESS_UPSTREAM_TERM_ENABLED_V6 =
            PiActionId.of("FabricIngress.bng_ingress.upstream.term_enabled_v6");
    public static final PiActionId FABRIC_INGRESS_BNG_INGRESS_UPSTREAM_TERM_ENABLED_V4 =
            PiActionId.of("FabricIngress.bng_ingress.upstream.term_enabled_v4");
    public static final PiActionId FABRIC_INGRESS_SPGW_INGRESS_DECAP_INNER_UNKNOWN =
            PiActionId.of("FabricIngress.spgw_ingress.decap_inner_unknown");
    public static final PiActionId FABRIC_INGRESS_PROCESS_SET_SOURCE_SINK_INT_SET_SOURCE =
            PiActionId.of("FabricIngress.process_set_source_sink.int_set_source");
    public static final PiActionId NOP = PiActionId.of("nop");
    public static final PiActionId FABRIC_INGRESS_NEXT_OUTPUT_SIMPLE =
            PiActionId.of("FabricIngress.next.output_simple");
    public static final PiActionId FABRIC_INGRESS_SPGW_INGRESS_SET_SOURCE_IFACE =
            PiActionId.of("FabricIngress.spgw_ingress.set_source_iface");
    public static final PiActionId FABRIC_INGRESS_FILTERING_DENY =
            PiActionId.of("FabricIngress.filtering.deny");
    public static final PiActionId FABRIC_INGRESS_BNG_INGRESS_DOWNSTREAM_SET_SESSION =
            PiActionId.of("FabricIngress.bng_ingress.downstream.set_session");
    public static final PiActionId FABRIC_INGRESS_NEXT_SET_MCAST_GROUP_ID =
            PiActionId.of("FabricIngress.next.set_mcast_group_id");
    public static final PiActionId FABRIC_INGRESS_FILTERING_SET_FORWARDING_TYPE =
            PiActionId.of("FabricIngress.filtering.set_forwarding_type");
    public static final PiActionId FABRIC_EGRESS_PROCESS_INT_MAIN_PROCESS_INT_REPORT_DO_REPORT_ENCAPSULATION =
            PiActionId.of("FabricEgress.process_int_main.process_int_report.do_report_encapsulation");
    public static final PiActionId NO_ACTION = PiActionId.of("NoAction");
    public static final PiActionId FABRIC_INGRESS_NEXT_OUTPUT_XCONNECT =
            PiActionId.of("FabricIngress.next.output_xconnect");
    public static final PiActionId FABRIC_INGRESS_SPGW_INGRESS_LOAD_TUNNEL_FAR_ATTRIBUTES =
            PiActionId.of("FabricIngress.spgw_ingress.load_tunnel_far_attributes");
    // Action Param IDs
    public static final PiActionParamId DMAC = PiActionParamId.of("dmac");
    public static final PiActionParamId MON_PORT =
            PiActionParamId.of("mon_port");
    public static final PiActionParamId NOTIFY_CP =
            PiActionParamId.of("notify_cp");
    public static final PiActionParamId SRC_IFACE =
            PiActionParamId.of("src_iface");
    public static final PiActionParamId SWITCH_ID =
            PiActionParamId.of("switch_id");
    public static final PiActionParamId SMAC = PiActionParamId.of("smac");
    public static final PiActionParamId CLONE_ID =
            PiActionParamId.of("clone_id");
    public static final PiActionParamId FAR_ID = PiActionParamId.of("far_id");
    public static final PiActionParamId VLAN_ID = PiActionParamId.of("vlan_id");
    public static final PiActionParamId TUNNEL_SRC_ADDR =
            PiActionParamId.of("tunnel_src_addr");
    public static final PiActionParamId TUNNEL_SRC_PORT =
            PiActionParamId.of("tunnel_src_port");
    public static final PiActionParamId LABEL = PiActionParamId.of("label");
    public static final PiActionParamId SRC_IP = PiActionParamId.of("src_ip");
    public static final PiActionParamId SKIP_SPGW =
            PiActionParamId.of("skip_spgw");
    public static final PiActionParamId MON_MAC = PiActionParamId.of("mon_mac");
    public static final PiActionParamId NEXT_ID = PiActionParamId.of("next_id");
    public static final PiActionParamId INS_CNT = PiActionParamId.of("ins_cnt");
    public static final PiActionParamId SRC_MAC = PiActionParamId.of("src_mac");
    public static final PiActionParamId INNER_VLAN_ID =
            PiActionParamId.of("inner_vlan_id");
    public static final PiActionParamId DIRECTION =
            PiActionParamId.of("direction");
    public static final PiActionParamId PPPOE_SESSION_ID =
            PiActionParamId.of("pppoe_session_id");
    public static final PiActionParamId CTR_ID = PiActionParamId.of("ctr_id");
    public static final PiActionParamId MON_IP = PiActionParamId.of("mon_ip");
    public static final PiActionParamId INS_MASK0003 =
            PiActionParamId.of("ins_mask0003");
    public static final PiActionParamId LINE_ID = PiActionParamId.of("line_id");
    public static final PiActionParamId FWD_TYPE =
            PiActionParamId.of("fwd_type");
    public static final PiActionParamId OUTER_VLAN_ID =
            PiActionParamId.of("outer_vlan_id");
    public static final PiActionParamId NEEDS_GTPU_DECAP =
            PiActionParamId.of("needs_gtpu_decap");
    public static final PiActionParamId INS_MASK0407 =
            PiActionParamId.of("ins_mask0407");
    public static final PiActionParamId TEID = PiActionParamId.of("teid");
    public static final PiActionParamId DROP = PiActionParamId.of("drop");
    public static final PiActionParamId PORT_NUM =
            PiActionParamId.of("port_num");
    public static final PiActionParamId TUNNEL_DST_ADDR =
            PiActionParamId.of("tunnel_dst_addr");
    public static final PiActionParamId GROUP_ID =
            PiActionParamId.of("group_id");
    public static final PiActionParamId MAX_HOP = PiActionParamId.of("max_hop");
    // Action Profile IDs
    public static final PiActionProfileId FABRIC_INGRESS_NEXT_HASHED_SELECTOR =
            PiActionProfileId.of("FabricIngress.next.hashed_selector");
    // Packet Metadata IDs
    public static final PiPacketMetadataId INGRESS_PORT =
            PiPacketMetadataId.of("ingress_port");
    public static final PiPacketMetadataId EGRESS_PORT =
            PiPacketMetadataId.of("egress_port");
    // Meter IDs
    public static final PiMeterId FABRIC_INGRESS_BNG_INGRESS_DOWNSTREAM_M_BESTEFF =
            PiMeterId.of("FabricIngress.bng_ingress.downstream.m_besteff");
    public static final PiMeterId FABRIC_INGRESS_BNG_INGRESS_DOWNSTREAM_M_PRIO =
            PiMeterId.of("FabricIngress.bng_ingress.downstream.m_prio");
}