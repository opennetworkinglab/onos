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

package org.onosproject.p4runtime.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.pi.model.PiActionId;
import org.onosproject.net.pi.model.PiActionModel;
import org.onosproject.net.pi.model.PiActionParamId;
import org.onosproject.net.pi.model.PiActionParamModel;
import org.onosproject.net.pi.model.PiActionProfileId;
import org.onosproject.net.pi.model.PiActionProfileModel;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiCounterModel;
import org.onosproject.net.pi.model.PiCounterType;
import org.onosproject.net.pi.model.PiMatchFieldId;
import org.onosproject.net.pi.model.PiMatchFieldModel;
import org.onosproject.net.pi.model.PiMatchType;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiMeterModel;
import org.onosproject.net.pi.model.PiMeterType;
import org.onosproject.net.pi.model.PiPacketMetadataId;
import org.onosproject.net.pi.model.PiPacketMetadataModel;
import org.onosproject.net.pi.model.PiPacketOperationModel;
import org.onosproject.net.pi.model.PiPacketOperationType;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.model.PiRegisterId;
import org.onosproject.net.pi.model.PiRegisterModel;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.model.PiTableModel;
import org.onosproject.net.pi.model.PiTableType;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for P4PipelineModel class.
 */
public class P4PipelineModelTest {

    /* Action Profiles */
    private static final PiActionProfileId PI_ACTION_PROFILE_ID_1 = PiActionProfileId.of("Action1");
    private static final PiActionProfileId PI_ACTION_PROFILE_ID_2 = PiActionProfileId.of("Action2");

    private static final PiTableId ACTION_PI_TABLE_ID_1 = PiTableId.of("ActionTable1");
    private static final PiTableId ACTION_PI_TABLE_ID_2 = PiTableId.of("ActionTable2");

    private static final ImmutableSet<PiTableId> ACTION_TABLES_1 = new ImmutableSet.Builder<PiTableId>()
            .add(ACTION_PI_TABLE_ID_1)
            .build();

    private static final ImmutableSet<PiTableId> ACTION_TABLES_2 = new ImmutableSet.Builder<PiTableId>()
            .add(ACTION_PI_TABLE_ID_2)
            .build();

    private static final boolean ACTION_HAS_SELECTOR_1 = true;
    private static final boolean ACTION_HAS_SELECTOR_2 = false;

    private static final long ACTION_MAX_SIZE_1 = 100;
    private static final long ACTION_MAX_SIZE_2 = 200;

    private static final int ACTION_MAX_GROUP_SIZE_1 = 10;
    private static final int ACTION_MAX_GROUP_SIZE_2 = 20;

    private static final PiActionProfileModel P4_ACTION_PROFILE_MODEL_1 =
            new P4ActionProfileModel(PI_ACTION_PROFILE_ID_1, ACTION_TABLES_1,
                                     ACTION_HAS_SELECTOR_1, ACTION_MAX_SIZE_1,
                                     ACTION_MAX_GROUP_SIZE_1);
    private static final PiActionProfileModel P4_ACTION_PROFILE_MODEL_2 =
            new P4ActionProfileModel(PI_ACTION_PROFILE_ID_2, ACTION_TABLES_2,
                                     ACTION_HAS_SELECTOR_2, ACTION_MAX_SIZE_2,
                                     ACTION_MAX_GROUP_SIZE_2);

    /* Counters */
    private static final PiCounterId PI_COUNTER_ID_1 = PiCounterId.of("Counter1");
    private static final PiCounterId PI_COUNTER_ID_2 = PiCounterId.of("Counter2");

    private static final PiCounterType PI_COUNTER_TYPE_1 = PiCounterType.DIRECT;
    private static final PiCounterType PI_COUNTER_TYPE_2 = PiCounterType.INDIRECT;

    private static final PiCounterModel.Unit COUNTER_UNIT_BYTES = P4CounterModel.Unit.BYTES;
    private static final PiCounterModel.Unit COUNTER_UNIT_PACKETS = P4CounterModel.Unit.PACKETS;

    private static final PiTableId COUNTER_PI_TABLE_ID_1 = PiTableId.of("CounterTable1");
    private static final PiTableId COUNTER_PI_TABLE_ID_2 = PiTableId.of("CounterTable2");

