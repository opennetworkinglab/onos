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
package org.onosproject.p4runtime.ctl;

import com.google.common.testing.EqualsTester;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiPacketMetadataId;
import org.onosproject.net.pi.runtime.PiPacketMetadata;
import org.onosproject.net.pi.runtime.PiPacketOperation;
import org.onosproject.p4runtime.ctl.controller.PacketInEvent;

import static org.onlab.util.ImmutableByteSequence.copyFrom;
import static org.onosproject.net.pi.model.PiPacketOperationType.PACKET_IN;
import static org.onosproject.net.pi.model.PiPacketOperationType.PACKET_OUT;

/**
 * Test for DefaultPacketIn class.
 */
public class PacketInEventTest {

    private static final int DEFAULT_ORIGINAL_VALUE = 255;
    private static final int DEFAULT_BIT_WIDTH = 9;

    private final DeviceId deviceId = DeviceId.deviceId("dummy:1");
    private final DeviceId sameDeviceId = DeviceId.deviceId("dummy:1");
    private final DeviceId deviceId2 = DeviceId.deviceId("dummy:2");
    private final DeviceId nullDeviceId = null;

    private PiPacketOperation packetOperation;
    private PiPacketOperation packetOperation2;
    private PiPacketOperation nullPacketOperation = null;

    private org.onosproject.p4runtime.ctl.controller.PacketInEvent packetIn;
    private org.onosproject.p4runtime.ctl.controller.PacketInEvent sameAsPacketIn;
    private org.onosproject.p4runtime.ctl.controller.PacketInEvent packetIn2;
    private PacketInEvent packetIn3;

    /**
     * Setup method for packetOperation and packetOperation2.
     * @throws ImmutableByteSequence.ByteSequenceTrimException if byte sequence cannot be trimmed
     */
    @Before
    public void setup() throws ImmutableByteSequence.ByteSequenceTrimException {

        packetOperation = PiPacketOperation.builder()
                .withData(ImmutableByteSequence.ofOnes(512))
                .withType(PACKET_OUT)
                .withMetadata(PiPacketMetadata.builder()
                                      .withId(PiPacketMetadataId.of("egress_port"))
                                      .withValue(copyFrom(DEFAULT_ORIGINAL_VALUE).fit(DEFAULT_BIT_WIDTH))
                                      .build())
                .build();

        packetOperation2 = PiPacketOperation.builder()
                .withData(ImmutableByteSequence.ofOnes(512))
                .withType(PACKET_IN)
                .withMetadata(PiPacketMetadata.builder()
                                      .withId(PiPacketMetadataId.of("ingress_port"))
                                      .withValue(copyFrom(DEFAULT_ORIGINAL_VALUE).fit(DEFAULT_BIT_WIDTH))
                                      .build())
                .build();

        packetIn = new org.onosproject.p4runtime.ctl.controller.PacketInEvent(deviceId, packetOperation);
        sameAsPacketIn = new org.onosproject.p4runtime.ctl.controller.PacketInEvent(sameDeviceId, packetOperation);
        packetIn2 = new org.onosproject.p4runtime.ctl.controller.PacketInEvent(deviceId2, packetOperation);
        packetIn3 = new org.onosproject.p4runtime.ctl.controller.PacketInEvent(deviceId, packetOperation2);
    }

    /**
     * tearDown method for packetOperation and packetOperation2.
     */
    @After
    public void tearDown() {
        packetOperation = null;
        packetOperation2 = null;

        packetIn = null;
        sameAsPacketIn = null;
        packetIn2 = null;
        packetIn3 = null;
    }


    /**
     * Tests constructor with null object as a DeviceId parameter.
     */
    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullDeviceId() {

        new org.onosproject.p4runtime.ctl.controller.PacketInEvent(nullDeviceId, packetOperation);
    }

    /**
     * Tests constructor with null object as PacketOperation parameter.
     */
    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullPacketOperation() {

        new org.onosproject.p4runtime.ctl.controller.PacketInEvent(deviceId, nullPacketOperation);
    }

    /**
     * Test for deviceId method.
     */
    @Test
    public void deviceId() {
        new EqualsTester()
                .addEqualityGroup(deviceId, packetIn.deviceId(), sameAsPacketIn.deviceId())
                .addEqualityGroup(packetIn2)
                .testEquals();
    }

    /**
     * Test for packetOperation method.
     */
    @Test
    public void packetOperation() {
        new EqualsTester()
                .addEqualityGroup(packetOperation, packetIn.packetOperation())
                .addEqualityGroup(packetIn3.packetOperation())
                .testEquals();
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(packetIn, sameAsPacketIn)
                .addEqualityGroup(packetIn2)
                .addEqualityGroup(packetIn3)
                .testEquals();
    }
}
