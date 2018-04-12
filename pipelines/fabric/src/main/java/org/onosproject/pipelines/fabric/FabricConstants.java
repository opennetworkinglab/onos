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

    public static final String DOT = ".";
    // Header IDs
    public static final String HDR = "hdr";
    public static final String STANDARD_METADATA = "standard_metadata";
    public static final String MPLS = "mpls";
    public static final String FABRIC_METADATA = "fabric_metadata";
    public static final String IPV4 = "ipv4";
    public static final String IPV6 = "ipv6";
    public static final String ETHERNET = "ethernet";
    public static final String VLAN_TAG = "vlan_tag";
    public static final String ICMP = "icmp";

    // Header field IDs
    public static final PiMatchFieldId HF_FABRIC_METADATA_L4_SRC_PORT_ID =
            buildPiMatchField(FABRIC_METADATA, "l4_src_port", false);
    public static final PiMatchFieldId HF_IPV4_SRC_ADDR_ID =
            buildPiMatchField(IPV4, "src_addr", true);
    public static final PiMatchFieldId HF_VLAN_TAG_VLAN_ID_ID =
            buildPiMatchField(VLAN_TAG, "vlan_id", true);
    public static final PiMatchFieldId HF_MPLS_LABEL_ID =
            buildPiMatchField(MPLS, "label", true);
    public static final PiMatchFieldId HF_IPV6_DST_ADDR_ID =
            buildPiMatchField(IPV6, "dst_addr", true);
    public static final PiMatchFieldId HF_ETHERNET_SRC_ADDR_ID =
            buildPiMatchField(ETHERNET, "src_addr", true);
    public static final PiMatchFieldId HF_ICMP_ICMP_TYPE_ID =
            buildPiMatchField(ICMP, "icmp_type", true);
    public static final PiMatchFieldId HF_FABRIC_METADATA_NEXT_ID_ID =
            buildPiMatchField(FABRIC_METADATA, "next_id", false);
    public static final PiMatchFieldId HF_FABRIC_METADATA_L4_DST_PORT_ID =
            buildPiMatchField(FABRIC_METADATA, "l4_dst_port", false);
    public static final PiMatchFieldId HF_STANDARD_METADATA_INGRESS_PORT_ID =
            buildPiMatchField(STANDARD_METADATA, "ingress_port", false);
    public static final PiMatchFieldId HF_FABRIC_METADATA_ORIGINAL_ETHER_TYPE_ID =
            buildPiMatchField(FABRIC_METADATA, "original_ether_type", false);
    public static final PiMatchFieldId HF_IPV4_DST_ADDR_ID =
            buildPiMatchField(IPV4, "dst_addr", true);
    public static final PiMatchFieldId HF_VLAN_TAG_IS_VALID_ID =
            buildPiMatchField(VLAN_TAG, "is_valid", true);
    public static final PiMatchFieldId HF_FABRIC_METADATA_IP_PROTO_ID =
            buildPiMatchField(FABRIC_METADATA, "ip_proto", false);
    public static final PiMatchFieldId HF_ETHERNET_DST_ADDR_ID =
            buildPiMatchField(ETHERNET, "dst_addr", true);
    public static final PiMatchFieldId HF_ICMP_ICMP_CODE_ID =
            buildPiMatchField(ICMP, "icmp_code", true);

    private static PiMatchFieldId buildPiMatchField(String header, String field, boolean withHdrPrefix) {
        if (withHdrPrefix) {
            return PiMatchFieldId.of(HDR + DOT + header + DOT + field);
        } else {
            return PiMatchFieldId.of(header + DOT + field);
        }
    }

    // Table IDs
    public static final PiTableId TBL_ACL_ID =
            PiTableId.of("FabricIngress.forwarding.acl");
    public static final PiTableId TBL_HASHED_ID =
            PiTableId.of("FabricIngress.next.hashed");
    public static final PiTableId TBL_MPLS_ID =
            PiTableId.of("FabricIngress.forwarding.mpls");
    public static final PiTableId TBL_MULTICAST_ID =
            PiTableId.of("FabricIngress.next.multicast");
    public static final PiTableId TBL_MULTICAST_V4_ID =
            PiTableId.of("FabricIngress.forwarding.multicast_v4");
    public static final PiTableId TBL_MULTICAST_V6_ID =
            PiTableId.of("FabricIngress.forwarding.multicast_v6");
    public static final PiTableId TBL_UNICAST_V4_ID =
            PiTableId.of("FabricIngress.forwarding.unicast_v4");
    public static final PiTableId TBL_FWD_CLASSIFIER_ID =
            PiTableId.of("FabricIngress.filtering.fwd_classifier");
    public static final PiTableId TBL_BRIDGING_ID =
            PiTableId.of("FabricIngress.forwarding.bridging");
    public static final PiTableId TBL_INGRESS_PORT_VLAN_ID =
            PiTableId.of("FabricIngress.filtering.ingress_port_vlan");
    public static final PiTableId TBL_UNICAST_V6_ID =
            PiTableId.of("FabricIngress.forwarding.unicast_v6");
    public static final PiTableId TBL_SIMPLE_ID =
            PiTableId.of("FabricIngress.next.simple");

    // Indirect Counter IDs
    public static final PiCounterId CNT_EGRESS_PORT_COUNTER_ID =
            PiCounterId.of("FabricIngress.port_counters_control.egress_port_counter");
    public static final PiCounterId CNT_INGRESS_PORT_COUNTER_ID =
            PiCounterId.of("FabricIngress.port_counters_control.ingress_port_counter");

    // Direct Counter IDs
    public static final PiCounterId CNT_ACL_COUNTER_ID =
            PiCounterId.of("FabricIngress.forwarding.acl_counter");
    public static final PiCounterId CNT_MULTICAST_COUNTER_ID =
            PiCounterId.of("FabricIngress.next.multicast_counter");
    public static final PiCounterId CNT_SIMPLE_COUNTER_ID =
            PiCounterId.of("FabricIngress.next.simple_counter");
    public static final PiCounterId CNT_FWD_CLASSIFIER_COUNTER_ID =
            PiCounterId.of("FabricIngress.filtering.fwd_classifier_counter");
    public static final PiCounterId CNT_BRIDGING_COUNTER_ID =
            PiCounterId.of("FabricIngress.forwarding.bridging_counter");
    public static final PiCounterId CNT_MULTICAST_V6_COUNTER_ID =
            PiCounterId.of("FabricIngress.forwarding.multicast_v6_counter");
    public static final PiCounterId CNT_MULTICAST_V4_COUNTER_ID =
            PiCounterId.of("FabricIngress.forwarding.multicast_v4_counter");
    public static final PiCounterId CNT_UNICAST_V6_COUNTER_ID =
            PiCounterId.of("FabricIngress.forwarding.unicast_v6_counter");
    public static final PiCounterId CNT_UNICAST_V4_COUNTER_ID =
            PiCounterId.of("FabricIngress.forwarding.unicast_v4_counter");
    public static final PiCounterId CNT_INGRESS_PORT_VLAN_COUNTER_ID =
            PiCounterId.of("FabricIngress.filtering.ingress_port_vlan_counter");
    public static final PiCounterId CNT_MPLS_COUNTER_ID =
            PiCounterId.of("FabricIngress.forwarding.mpls_counter");
    public static final PiCounterId CNT_HASHED_COUNTER_ID =
            PiCounterId.of("FabricIngress.next.hashed_counter");

    // Action IDs
    public static final PiActionId ACT_FABRICINGRESS_FILTERING_DROP_ID =
            PiActionId.of("FabricIngress.filtering.drop");
    public static final PiActionId ACT_FABRICINGRESS_FORWARDING_POP_MPLS_AND_NEXT_ID =
            PiActionId.of("FabricIngress.forwarding.pop_mpls_and_next");
    public static final PiActionId ACT_FABRICINGRESS_FILTERING_SET_FORWARDING_TYPE_ID =
            PiActionId.of("FabricIngress.filtering.set_forwarding_type");
    public static final PiActionId ACT_NOP_ID = PiActionId.of("nop");
    public static final PiActionId ACT_FABRICINGRESS_FILTERING_SET_VLAN_ID =
            PiActionId.of("FabricIngress.filtering.set_vlan");
    public static final PiActionId ACT_FABRICINGRESS_NEXT_MPLS_ROUTING_V6_ID =
            PiActionId.of("FabricIngress.next.mpls_routing_v6");
    public static final PiActionId ACT_NOACTION_ID = PiActionId.of("NoAction");
    public static final PiActionId ACT_FABRICINGRESS_NEXT_SET_MCAST_GROUP_ID =
            PiActionId.of("FabricIngress.next.set_mcast_group");
    public static final PiActionId ACT_FABRICINGRESS_FORWARDING_DUPLICATE_TO_CONTROLLER_ID =
            PiActionId.of("FabricIngress.forwarding.duplicate_to_controller");
    public static final PiActionId ACT_FABRICINGRESS_NEXT_L3_ROUTING_ID =
            PiActionId.of("FabricIngress.next.l3_routing");
    public static final PiActionId ACT_FABRICINGRESS_NEXT_MPLS_ROUTING_V4_ID =
            PiActionId.of("FabricIngress.next.mpls_routing_v4");
    public static final PiActionId ACT_FABRICINGRESS_NEXT_SET_VLAN_OUTPUT_ID =
            PiActionId.of("FabricIngress.next.set_vlan_output");
    public static final PiActionId ACT_FABRICINGRESS_FORWARDING_SET_NEXT_ID_ID =
            PiActionId.of("FabricIngress.forwarding.set_next_id");
    public static final PiActionId ACT_FABRICINGRESS_FILTERING_PUSH_INTERNAL_VLAN_ID =
            PiActionId.of("FabricIngress.filtering.push_internal_vlan");
    public static final PiActionId ACT_FABRICINGRESS_FORWARDING_DROP_ID =
            PiActionId.of("FabricIngress.forwarding.drop");
    public static final PiActionId ACT_FABRICINGRESS_NEXT_OUTPUT_ID =
            PiActionId.of("FabricIngress.next.output");

    // Action Param IDs
    public static final PiActionParamId ACT_PRM_DMAC_ID =
            PiActionParamId.of("dmac");
    public static final PiActionParamId ACT_PRM_PORT_NUM_ID =
            PiActionParamId.of("port_num");
    public static final PiActionParamId ACT_PRM_LABEL_ID =
            PiActionParamId.of("label");
    public static final PiActionParamId ACT_PRM_SMAC_ID =
            PiActionParamId.of("smac");
    public static final PiActionParamId ACT_PRM_GID_ID =
            PiActionParamId.of("gid");
    public static final PiActionParamId ACT_PRM_NEW_VLAN_ID_ID =
            PiActionParamId.of("new_vlan_id");
    public static final PiActionParamId ACT_PRM_FWD_TYPE_ID =
            PiActionParamId.of("fwd_type");
    public static final PiActionParamId ACT_PRM_NEXT_ID_ID =
            PiActionParamId.of("next_id");

    // Action Profile IDs
    public static final PiActionProfileId ACT_PRF_FABRICINGRESS_NEXT_ECMP_SELECTOR_ID =
            PiActionProfileId.of("FabricIngress.next.ecmp_selector");

    // Packet Metadata IDs
    public static final PiControlMetadataId CTRL_META_INGRESS_PORT_ID =
            PiControlMetadataId.of("ingress_port");
    public static final PiControlMetadataId CTRL_META_EGRESS_PORT_ID =
            PiControlMetadataId.of("egress_port");

    public static final int PORT_BITWIDTH = 9;
}