    private static final long COUNTER_SIZE_1 = 1000;
    private static final long COUNTER_SIZE_2 = 2000;

    private static final PiCounterModel P4_COUNTER_MODEL_1 =
            new P4CounterModel(PI_COUNTER_ID_1, PI_COUNTER_TYPE_1,
                               COUNTER_UNIT_BYTES, COUNTER_PI_TABLE_ID_1, COUNTER_SIZE_1);
    private static final PiCounterModel P4_COUNTER_MODEL_2 =
            new P4CounterModel(PI_COUNTER_ID_2, PI_COUNTER_TYPE_2,
                               COUNTER_UNIT_PACKETS, COUNTER_PI_TABLE_ID_2, COUNTER_SIZE_2);

    private static final ImmutableMap<PiCounterId, PiCounterModel> COUNTERS_1 =
            new ImmutableMap.Builder<PiCounterId, PiCounterModel>()
                    .put(PI_COUNTER_ID_1, P4_COUNTER_MODEL_1)
                    .build();
    private static final ImmutableMap<PiCounterId, PiCounterModel> COUNTERS_2 =
            new ImmutableMap.Builder<PiCounterId, PiCounterModel>()
                    .put(PI_COUNTER_ID_2, P4_COUNTER_MODEL_2)
                    .build();

    /* Meters */
    private static final PiMeterId PI_METER_ID_1 = PiMeterId.of("Meter1");
    private static final PiMeterId PI_METER_ID_2 = PiMeterId.of("Meter2");

    private static final PiMeterType PI_METER_TYPE_1 = PiMeterType.DIRECT;
    private static final PiMeterType PI_METER_TYPE_2 = PiMeterType.INDIRECT;

    private static final PiMeterModel.Unit METER_UNIT_BYTES = P4MeterModel.Unit.BYTES;
    private static final PiMeterModel.Unit METER_UNIT_PACKETS = P4MeterModel.Unit.PACKETS;

    private static final PiTableId METER_PI_TABLE_ID_1 = PiTableId.of("MeterTable1");
    private static final PiTableId METER_PI_TABLE_ID_2 = PiTableId.of("MeterTable2");

    private static final long METER_SIZE_1 = 1000;
    private static final long METER_SIZE_2 = 2000;

    private static final PiMeterModel P4_METER_MODEL_1 =
            new P4MeterModel(PI_METER_ID_1, PI_METER_TYPE_1, METER_UNIT_BYTES, METER_PI_TABLE_ID_1, METER_SIZE_1);
    private static final PiMeterModel P4_METER_MODEL_2 =
            new P4MeterModel(PI_METER_ID_2, PI_METER_TYPE_2, METER_UNIT_PACKETS, METER_PI_TABLE_ID_2, METER_SIZE_2);

    private static final ImmutableMap<PiMeterId, PiMeterModel> METERS_1 =
            new ImmutableMap.Builder<PiMeterId, PiMeterModel>()
                    .put(PI_METER_ID_1, P4_METER_MODEL_1)
                    .build();
    private static final ImmutableMap<PiMeterId, PiMeterModel> METERS_2 =
            new ImmutableMap.Builder<PiMeterId, PiMeterModel>()
                    .put(PI_METER_ID_2, P4_METER_MODEL_2)
                    .build();

    /* Registers */
    private static final PiRegisterId PI_REGISTER_ID_1 = PiRegisterId.of("Register1");
    private static final PiRegisterId PI_REGISTER_ID_2 = PiRegisterId.of("Register2");

    private static final long REGISTER_SIZE_1 = 1000;
    private static final long REGISTER_SIZE_2 = 2000;

    private static final P4RegisterModel P4_REGISTER_MODEL_1 = new P4RegisterModel(PI_REGISTER_ID_1, REGISTER_SIZE_1);
    private static final P4RegisterModel P4_REGISTER_MODEL_2 = new P4RegisterModel(PI_REGISTER_ID_2, REGISTER_SIZE_2);

    private static final ImmutableMap<PiRegisterId, PiRegisterModel> REGISTERS_1 =
            new ImmutableMap.Builder<PiRegisterId, PiRegisterModel>()
                    .put(PI_REGISTER_ID_1, P4_REGISTER_MODEL_1)
                    .build();
    private static final ImmutableMap<PiRegisterId, PiRegisterModel> REGISTERS_2 =
            new ImmutableMap.Builder<PiRegisterId, PiRegisterModel>()
                    .put(PI_REGISTER_ID_2, P4_REGISTER_MODEL_2)
                    .build();

