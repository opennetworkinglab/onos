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
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiControlMetadataId;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiTableId;

/**
 * Constants for the basic.p4 program.
 */
public final class BasicConstants {

    // TODO: constants could be auto-generated starting from the P4info.

    public static final String DOT = ".";
    private static final String INGRESS = "ingress";
    public static final String EGRESS = "egress";
    private static final String TABLE0_CTRL = INGRESS + DOT + "table0_control";
    private static final String WCMP_CTRL = INGRESS + DOT + "wcmp_control";
    private static final String PORT_COUNT_INGRESS_CTRL = INGRESS + DOT + "port_counters_ingress";
    private static final String PORT_COUNT_EGRESS_CTRL = EGRESS + DOT + "port_counters_egress";
    public static final String HDR = "hdr";
    public static final String ETHERNET = "ethernet";
    public static final String IPV4 = "ipv4";
    public static final String LOCAL_METADATA = "local_metadata";
    public static final String STANDARD_METADATA = "standard_metadata";

    // Header field IDs

    public static final PiMatchFieldId HDR_IN_PORT_ID =
            PiMatchFieldId.of(STANDARD_METADATA + DOT + "ingress_port");
    public static final PiMatchFieldId HDR_ETH_DST_ID =
            PiMatchFieldId.of(HDR + DOT + ETHERNET + DOT + "dst_addr");
    public static final PiMatchFieldId HDR_ETH_SRC_ID =
            PiMatchFieldId.of(HDR + DOT + ETHERNET + DOT + "src_addr");
    public static final PiMatchFieldId HDR_ETH_TYPE_ID =
            PiMatchFieldId.of(HDR + DOT + ETHERNET + DOT + "ether_type");
    public static final PiMatchFieldId HDR_IPV4_DST_ID =
            PiMatchFieldId.of(HDR + DOT + IPV4 + DOT + "dst_addr");
    public static final PiMatchFieldId HDR_IPV4_SRC_ID =
            PiMatchFieldId.of(HDR + DOT + IPV4 + DOT + "src_addr");
    public static final PiMatchFieldId HDR_NEXT_HOP_ID =
            PiMatchFieldId.of(LOCAL_METADATA + DOT + "next_hop_id");
    public static final PiMatchFieldId HDR_SELECTOR_ID =
            PiMatchFieldId.of(LOCAL_METADATA + DOT + "selector");
    // Table IDs
    public static final PiTableId TBL_TABLE0_ID =
            PiTableId.of(TABLE0_CTRL + DOT + "table0");
    public static final PiTableId TBL_WCMP_TABLE_ID =
            PiTableId.of(WCMP_CTRL + DOT + "wcmp_table");
    // Counter IDs
    public static final PiCounterId CNT_EGRESS_PORT_COUNTER_ID =
            PiCounterId.of(PORT_COUNT_EGRESS_CTRL + DOT + "egress_port_counter");
    public static final PiCounterId CNT_INGRESS_PORT_COUNTER_ID =
            PiCounterId.of(PORT_COUNT_INGRESS_CTRL + DOT + "ingress_port_counter");
    public static final PiCounterId CNT_TABLE0_ID =
            PiCounterId.of(TABLE0_CTRL + DOT + "table0_counter");
    public static final PiCounterId CNT_WCMP_TABLE_ID =
            PiCounterId.of(WCMP_CTRL + DOT + "wcmp_table_counter");
    // Action IDs
    public static final PiActionId ACT_NOACTION_ID =
            PiActionId.of("NoAction");
    public static final PiActionId ACT_DROP_ID =
            PiActionId.of("_drop");
    public static final PiActionId ACT_SET_EGRESS_PORT_TABLE0_ID =
            PiActionId.of(TABLE0_CTRL + DOT + "set_egress_port");
    public static final PiActionId ACT_SET_EGRESS_PORT_WCMP_ID =
            PiActionId.of(WCMP_CTRL + DOT + "set_egress_port");
    public static final PiActionId ACT_SET_NEXT_HOP_ID =
            PiActionId.of(TABLE0_CTRL + DOT + "set_next_hop_id");
    public static final PiActionId ACT_SEND_TO_CPU_ID =
            PiActionId.of(TABLE0_CTRL + DOT + "send_to_cpu");
    // Action Param IDs
    public static final PiActionParamId ACT_PRM_PORT_ID =
            PiActionParamId.of("port");
    public static final PiActionParamId ACT_PRM_NEXT_HOP_ID =
            PiActionParamId.of("next_hop_id");
    // Action Profile IDs
    public static final PiActionProfileId ACT_PRF_WCMP_SELECTOR_ID =
            PiActionProfileId.of(WCMP_CTRL + DOT + "wcmp_selector");
    // Packet Metadata IDs
    public static final PiControlMetadataId PKT_META_EGRESS_PORT_ID =
            PiControlMetadataId.of("egress_port");
    public static final PiControlMetadataId PKT_META_INGRESS_PORT_ID =
            PiControlMetadataId.of("ingress_port");
    // Bitwidths
    public static final int PORT_BITWIDTH = 9;

    private BasicConstants() {
        // Hides constructor.
    }
}
