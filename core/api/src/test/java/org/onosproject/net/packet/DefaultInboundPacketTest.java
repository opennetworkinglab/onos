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
import java.util.Optional;

import org.junit.Test;
import org.onlab.packet.Ethernet;
import org.onlab.packet.MacAddress;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.NetTestTools.connectPoint;

/**
 * Unit tests for the DefaultInboundPacket class.
 */
public class DefaultInboundPacketTest {

    final Ethernet eth = new Ethernet()
            .setDestinationMACAddress(MacAddress.BROADCAST)
            .setSourceMACAddress(MacAddress.BROADCAST);
    final ByteBuffer byteBuffer = ByteBuffer.wrap(eth.serialize());
    final DefaultInboundPacket packet1 =
            new DefaultInboundPacket(connectPoint("d1", 1),
                    eth,
                    byteBuffer,
                    Optional.of(1L));
    final DefaultInboundPacket sameAsPacket1 =
            new DefaultInboundPacket(connectPoint("d1", 1),
                    eth,
                    byteBuffer,
                    Optional.of(1L));
    final DefaultInboundPacket packet2 =
            new DefaultInboundPacket(connectPoint("d2", 1),
                    eth,
                    byteBuffer);
    final DefaultInboundPacket sameAsPacket2 =
            new DefaultInboundPacket(connectPoint("d2", 1),
                    eth,
                    byteBuffer,
                    Optional.empty());
    /**
     * Checks that the DefaultInboundPacket class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultInboundPacket.class);
    }

    /**
     * Tests the equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(packet1, sameAsPacket1)
                .addEqualityGroup(packet2, sameAsPacket2)
                .testEquals();
    }

    /**
     * Tests the object creation through the constructor.
     */
    @Test
    public void testConstruction() {
        assertThat(packet1.receivedFrom(), equalTo(connectPoint("d1", 1)));
        assertThat(packet1.parsed(), equalTo(eth));
        assertThat(packet1.unparsed(), notNullValue());
        assertThat(packet1.cookie(), equalTo(Optional.of(1L)));
    }
}