    /* Match Fields */
    private static final PiMatchFieldId PI_MATCH_FIELD_ID_1 = PiMatchFieldId.of("MatchField1");
    private static final PiMatchFieldId PI_MATCH_FIELD_ID_2 = PiMatchFieldId.of("MatchField2");

    private static final int MATCH_FIELD_BIT_WIDTH_1 = 8;
    private static final int MATCH_FIELD_BIT_WIDTH_2 = 16;

    private static final PiMatchType PI_MATCH_TYPE_1 = PiMatchType.EXACT;
    private static final PiMatchType PI_MATCH_TYPE_2 = PiMatchType.TERNARY;

    private static final PiMatchFieldModel P4_MATCH_FIELD_MODEL_1 =
            new P4MatchFieldModel(PI_MATCH_FIELD_ID_1, MATCH_FIELD_BIT_WIDTH_1, PI_MATCH_TYPE_1);
    private static final PiMatchFieldModel P4_MATCH_FIELD_MODEL_2 =
            new P4MatchFieldModel(PI_MATCH_FIELD_ID_2, MATCH_FIELD_BIT_WIDTH_2, PI_MATCH_TYPE_2);

    private static final ImmutableMap<PiMatchFieldId, PiMatchFieldModel> MATCH_FIELDS_1 =
            new ImmutableMap.Builder<PiMatchFieldId, PiMatchFieldModel>()
                    .put(PI_MATCH_FIELD_ID_1, P4_MATCH_FIELD_MODEL_1)
                    .build();
    private static final ImmutableMap<PiMatchFieldId, PiMatchFieldModel> MATCH_FIELDS_2 =
            new ImmutableMap.Builder<PiMatchFieldId, PiMatchFieldModel>()
                    .put(PI_MATCH_FIELD_ID_2, P4_MATCH_FIELD_MODEL_2)
                    .build();

    /* Actions */
    private static final PiActionId PI_ACTION_ID_1 = PiActionId.of("Action1");
    private static final PiActionId PI_ACTION_ID_2 = PiActionId.of("Action2");

    private static final PiActionParamId PI_ACTION_PARAM_ID_1 = PiActionParamId.of("ActionParameter1");
    private static final PiActionParamId PI_ACTION_PARAM_ID_2 = PiActionParamId.of("ActionParameter2");

    private static final int ACTION_PARAM_BIT_WIDTH_1 = 8;
    private static final int ACTION_PARAM_BIT_WIDTH_2 = 16;

    private static final PiActionParamModel P4_ACTION_PARAM_MODEL_1 =
            new P4ActionParamModel(PI_ACTION_PARAM_ID_1, ACTION_PARAM_BIT_WIDTH_1);
    private static final PiActionParamModel P4_ACTION_PARAM_MODEL_2 =
            new P4ActionParamModel(PI_ACTION_PARAM_ID_2, ACTION_PARAM_BIT_WIDTH_2);

    private static final ImmutableMap<PiActionParamId, PiActionParamModel> PI_ACTION_PARAMS_1 =
            new ImmutableMap.Builder<PiActionParamId, PiActionParamModel>()
                    .put(PI_ACTION_PARAM_ID_1, P4_ACTION_PARAM_MODEL_1)
                    .build();
    private static final ImmutableMap<PiActionParamId, PiActionParamModel> PI_ACTION_PARAMS_2 =
            new ImmutableMap.Builder<PiActionParamId, PiActionParamModel>()
                    .put(PI_ACTION_PARAM_ID_2, P4_ACTION_PARAM_MODEL_2)
                    .build();

    private static final PiActionModel P4_ACTION_MODEL_1 = new P4ActionModel(PI_ACTION_ID_1, PI_ACTION_PARAMS_1);
    private static final PiActionModel P4_ACTION_MODEL_2 = new P4ActionModel(PI_ACTION_ID_2, PI_ACTION_PARAMS_2);

    private static final ImmutableMap<PiActionId, PiActionModel> ACTIONS_1 =
            new ImmutableMap.Builder<PiActionId, PiActionModel>()
                    .put(PI_ACTION_ID_1, P4_ACTION_MODEL_1)
                    .build();

