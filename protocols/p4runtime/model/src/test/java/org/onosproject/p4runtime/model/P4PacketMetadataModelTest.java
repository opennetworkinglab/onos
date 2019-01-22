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

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.net.pi.model.PiPacketMetadataId;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Test for P4PacketMetadataModel class.
 */
public class P4PacketMetadataModelTest {

    private final PiPacketMetadataId piPacketMetadataId = PiPacketMetadataId.of("EGRESS_PORT");
    private final PiPacketMetadataId sameAsPiPacketMetadataId = PiPacketMetadataId.of("EGRESS_PORT");
    private final PiPacketMetadataId piPacketMetadataId2 = PiPacketMetadataId.of("INGRESS_PORT");

    private static final int BIT_WIDTH_32 = 32;
    private static final int BIT_WIDTH_64 = 64;

    private final P4PacketMetadataModel metadataModel = new P4PacketMetadataModel(piPacketMetadataId, BIT_WIDTH_32);

    private final P4PacketMetadataModel sameAsMetadataModel = new P4PacketMetadataModel(sameAsPiPacketMetadataId,
                                                                                          BIT_WIDTH_32);

    private final P4PacketMetadataModel metadataModel2 = new P4PacketMetadataModel(piPacketMetadataId2,
                                                                                     BIT_WIDTH_32);

    private final P4PacketMetadataModel metadataModel3 = new P4PacketMetadataModel(piPacketMetadataId, BIT_WIDTH_64);



    /**
     * Checks that the P4CounterModel class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(P4PacketMetadataModel.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(metadataModel, sameAsMetadataModel)
                .addEqualityGroup(metadataModel2)
                .addEqualityGroup(metadataModel3)
                .testEquals();
    }
}
