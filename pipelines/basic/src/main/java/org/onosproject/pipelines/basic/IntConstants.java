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
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiTableId;

import static org.onosproject.pipelines.basic.BasicConstants.*;

/**
 * Constants for INT pipeline.
 */
public final class IntConstants {

    // Hide default constructor
    private IntConstants() {
    }

    // Strings
    private static final String EGRESS = "egress";
    private static final String CTRL_SET_SOURCE_SINK = EGRESS + DOT + "process_set_source_sink";
    private static final String CTRL_INT_SOURCE = EGRESS + DOT + "process_int_source";
    private static final String CTRL_INT_TRANSIT = EGRESS + DOT + "process_int_transit";
    private static final String CTRL_INT_SINK = EGRESS + DOT + "process_int_sink";
    private static final String CTRL_INT_OUTER_ENCAP = EGRESS + DOT + "process_int_outer_encap";
    private static final String CTRL_INT_REPORT = EGRESS + DOT + "process_int_report";
    private static final String INT_METADATA = "int_meta";
    private static final String INT_HDR = "int_header";

    // Header field IDs
    public static final PiMatchFieldId LOCAL_META_SRC_PORT_ID =
            PiMatchFieldId.of(LOCAL_METADATA + DOT + "l4_src_port");
    public static final PiMatchFieldId LOCAL_META_DST_PORT_ID =
            PiMatchFieldId.of(LOCAL_METADATA + DOT + "l4_dst_port");
    public static final PiMatchFieldId INT_META_SINK_ID =
            PiMatchFieldId.of(LOCAL_METADATA + DOT + INT_METADATA + DOT + "sink");
    public static final PiMatchFieldId INT_HDR_INST_MASK_0003_ID =
            PiMatchFieldId.of(HDR + DOT + INT_HDR + DOT + "instruction_mask_0003");
    public static final PiMatchFieldId INT_HDR_INST_MASK_0407_ID =
            PiMatchFieldId.of(HDR + DOT + INT_HDR + DOT + "instruction_mask_0407");
    public static final PiMatchFieldId HDR_OUT_PORT_ID =
            PiMatchFieldId.of(STANDARD_METADATA + DOT + "egress_port");
    public static final PiMatchFieldId STD_META_INSTANCE_TYPE_ID =
            PiMatchFieldId.of(STANDARD_METADATA + DOT + "instance_type");

    // Table IDs
    public static final PiTableId TBL_SET_SOURCE_ID =
            PiTableId.of(CTRL_SET_SOURCE_SINK + DOT + "tb_set_source");
    public static final PiTableId TBL_SET_SINK_ID =
            PiTableId.of(CTRL_SET_SOURCE_SINK + DOT + "tb_set_sink");
    public static final PiTableId TBL_INT_SOURCE_ID =
            PiTableId.of(CTRL_INT_SOURCE + DOT + "tb_int_source");
    public static final PiTableId TBL_INT_INSERT_ID =
            PiTableId.of(CTRL_INT_TRANSIT + DOT + "tb_int_insert");
    public static final PiTableId TBL_INT_INST_0003_ID =
            PiTableId.of(CTRL_INT_TRANSIT + DOT + "tb_int_inst_0003");
    public static final PiTableId TBL_INT_INST_0407_ID =
            PiTableId.of(CTRL_INT_TRANSIT + DOT + "tb_int_inst_0407");
    public static final PiTableId TBL_GENERATE_REPORT_ID =
            PiTableId.of(CTRL_INT_REPORT + DOT + "tb_generate_report");

    // Counter IDs
    public static final PiCounterId CNT_SET_SOURCE_SINK_ID =
            PiCounterId.of(CTRL_SET_SOURCE_SINK + DOT + "counter_set_source_sink");
    public static final PiCounterId CNT_INT_SOURCE_ID =
            PiCounterId.of(CTRL_INT_SOURCE + DOT + "counter_int_source");
    public static final PiCounterId CNT_INT_INSERT_ID =
            PiCounterId.of(CTRL_INT_TRANSIT + DOT + "counter_int_insert");
    public static final PiCounterId CNT_INT_INST_0003_ID =
            PiCounterId.of(CTRL_INT_TRANSIT + DOT + "counter_int_inst_0003");
    public static final PiCounterId CNT_INT_INST_0407_ID =
            PiCounterId.of(CTRL_INT_TRANSIT + DOT + "counter_int_inst_0407");