    private static final ImmutableMap<PiActionId, PiActionModel> ACTIONS_2 =
            new ImmutableMap.Builder<PiActionId, PiActionModel>()
                    .put(PI_ACTION_ID_2, P4_ACTION_MODEL_2)
                    .build();

    /* Default Action */
    private static final PiActionId PI_ACTION_ID_DEFAULT_1 = PiActionId.of("DefaultAction1");
    private static final PiActionId PI_ACTION_ID_DEFAULT_2 = PiActionId.of("DefaultAction2");

    private static final PiActionParamId PI_ACTION_PARAM_ID_DEFAULT_1 = PiActionParamId.of("DefaultActionParameter1");
    private static final PiActionParamId PI_ACTION_PARAM_ID_DEFAULT_2 = PiActionParamId.of("DefaultActionParameter2");

    private static final int ACTION_PARAM_BIT_WIDTH_DEFAULT_1 = 8;
    private static final int ACTION_PARAM_BIT_WIDTH_DEFAULT_2 = 16;

    private static final PiActionParamModel P4_ACTION_PARAM_MODEL_DEFAULT_1 =
            new P4ActionParamModel(PI_ACTION_PARAM_ID_DEFAULT_1, ACTION_PARAM_BIT_WIDTH_DEFAULT_1);
    private static final PiActionParamModel P4_ACTION_PARAM_MODEL_DEFAULT_2 =
            new P4ActionParamModel(PI_ACTION_PARAM_ID_DEFAULT_2, ACTION_PARAM_BIT_WIDTH_DEFAULT_2);

    private static final ImmutableMap<PiActionParamId, PiActionParamModel> PI_ACTION_PARAMS_DEFAULT_1 =
            new ImmutableMap.Builder<PiActionParamId, PiActionParamModel>()
                    .put(PI_ACTION_PARAM_ID_DEFAULT_1, P4_ACTION_PARAM_MODEL_DEFAULT_1)
                    .build();
    private static final ImmutableMap<PiActionParamId, PiActionParamModel> PI_ACTION_PARAMS_DEFAULT_2 =
            new ImmutableMap.Builder<PiActionParamId, PiActionParamModel>()
                    .put(PI_ACTION_PARAM_ID_DEFAULT_2, P4_ACTION_PARAM_MODEL_DEFAULT_2)
                    .build();

    private static final PiActionModel P4_ACTION_MODEL_DEFAULT_1 =
            new P4ActionModel(PI_ACTION_ID_DEFAULT_1, PI_ACTION_PARAMS_DEFAULT_1);
    private static final PiActionModel P4_ACTION_MODEL_DEFAULT_2 =
            new P4ActionModel(PI_ACTION_ID_DEFAULT_2, PI_ACTION_PARAMS_DEFAULT_2);

    /* Table Models */
    private static final PiTableId PI_TABLE_ID_1 = PiTableId.of("Table1");
    private static final PiTableId PI_TABLE_ID_2 = PiTableId.of("Table2");
    private static final PiTableId PI_TABLE_ID_3 = PiTableId.of("Table3");

    private static final PiTableType PI_TABLE_TYPE_1 = PiTableType.DIRECT;
    private static final PiTableType PI_TABLE_TYPE_2 = PiTableType.INDIRECT;

    private static final long MAX_SIZE_1 = 10000;
    private static final long MAX_SIZE_2 = 20000;

    private static final boolean SUPPORT_AGING_1 = true;
    private static final boolean SUPPORT_AGING_2 = false;

    private static final boolean IS_CONST_TABLE_1 = true;
    private static final boolean IS_CONST_TABLE_2 = false;

