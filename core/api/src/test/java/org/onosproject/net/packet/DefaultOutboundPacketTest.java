/*
 * Copyright 2014-present Open Networking Laboratory
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

import java.nio.ByteBuffer;

import org.junit.Test;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.intent.IntentTestsMocks;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.NetTestTools.did;

/**
 * Unit tests for the DefaultOutboundPacketTest class.
 */
public class DefaultOutboundPacketTest {
    final Ethernet eth = new Ethernet()
            .setDestinationMACAddress(MacAddress.BROADCAST)
            .setSourceMACAddress(MacAddress.BROADCAST);
    final ByteBuffer byteBuffer = ByteBuffer.wrap(eth.serialize());
    final TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();
    final DefaultOutboundPacket packet1 =
            new DefaultOutboundPacket(did("d1"),
                    treatment,
                    byteBuffer);
    final DefaultOutboundPacket sameAsPacket1 =
            new DefaultOutboundPacket(did("d1"),
                    treatment,
                    byteBuffer);
    final DefaultOutboundPacket packet2 =
            new DefaultOutboundPacket(did("d2"),
                    treatment,
                    byteBuffer);
    /**
     * Checks that the DefaultOutboundPacket class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultOutboundPacket.class);
    }

    /**
     * Tests the equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(packet1, sameAsPacket1)
                .addEqualityGroup(packet2)
                .testEquals();
    }

    /**
     * Tests the object creation through the constructor.
     */
    @Test
    public void testConstruction() {
        assertThat(packet1.sendThrough(), equalTo(did("d1")));
        assertThat(packet1.data(), equalTo(byteBuffer));
        assertThat(packet1.treatment(), equalTo(treatment));
    }
}
