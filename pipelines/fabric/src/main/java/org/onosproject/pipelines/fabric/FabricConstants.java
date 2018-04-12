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
    public static final PiMatchFieldId FABRIC_METADATA_L4_SRC_PORT =
            PiMatchFieldId.of("fabric_metadata.l4_src_port");
    public static final PiMatchFieldId HDR_IPV4_SRC_ADDR =
            PiMatchFieldId.of("hdr.ipv4.src_addr");
    public static final PiMatchFieldId HDR_VLAN_TAG_VLAN_ID =
            PiMatchFieldId.of("hdr.vlan_tag.vlan_id");
    public static final PiMatchFieldId HDR_MPLS_LABEL =
            PiMatchFieldId.of("hdr.mpls.label");
    public static final PiMatchFieldId HDR_IPV6_DST_ADDR =
            PiMatchFieldId.of("hdr.ipv6.dst_addr");
    public static final PiMatchFieldId HDR_ETHERNET_SRC_ADDR =
            PiMatchFieldId.of("hdr.ethernet.src_addr");
    public static final PiMatchFieldId HDR_ICMP_ICMP_TYPE =
            PiMatchFieldId.of("hdr.icmp.icmp_type");
    public static final PiMatchFieldId STANDARD_METADATA_EGRESS_PORT =
            PiMatchFieldId.of("standard_metadata.egress_port");
    public static final PiMatchFieldId FABRIC_METADATA_NEXT_ID =
            PiMatchFieldId.of("fabric_metadata.next_id");
    public static final PiMatchFieldId FABRIC_METADATA_L4_DST_PORT =
            PiMatchFieldId.of("fabric_metadata.l4_dst_port");
    public static final PiMatchFieldId STANDARD_METADATA_INGRESS_PORT =
            PiMatchFieldId.of("standard_metadata.ingress_port");
    public static final PiMatchFieldId FABRIC_METADATA_ORIGINAL_ETHER_TYPE =
            PiMatchFieldId.of("fabric_metadata.original_ether_type");
    public static final PiMatchFieldId HDR_IPV4_DST_ADDR =
            PiMatchFieldId.of("hdr.ipv4.dst_addr");
    public static final PiMatchFieldId HDR_VLAN_TAG_IS_VALID =
            PiMatchFieldId.of("hdr.vlan_tag.is_valid");
    public static final PiMatchFieldId FABRIC_METADATA_IP_PROTO =
            PiMatchFieldId.of("fabric_metadata.ip_proto");
    public static final PiMatchFieldId HDR_ETHERNET_DST_ADDR =
            PiMatchFieldId.of("hdr.ethernet.dst_addr");
    public static final PiMatchFieldId HDR_ICMP_ICMP_CODE =
            PiMatchFieldId.of("hdr.icmp.icmp_code");
    // Table IDs
    public static final PiTableId FABRIC_INGRESS_FORWARDING_ACL =
            PiTableId.of("FabricIngress.forwarding.acl");
    public static final PiTableId FABRIC_INGRESS_NEXT_HASHED =
            PiTableId.of("FabricIngress.next.hashed");
    public static final PiTableId FABRIC_EGRESS_EGRESS_NEXT_EGRESS_VLAN =
            PiTableId.of("FabricEgress.egress_next.egress_vlan");
    public static final PiTableId FABRIC_INGRESS_FORWARDING_MPLS =
            PiTableId.of("FabricIngress.forwarding.mpls");
    public static final PiTableId FABRIC_INGRESS_NEXT_MULTICAST =
            PiTableId.of("FabricIngress.next.multicast");
    public static final PiTableId FABRIC_INGRESS_FORWARDING_MULTICAST_V4 =
            PiTableId.of("FabricIngress.forwarding.multicast_v4");
    public static final PiTableId FABRIC_INGRESS_FORWARDING_MULTICAST_V6 =
            PiTableId.of("FabricIngress.forwarding.multicast_v6");
    public static final PiTableId FABRIC_INGRESS_FORWARDING_UNICAST_V4 =
            PiTableId.of("FabricIngress.forwarding.unicast_v4");
    public static final PiTableId FABRIC_INGRESS_FILTERING_FWD_CLASSIFIER =
            PiTableId.of("FabricIngress.filtering.fwd_classifier");
    public static final PiTableId FABRIC_INGRESS_FORWARDING_BRIDGING =
            PiTableId.of("FabricIngress.forwarding.bridging");
    public static final PiTableId FABRIC_INGRESS_NEXT_SIMPLE =
            PiTableId.of("FabricIngress.next.simple");
    public static final PiTableId FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN =
            PiTableId.of("FabricIngress.filtering.ingress_port_vlan");
    public static final PiTableId FABRIC_INGRESS_FORWARDING_UNICAST_V6 =
            PiTableId.of("FabricIngress.forwarding.unicast_v6");
    public static final PiTableId FABRIC_INGRESS_NEXT_VLAN_META =
            PiTableId.of("FabricIngress.next.vlan_meta");
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
    public static final PiCounterId FABRIC_INGRESS_FORWARDING_MULTICAST_V6_COUNTER =
            PiCounterId.of("FabricIngress.forwarding.multicast_v6_counter");
    public static final PiCounterId FABRIC_INGRESS_FORWARDING_MULTICAST_V4_COUNTER =
            PiCounterId.of("FabricIngress.forwarding.multicast_v4_counter");
    public static final PiCounterId FABRIC_INGRESS_FORWARDING_UNICAST_V6_COUNTER =
            PiCounterId.of("FabricIngress.forwarding.unicast_v6_counter");
    public static final PiCounterId FABRIC_INGRESS_FORWARDING_UNICAST_V4_COUNTER =
            PiCounterId.of("FabricIngress.forwarding.unicast_v4_counter");
    public static final PiCounterId FABRIC_INGRESS_NEXT_SIMPLE_COUNTER =
            PiCounterId.of("FabricIngress.next.simple_counter");
    public static final PiCounterId FABRIC_INGRESS_FILTERING_INGRESS_PORT_VLAN_COUNTER =
            PiCounterId.of("FabricIngress.filtering.ingress_port_vlan_counter");
    public static final PiCounterId FABRIC_INGRESS_FORWARDING_MPLS_COUNTER =
            PiCounterId.of("FabricIngress.forwarding.mpls_counter");
    public static final PiCounterId FABRIC_INGRESS_NEXT_HASHED_COUNTER =
            PiCounterId.of("FabricIngress.next.hashed_counter");
    // Action IDs
    public static final PiActionId FABRIC_INGRESS_FILTERING_DROP =
            PiActionId.of("FabricIngress.filtering.drop");
    public static final PiActionId FABRIC_INGRESS_NEXT_SET_VLAN =
            PiActionId.of("FabricIngress.next.set_vlan");
    public static final PiActionId FABRIC_INGRESS_FORWARDING_POP_MPLS_AND_NEXT =
            PiActionId.of("FabricIngress.forwarding.pop_mpls_and_next");
    public static final PiActionId FABRIC_EGRESS_EGRESS_NEXT_POP_VLAN =
            PiActionId.of("FabricEgress.egress_next.pop_vlan");
    public static final PiActionId FABRIC_INGRESS_FILTERING_SET_FORWARDING_TYPE =
            PiActionId.of("FabricIngress.filtering.set_forwarding_type");
    public static final PiActionId NOP = PiActionId.of("nop");
    public static final PiActionId FABRIC_INGRESS_FILTERING_SET_VLAN =
            PiActionId.of("FabricIngress.filtering.set_vlan");
    public static final PiActionId FABRIC_INGRESS_NEXT_MPLS_ROUTING_V6 =
            PiActionId.of("FabricIngress.next.mpls_routing_v6");
    public static final PiActionId FABRIC_EGRESS_PKT_IO_EGRESS_POP_VLAN =
            PiActionId.of("FabricEgress.pkt_io_egress.pop_vlan");
    public static final PiActionId FABRIC_INGRESS_NEXT_L3_ROUTING_VLAN =
            PiActionId.of("FabricIngress.next.l3_routing_vlan");
    public static final PiActionId NO_ACTION = PiActionId.of("NoAction");
    public static final PiActionId FABRIC_INGRESS_NEXT_SET_MCAST_GROUP =
            PiActionId.of("FabricIngress.next.set_mcast_group");
    public static final PiActionId FABRIC_INGRESS_FORWARDING_DUPLICATE_TO_CONTROLLER =
            PiActionId.of("FabricIngress.forwarding.duplicate_to_controller");
    public static final PiActionId FABRIC_INGRESS_NEXT_L3_ROUTING =
            PiActionId.of("FabricIngress.next.l3_routing");
    public static final PiActionId FABRIC_INGRESS_NEXT_MPLS_ROUTING_V4 =
            PiActionId.of("FabricIngress.next.mpls_routing_v4");
    public static final PiActionId FABRIC_INGRESS_NEXT_SET_VLAN_OUTPUT =
            PiActionId.of("FabricIngress.next.set_vlan_output");
    public static final PiActionId FABRIC_INGRESS_FORWARDING_SET_NEXT_ID =
            PiActionId.of("FabricIngress.forwarding.set_next_id");
    public static final PiActionId FABRIC_INGRESS_FILTERING_PUSH_INTERNAL_VLAN =
            PiActionId.of("FabricIngress.filtering.push_internal_vlan");
    public static final PiActionId FABRIC_INGRESS_FORWARDING_DROP =
            PiActionId.of("FabricIngress.forwarding.drop");
    public static final PiActionId FABRIC_INGRESS_NEXT_OUTPUT =
            PiActionId.of("FabricIngress.next.output");
    // Action Param IDs
    public static final PiActionParamId DMAC = PiActionParamId.of("dmac");
    public static final PiActionParamId PORT_NUM =
            PiActionParamId.of("port_num");
    public static final PiActionParamId LABEL = PiActionParamId.of("label");
    public static final PiActionParamId SMAC = PiActionParamId.of("smac");
    public static final PiActionParamId GID = PiActionParamId.of("gid");
    public static final PiActionParamId NEW_VLAN_ID =
            PiActionParamId.of("new_vlan_id");
    public static final PiActionParamId FWD_TYPE =
            PiActionParamId.of("fwd_type");
    public static final PiActionParamId NEXT_ID = PiActionParamId.of("next_id");
    // Action Profile IDs
    public static final PiActionProfileId FABRIC_INGRESS_NEXT_ECMP_SELECTOR =
            PiActionProfileId.of("FabricIngress.next.ecmp_selector");
    // Packet Metadata IDs
    public static final PiControlMetadataId INGRESS_PORT =
            PiControlMetadataId.of("ingress_port");
    public static final PiControlMetadataId EGRESS_PORT =
            PiControlMetadataId.of("egress_port");
    public static final int PORT_BITWIDTH = 9;
}