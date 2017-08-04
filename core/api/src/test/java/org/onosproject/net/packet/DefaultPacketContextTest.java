/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.packet;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.IntentTestsMocks;

import java.nio.ByteBuffer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutableBaseClass;
import static org.onosproject.net.NetTestTools.connectPoint;
import static org.onosproject.net.NetTestTools.did;

/**
 * Unit tests for the DefaultPacketContextTest.
 */
public class DefaultPacketContextTest {
    final Ethernet eth = new Ethernet()
            .setDestinationMACAddress(MacAddress.BROADCAST)
            .setSourceMACAddress(MacAddress.BROADCAST);
    final ByteBuffer byteBuffer = ByteBuffer.wrap(eth.serialize());
    final DefaultInboundPacket inPacket =
            new DefaultInboundPacket(connectPoint("d1", 1),
                    eth,
                    byteBuffer);
    final TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();
    final DefaultOutboundPacket outPacket =
            new DefaultOutboundPacket(did("d1"),
                    treatment,
                    byteBuffer);

    final DefaultPacketContext context1 =
            new PacketContextAdapter(123L, inPacket, outPacket, true);
    final DefaultPacketContext sameAsContext1 =
            new PacketContextAdapter(123L, inPacket, outPacket, true);
    final DefaultPacketContext context2 =
            new PacketContextAdapter(123123L, inPacket, outPacket, true);

    /**
     * Checks that the DefaultOutboundPacket class is immutable but can be
     * used as a base class.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutableBaseClass(DefaultPacketContext.class);
    }

    /**
     * Tests the equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        // No hashCode() or equals() defined, object comparison is used.
        new EqualsTester()
                .addEqualityGroup(context1)
                .addEqualityGroup(sameAsContext1)
                .addEqualityGroup(context2)
                .testEquals();
    }

    /**
     * Tests that objects are created properly.
     */
    @Test
    public void testConstruction() {
        assertThat(context1.block(), is(true));
        assertThat(context1.inPacket(), is(inPacket));
        assertThat(context1.isHandled(), is(true));
        assertThat(context1.outPacket(), is(outPacket));
        assertThat(context1.time(), is(123L));
        assertThat(context1.treatmentBuilder(), is(notNullValue()));
    }
}