    private static final PiTableModel P4_TABLE_MODEL_1 =
            new P4TableModel(PI_TABLE_ID_1, PI_TABLE_TYPE_1, P4_ACTION_PROFILE_MODEL_1, MAX_SIZE_1, COUNTERS_1,
                             METERS_1, SUPPORT_AGING_1, MATCH_FIELDS_1, ACTIONS_1, P4_ACTION_MODEL_DEFAULT_1,
                             IS_CONST_TABLE_1, false);
    private static final PiTableModel P4_TABLE_MODEL_2 =
            new P4TableModel(PI_TABLE_ID_2, PI_TABLE_TYPE_2, P4_ACTION_PROFILE_MODEL_2, MAX_SIZE_2, COUNTERS_2,
                             METERS_2, SUPPORT_AGING_2, MATCH_FIELDS_2, ACTIONS_2, P4_ACTION_MODEL_DEFAULT_2,
                             IS_CONST_TABLE_2, false);
    private static final PiTableModel P4_TABLE_MODEL_3 =
            new P4TableModel(PI_TABLE_ID_2, PI_TABLE_TYPE_2, P4_ACTION_PROFILE_MODEL_2, MAX_SIZE_2, COUNTERS_2,
                             METERS_2, SUPPORT_AGING_2, MATCH_FIELDS_2, ACTIONS_2, P4_ACTION_MODEL_DEFAULT_2,
                             IS_CONST_TABLE_2, true);

    /* Packet operations */
    private static final PiPacketOperationType PI_PACKET_OPERATION_TYPE_1 = PiPacketOperationType.PACKET_IN;
    private static final PiPacketOperationType PI_PACKET_OPERATION_TYPE_2 = PiPacketOperationType.PACKET_OUT;

    private static final PiPacketMetadataId PI_CONTROL_METADATA_ID_1 = PiPacketMetadataId.of("INGRESS PORT");
    private static final PiPacketMetadataId PI_CONTROL_METADATA_ID_2 = PiPacketMetadataId.of("EGRESS PORT");

    private static final int META_BIT_WIDTH_1 = 32;
    private static final int META_BIT_WIDTH_2 = 64;

    private static final PiPacketMetadataModel P4_CONTROL_METADATA_MODEL_1 =
            new P4PacketMetadataModel(PI_CONTROL_METADATA_ID_1, META_BIT_WIDTH_1);
    private static final PiPacketMetadataModel P4_CONTROL_METADATA_MODEL_2 =
            new P4PacketMetadataModel(PI_CONTROL_METADATA_ID_2, META_BIT_WIDTH_2);
    private static final PiPacketMetadataModel P4_CONTROL_METADATA_MODEL_3 =
            new P4PacketMetadataModel(PI_CONTROL_METADATA_ID_2, P4PacketMetadataModel.BIT_WIDTH_UNDEFINED);

    /* Pipeline Models */
    private static final ImmutableMap<PiTableId, PiTableModel> TABLES_1 =
            new ImmutableMap.Builder<PiTableId, PiTableModel>()
                    .put(PI_TABLE_ID_1, P4_TABLE_MODEL_1)
                    .build();
    private static final ImmutableMap<PiTableId, PiTableModel> TABLES_2 =
            new ImmutableMap.Builder<PiTableId, PiTableModel>()
                    .put(PI_TABLE_ID_2, P4_TABLE_MODEL_2)
                    .build();
    private static final ImmutableMap<PiTableId, PiTableModel> TABLES_3 =
            new ImmutableMap.Builder<PiTableId, PiTableModel>()
                    .put(PI_TABLE_ID_3, P4_TABLE_MODEL_3)
                    .build();

    private static final ImmutableMap<PiActionProfileId, PiActionProfileModel> ACTION_PROFILES_1 =
            new ImmutableMap.Builder<PiActionProfileId, PiActionProfileModel>()
                    .put(PI_ACTION_PROFILE_ID_1, P4_ACTION_PROFILE_MODEL_1)
                    .build();
    private static final ImmutableMap<PiActionProfileId, PiActionProfileModel> ACTION_PROFILES_2 =
            new ImmutableMap.Builder<PiActionProfileId, PiActionProfileModel>()
                    .put(PI_ACTION_PROFILE_ID_2, P4_ACTION_PROFILE_MODEL_2)
                    .build();

    private static final ImmutableList<PiPacketMetadataModel> METADATAS_1 =
            new ImmutableList.Builder<PiPacketMetadataModel>()
                    .add(P4_CONTROL_METADATA_MODEL_1)
                    .build();
    private static final ImmutableList<PiPacketMetadataModel> METADATAS_2 =
            new ImmutableList.Builder<PiPacketMetadataModel>()
                    .add(P4_CONTROL_METADATA_MODEL_2)
                    .build();
    private static final ImmutableList<PiPacketMetadataModel> METADATAS_3 =
            new ImmutableList.Builder<PiPacketMetadataModel>()
                    .add(P4_CONTROL_METADATA_MODEL_3)
                    .build();

