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
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.pi.model.PiPacketMetadataId;
import org.onosproject.net.pi.model.PiPacketMetadataModel;
import org.onosproject.net.pi.model.PiPacketOperationType;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for P4PacketOperationModel class.
 */
public class P4PacketOperationModelTest {

    private static final PiPacketOperationType PI_PACKET_OPERATION_TYPE_1 = PiPacketOperationType.PACKET_IN;
    private static final PiPacketOperationType PI_PACKET_OPERATION_TYPE_2 = PiPacketOperationType.PACKET_OUT;

    private static final PiPacketMetadataId PI_CONTROL_METADATA_ID_1 = PiPacketMetadataId.of("Metadata1");
    private static final PiPacketMetadataId PI_CONTROL_METADATA_ID_2 = PiPacketMetadataId.of("Metadata2");

    private static final int BIT_WIDTH_1 = 8;
    private static final int BIT_WIDTH_2 = 9;

    private static final PiPacketMetadataModel PI_CONTROL_METADATA_MODEL_1 =
        new P4PacketMetadataModel(PI_CONTROL_METADATA_ID_1, BIT_WIDTH_1);
    private static final PiPacketMetadataModel PI_CONTROL_METADATA_MODEL_2 =
        new P4PacketMetadataModel(PI_CONTROL_METADATA_ID_2, BIT_WIDTH_2);
    private static final PiPacketMetadataModel PI_CONTROL_METADATA_MODEL_3 =
            new P4PacketMetadataModel(
                    PI_CONTROL_METADATA_ID_2, P4PacketMetadataModel.BIT_WIDTH_UNDEFINED);

    private static final ImmutableList<PiPacketMetadataModel> METADATAS_1 =
        new ImmutableList.Builder<PiPacketMetadataModel>()
            .add(PI_CONTROL_METADATA_MODEL_1)
            .build();

    private static final ImmutableList<PiPacketMetadataModel> METADATAS_2 =
        new ImmutableList.Builder<PiPacketMetadataModel>()
            .add(PI_CONTROL_METADATA_MODEL_2)
            .build();

    private static final ImmutableList<PiPacketMetadataModel> METADATAS_3 =
        new ImmutableList.Builder<PiPacketMetadataModel>()
            .add(PI_CONTROL_METADATA_MODEL_1)
            .add(PI_CONTROL_METADATA_MODEL_2)
            .build();
    private static final ImmutableList<PiPacketMetadataModel> METADATAS_4 =
            new ImmutableList.Builder<PiPacketMetadataModel>()
                    .add(PI_CONTROL_METADATA_MODEL_1)
                    .add(PI_CONTROL_METADATA_MODEL_3)
                    .build();

    private static final P4PacketOperationModel P4_PACKET_OPERATION_MODEL_1 =
        new P4PacketOperationModel(PI_PACKET_OPERATION_TYPE_1, METADATAS_1);
    private static final P4PacketOperationModel SAME_AS_P4_PACKET_OPERATION_MODEL_1 =
        new P4PacketOperationModel(PI_PACKET_OPERATION_TYPE_1, METADATAS_1);
    private static final P4PacketOperationModel P4_PACKET_OPERATION_MODEL_2 =
        new P4PacketOperationModel(PI_PACKET_OPERATION_TYPE_2, METADATAS_2);
    private static final P4PacketOperationModel P4_PACKET_OPERATION_MODEL_3 =
        new P4PacketOperationModel(PI_PACKET_OPERATION_TYPE_2, METADATAS_3);
    private static final P4PacketOperationModel P4_PACKET_OPERATION_MODEL_4 =
            new P4PacketOperationModel(PI_PACKET_OPERATION_TYPE_2, METADATAS_4);

    /**
     * Checks that the P4PacketOperationModel class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(P4PacketOperationModel.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
            .addEqualityGroup(P4_PACKET_OPERATION_MODEL_1, SAME_AS_P4_PACKET_OPERATION_MODEL_1)
            .addEqualityGroup(P4_PACKET_OPERATION_MODEL_2)
            .addEqualityGroup(P4_PACKET_OPERATION_MODEL_3)
            .addEqualityGroup(P4_PACKET_OPERATION_MODEL_4)
            .testEquals();
    }
}
