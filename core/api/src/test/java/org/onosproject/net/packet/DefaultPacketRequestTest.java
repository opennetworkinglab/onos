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
package org.onosproject.net.packet;

import org.junit.Test;
import org.onosproject.cluster.NodeId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;

import com.google.common.testing.EqualsTester;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for the DefaultPacketRequest class.
 */
public class DefaultPacketRequestTest {

    private final TrafficSelector selector = DefaultTrafficSelector
            .builder()
            .matchIcmpCode((byte) 1)
            .build();

    private final DefaultPacketRequest packetRequest1 =
            new DefaultPacketRequest(DefaultTrafficSelector.emptySelector(),
                                     PacketPriority.CONTROL,
                                     NetTestTools.APP_ID,
                                     NetTestTools.NODE_ID, Optional.empty());
    private final DefaultPacketRequest sameAsacketRequest1 =
            new DefaultPacketRequest(DefaultTrafficSelector.emptySelector(),
                                     PacketPriority.CONTROL,
                                     NetTestTools.APP_ID,
                                     NetTestTools.NODE_ID, Optional.empty());
    private final DefaultPacketRequest packetRequest2 =
            new DefaultPacketRequest(selector,
                                     PacketPriority.CONTROL,
                                     NetTestTools.APP_ID,
                                     NetTestTools.NODE_ID, Optional.empty());
    private final DefaultPacketRequest packetRequest3 =
            new DefaultPacketRequest(DefaultTrafficSelector.emptySelector(),
                                     PacketPriority.REACTIVE,
                                     NetTestTools.APP_ID,
                                     NetTestTools.NODE_ID, Optional.empty());
    private final DefaultPacketRequest packetRequest4 =
            new DefaultPacketRequest(DefaultTrafficSelector.emptySelector(),
                                     PacketPriority.CONTROL,
                                     new DefaultApplicationId(1, "foo"),
                                     new NodeId("node1"), Optional.empty());

    /**
     * Tests the operation of the equals(), toAstring() and hashCode() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(packetRequest1, sameAsacketRequest1)
                .addEqualityGroup(packetRequest2)
                .addEqualityGroup(packetRequest3)
                .addEqualityGroup(packetRequest4)
                .testEquals();
    }

    /**
     * Tests that building and fetching from a DefaultPacketRequest is correct.
     */
    @Test
    public void testConstruction() {
        assertThat(packetRequest1.priority(), is(PacketPriority.CONTROL));
        assertThat(packetRequest1.priority().priorityValue(),
                   is(PacketPriority.CONTROL.priorityValue()));
        assertThat(packetRequest1.selector(), is(DefaultTrafficSelector.emptySelector()));
        assertThat(packetRequest1.appId(), is(NetTestTools.APP_ID));
    }

    /**
     * Checks that the DefaultPacketRequest class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultPacketRequest.class);
    }
}