    private static final PiPacketOperationModel P4_PACKET_OPERATION_MODEL_1 =
            new P4PacketOperationModel(PI_PACKET_OPERATION_TYPE_1, METADATAS_1);
    private static final PiPacketOperationModel P4_PACKET_OPERATION_MODEL_2 =
            new P4PacketOperationModel(PI_PACKET_OPERATION_TYPE_2, METADATAS_2);
    private static final PiPacketOperationModel P4_PACKET_OPERATION_MODEL_3 =
            new P4PacketOperationModel(PI_PACKET_OPERATION_TYPE_1, METADATAS_3);

    private static final ImmutableMap<PiPacketOperationType, PiPacketOperationModel> PACKET_OPERATIONS_1 =
            new ImmutableMap.Builder<PiPacketOperationType, PiPacketOperationModel>()
                    .put(PI_PACKET_OPERATION_TYPE_1, P4_PACKET_OPERATION_MODEL_1)
                    .build();
    private static final ImmutableMap<PiPacketOperationType, PiPacketOperationModel> PACKET_OPERATIONS_2 =
            new ImmutableMap.Builder<PiPacketOperationType, PiPacketOperationModel>()
                    .put(PI_PACKET_OPERATION_TYPE_2, P4_PACKET_OPERATION_MODEL_2)
                    .build();
    private static final ImmutableMap<PiPacketOperationType, PiPacketOperationModel> PACKET_OPERATIONS_3 =
            new ImmutableMap.Builder<PiPacketOperationType, PiPacketOperationModel>()
                    .put(PI_PACKET_OPERATION_TYPE_1, P4_PACKET_OPERATION_MODEL_3)
                    .build();

    private static final int FINGER_PRINT_1 = 0;
    private static final int FINGER_PRINT_2 = 1;

    private static final String ARCHITECTURE_ID_1 = "tna";
    private static final String ARCHITECTURE_ID_2 = "v1model";


    private static final PiPipelineModel P4_PIPELINE_MODEL_1 =
            new P4PipelineModel(TABLES_1, COUNTERS_1, METERS_1, REGISTERS_1, ACTION_PROFILES_1, PACKET_OPERATIONS_1,
                                ARCHITECTURE_ID_1, FINGER_PRINT_1);
    private static final PiPipelineModel SAME_AS_P4_PIPELINE_MODEL_1 =
            new P4PipelineModel(TABLES_1, COUNTERS_1, METERS_1, REGISTERS_1, ACTION_PROFILES_1, PACKET_OPERATIONS_1,
                                ARCHITECTURE_ID_1, FINGER_PRINT_1);
    private static final PiPipelineModel P4_PIPELINE_MODEL_2 =
            new P4PipelineModel(TABLES_2, COUNTERS_2, METERS_2, REGISTERS_1, ACTION_PROFILES_2, PACKET_OPERATIONS_2,
                                ARCHITECTURE_ID_2, FINGER_PRINT_2);
    private static final PiPipelineModel P4_PIPELINE_MODEL_3 =
            new P4PipelineModel(TABLES_2, COUNTERS_2, METERS_2, REGISTERS_1, ACTION_PROFILES_2, PACKET_OPERATIONS_3,
                                ARCHITECTURE_ID_2, FINGER_PRINT_2);
    private static final PiPipelineModel P4_PIPELINE_MODEL_4 =
            new P4PipelineModel(TABLES_3, COUNTERS_2, METERS_2, REGISTERS_1, ACTION_PROFILES_2, PACKET_OPERATIONS_3,
                                ARCHITECTURE_ID_2, FINGER_PRINT_2);

    /**
     * Checks that the P4PipelineModel class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(P4PipelineModel.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(P4_PIPELINE_MODEL_1, SAME_AS_P4_PIPELINE_MODEL_1)
                .addEqualityGroup(P4_PIPELINE_MODEL_2)
                .addEqualityGroup(P4_PIPELINE_MODEL_3)
                .addEqualityGroup(P4_PIPELINE_MODEL_4)
                .testEquals();
    }
}
