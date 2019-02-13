/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.net.device;

import java.lang.reflect.Constructor;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;
import org.onosproject.net.NetTestTools;

import com.google.common.testing.EqualsTester;
import org.onosproject.net.PortNumber;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * DefaultPortStatistics unit tests.
 */
public class DefaultPortStatisticsTest {

    private final PortStatistics stats1 = DefaultPortStatistics.builder()
            .setBytesReceived(1)
            .setBytesSent(2)
            .setDurationNano(3)
            .setDurationSec(4)
            .setPacketsReceived(5)
            .setPacketsSent(6)
            .setPacketsRxDropped(7)
            .setPacketsRxErrors(8)
            .setPacketsTxDropped(9)
            .setPacketsTxErrors(10)
            .setPort(PortNumber.portNumber(80))
            .setDeviceId(NetTestTools.did("1"))
            .build();

    private final PortStatistics stats2 = DefaultPortStatistics.builder()
            .setBytesReceived(1)
            .setBytesSent(2)
            .setDurationNano(3)
            .setDurationSec(4)
            .setPacketsReceived(5)
            .setPacketsSent(6)
            .setPacketsRxDropped(7)
            .setPacketsRxErrors(8)
            .setPacketsTxDropped(9)
            .setPacketsTxErrors(11)
            .setPort(PortNumber.portNumber(80))
            .setDeviceId(NetTestTools.did("1"))
            .build();

    /**
     * Checks that the GroupOperation class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultPortStatistics.class);
    }

    @Test
    public void testConstruction() {
        assertThat(stats1.bytesReceived(), is(1L));
        assertThat(stats1.bytesSent(), is(2L));
        assertThat(stats1.durationNano(), is(3L));
        assertThat(stats1.durationSec(), is(4L));
        assertThat(stats1.packetsReceived(), is(5L));
        assertThat(stats1.packetsSent(), is(6L));
        assertThat(stats1.packetsRxDropped(), is(7L));
        assertThat(stats1.packetsRxErrors(), is(8L));
        assertThat(stats1.packetsTxDropped(), is(9L));
        assertThat(stats1.packetsTxErrors(), is(10L));
        assertThat(stats1.portNumber().toLong(), is(80L));
    }

    /**
     * Tests the equals, hashCode and toString methods using Guava EqualsTester.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(stats1, stats1)
                .addEqualityGroup(stats2)
                .testEquals();
    }

    /**
     * Tests that the empty argument list constructor for serialization
     * is present and creates a proper object.
     */
    @Test
    public void testSerializerConstructor() {
        try {
            Constructor[] constructors = DefaultPortStatistics.class.getDeclaredConstructors();
            assertThat(constructors, notNullValue());
            Arrays.stream(constructors).filter(ctor ->
                ctor.getParameterTypes().length == 0)
                    .forEach(noParamsCtor -> {
                        try {
                            noParamsCtor.setAccessible(true);
                            DefaultPortStatistics stats =
                                    (DefaultPortStatistics) noParamsCtor.newInstance();
                            assertThat(stats, notNullValue());
                        } catch (Exception e) {
                            Assert.fail("Exception instantiating no parameters constructor");
                        }
                    });
        } catch (Exception e) {
            Assert.fail("Exception looking up constructors");
        }
    }
}
