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

import org.onosproject.net.pi.runtime.PiActionId;
import org.onosproject.net.pi.runtime.PiActionParamId;
import org.onosproject.net.pi.runtime.PiActionProfileId;
import org.onosproject.net.pi.runtime.PiCounterId;
import org.onosproject.net.pi.runtime.PiCounterType;
import org.onosproject.net.pi.runtime.PiHeaderFieldId;
import org.onosproject.net.pi.runtime.PiPacketMetadataId;
import org.onosproject.net.pi.runtime.PiTableId;

/**
 * Constants for the basic.p4 program.
 */
public final class BasicConstants {

    // TODO: constants could be auto-generated starting from the P4info.

    // Header field IDs
    public static final String ETHERNET = "ethernet";
    public static final String LOCAL_METADATA = "local_metadata";
    public static final String STANDARD_METADATA = "standard_metadata";
    public static final PiHeaderFieldId HDR_IN_PORT_ID = PiHeaderFieldId.of(STANDARD_METADATA, "ingress_port");
    public static final PiHeaderFieldId HDR_ETH_DST_ID = PiHeaderFieldId.of(ETHERNET, "dst_addr");
    public static final PiHeaderFieldId HDR_ETH_SRC_ID = PiHeaderFieldId.of(ETHERNET, "src_addr");
    public static final PiHeaderFieldId HDR_ETH_TYPE_ID = PiHeaderFieldId.of(ETHERNET, "ether_type");
    public static final PiHeaderFieldId HDR_NEXT_HOP_ID = PiHeaderFieldId.of(LOCAL_METADATA, "next_hop_id");
    public static final PiHeaderFieldId HDR_SELECTOR_ID = PiHeaderFieldId.of(LOCAL_METADATA, "selector");
    // Table IDs
    public static final PiTableId TBL_TABLE0_ID = PiTableId.of("table0_control.table0");
    public static final PiTableId TBL_WCMP_TABLE_ID = PiTableId.of("wcmp_control.wcmp_table");
    // Counter IDs
    public static final PiCounterId CNT_TABLE0_ID = PiCounterId.of("table0_control.table0_counter",
                                                                   PiCounterType.DIRECT);
    public static final PiCounterId CNT_WCMP_TABLE_ID = PiCounterId.of("wcmp_control.wcmp_table_counter",
                                                                       PiCounterType.DIRECT);
    // Action IDs
    public static final PiActionId ACT_NOACTION_ID = PiActionId.of("NoAction");
    public static final PiActionId ACT_DROP_ID = PiActionId.of("_drop");
    public static final PiActionId ACT_SET_EGRESS_PORT_ID = PiActionId.of("set_egress_port");
    public static final PiActionId ACT_SET_NEXT_HOP_ID = PiActionId.of("table0_control.set_next_hop_id");
    public static final PiActionId ACT_SEND_TO_CPU_ID = PiActionId.of("send_to_cpu");
    // Action Param IDs
    public static final PiActionParamId ACT_PRM_PORT_ID = PiActionParamId.of("port");
    public static final PiActionParamId ACT_PRM_NEXT_HOP_ID = PiActionParamId.of("next_hop_id");
    // Action Profile IDs
    public static final PiActionProfileId ACT_PRF_WCMP_SELECTOR_ID = PiActionProfileId.of("wcmp_selector");
    // Packet Metadata IDs
    public static final PiPacketMetadataId PKT_META_EGRESS_PORT_ID = PiPacketMetadataId.of("egress_port");
    public static final PiPacketMetadataId PKT_META_INGRESS_PORT_ID = PiPacketMetadataId.of("ingress_port");
    // Bitwidths
    public static final int PORT_BITWIDTH = 9;

    private BasicConstants() {
        // Hides constructor.
    }
}