    // Action IDs
    public static final PiActionId ACT_INT_SET_SOURCE_ID =
            PiActionId.of(CTRL_SET_SOURCE_SINK + DOT + "int_set_source");
    public static final PiActionId ACT_INT_SET_SINK_ID =
            PiActionId.of(CTRL_SET_SOURCE_SINK + DOT + "int_set_sink");
    public static final PiActionId ACT_INT_SOURCE_DSCP_ID =
            PiActionId.of(CTRL_INT_SOURCE + DOT + "int_source_dscp");
    public static final PiActionId ACT_INT_UPDATE_TOTAL_HOP_CNT_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_update_total_hop_cnt");
    public static final PiActionId ACT_INT_TRANSIT_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_transit");
    public static final PiActionId ACT_INT_SET_HEADER_0003_I0_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0003_i0");
    public static final PiActionId ACT_INT_SET_HEADER_0003_I1_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0003_i1");
    public static final PiActionId ACT_INT_SET_HEADER_0003_I2_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0003_i2");
    public static final PiActionId ACT_INT_SET_HEADER_0003_I3_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0003_i3");
    public static final PiActionId ACT_INT_SET_HEADER_0003_I4_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0003_i4");
    public static final PiActionId ACT_INT_SET_HEADER_0003_I5_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0003_i5");
    public static final PiActionId ACT_INT_SET_HEADER_0003_I6_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0003_i6");
    public static final PiActionId ACT_INT_SET_HEADER_0003_I7_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0003_i7");
    public static final PiActionId ACT_INT_SET_HEADER_0003_I8_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0003_i8");
    public static final PiActionId ACT_INT_SET_HEADER_0003_I9_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0003_i9");
    public static final PiActionId ACT_INT_SET_HEADER_0003_I10_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0003_i10");
    public static final PiActionId ACT_INT_SET_HEADER_0003_I11_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0003_i11");
    public static final PiActionId ACT_INT_SET_HEADER_0003_I12_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0003_i12");
    public static final PiActionId ACT_INT_SET_HEADER_0003_I13_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0003_i13");
    public static final PiActionId ACT_INT_SET_HEADER_0003_I14_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0003_i14");
    public static final PiActionId ACT_INT_SET_HEADER_0003_I15_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0003_i15");
    public static final PiActionId ACT_INT_SET_HEADER_0407_I0_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0407_i0");
    public static final PiActionId ACT_INT_SET_HEADER_0407_I1_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0407_i1");
    public static final PiActionId ACT_INT_SET_HEADER_0407_I2_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0407_i2");
    public static final PiActionId ACT_INT_SET_HEADER_0407_I3_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0407_i3");
    public static final PiActionId ACT_INT_SET_HEADER_0407_I4_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0407_i4");
    public static final PiActionId ACT_INT_SET_HEADER_0407_I5_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0407_i5");
    public static final PiActionId ACT_INT_SET_HEADER_0407_I6_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0407_i6");
    public static final PiActionId ACT_INT_SET_HEADER_0407_I7_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0407_i7");
    public static final PiActionId ACT_INT_SET_HEADER_0407_I8_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0407_i8");
    public static final PiActionId ACT_INT_SET_HEADER_0407_I9_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0407_i9");
    public static final PiActionId ACT_INT_SET_HEADER_0407_I10_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0407_i10");
    public static final PiActionId ACT_INT_SET_HEADER_0407_I11_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0407_i11");
    public static final PiActionId ACT_INT_SET_HEADER_0407_I12_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0407_i12");
    public static final PiActionId ACT_INT_SET_HEADER_0407_I13_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0407_i13");
    public static final PiActionId ACT_INT_SET_HEADER_0407_I14_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0407_i14");
    public static final PiActionId ACT_INT_SET_HEADER_0407_I15_ID =
            PiActionId.of(CTRL_INT_TRANSIT + DOT + "int_set_header_0407_i15");
    public static final PiActionId ACT_INT_UPDATE_IPV4_ID =
            PiActionId.of(CTRL_INT_OUTER_ENCAP + DOT + "int_update_ipv4");
    public static final PiActionId ACT_INT_UPDATE_UDP_ID =
            PiActionId.of(CTRL_INT_OUTER_ENCAP + DOT + "int_update_udp");
    public static final PiActionId ACT_INT_UPDATE_SHIM_ID =
            PiActionId.of(CTRL_INT_OUTER_ENCAP + DOT + "int_update_shim");
    public static final PiActionId ACT_INT_RESTORE_HEADER_ID =
            PiActionId.of(CTRL_INT_SINK + DOT + "restore_header");
    public static final PiActionId ACT_INT_SINK_ID =
            PiActionId.of(CTRL_INT_SINK + DOT + "int_sink");
    public static final PiActionId ACT_DO_REPORT_ENCAP_ID =
            PiActionId.of(CTRL_INT_REPORT + DOT + "do_report_encapsulation");

    // Action param IDs
    public static final PiActionParamId ACT_PRM_MAX_HOP_ID = PiActionParamId.of("max_hop");
    public static final PiActionParamId ACT_PRM_INS_CNT_ID = PiActionParamId.of("ins_cnt");
    public static final PiActionParamId ACT_PRM_INS_MASK0003_ID = PiActionParamId.of("ins_mask0003");
    public static final PiActionParamId ACT_PRM_INS_MASK0407_ID = PiActionParamId.of("ins_mask0407");
    public static final PiActionParamId ACT_PRM_SWITCH_ID = PiActionParamId.of("switch_id");
    public static final PiActionParamId ACT_PRM_SRC_MAC_ID = PiActionParamId.of("src_mac");
    public static final PiActionParamId ACT_PRM_MON_MAC_ID = PiActionParamId.of("mon_mac");
    public static final PiActionParamId ACT_PRM_SRC_IP_ID = PiActionParamId.of("src_ip");
    public static final PiActionParamId ACT_PRM_MON_IP_ID = PiActionParamId.of("mon_ip");
    public static final PiActionParamId ACT_PRM_MON_PORT_ID = PiActionParamId.of("mon_port");

}
