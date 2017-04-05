/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.cluster;

import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.packet.IpAddress;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for the cluster event test.
 */
public class ClusterEventTest {
    private final NodeId node1 = new NodeId("1");
    private final NodeId node2 = new NodeId("2");
    private final IpAddress ip1 = IpAddress.valueOf("10.0.0.1");
    private final IpAddress ip2 = IpAddress.valueOf("10.0.0.2");
    private final ControllerNode cNode1 = new DefaultControllerNode(node1, ip1);
    private final ControllerNode cNode2 = new DefaultControllerNode(node2, ip2);
    private final ClusterEvent event1 =
            new ClusterEvent(ClusterEvent.Type.INSTANCE_ADDED, cNode1);
    private final ClusterEvent event2 =
            new ClusterEvent(ClusterEvent.Type.INSTANCE_REMOVED, cNode1);
    private final ClusterEvent event3 =
            new ClusterEvent(ClusterEvent.Type.INSTANCE_ACTIVATED, cNode1);
    private final ClusterEvent event4 =
            new ClusterEvent(ClusterEvent.Type.INSTANCE_READY, cNode1);
    private final ClusterEvent event5 =
            new ClusterEvent(ClusterEvent.Type.INSTANCE_DEACTIVATED, cNode1);
    private final ClusterEvent event6 =
            new ClusterEvent(ClusterEvent.Type.INSTANCE_ADDED, cNode2);
    private final long time = System.currentTimeMillis();
    private final ClusterEvent event7 =
            new ClusterEvent(ClusterEvent.Type.INSTANCE_READY, cNode2, time);
    private final ClusterEvent sameAsEvent7 =
            new ClusterEvent(ClusterEvent.Type.INSTANCE_READY, cNode2, time);

    /**
     * Tests for proper operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void checkEquals() {
        new EqualsTester()
                .addEqualityGroup(event1)
                .addEqualityGroup(event2)
                .addEqualityGroup(event3)
                .addEqualityGroup(event4)
                .addEqualityGroup(event5)
                .addEqualityGroup(event6)
                .addEqualityGroup(event7, sameAsEvent7)
                .testEquals();
    }

    /**
     * Tests that objects are created properly.
     */
    @Test
    public void checkConstruction() {
        assertThat(event1.type(), is(ClusterEvent.Type.INSTANCE_ADDED));
        assertThat(event1.subject(), is(cNode1));

        assertThat(event7.time(), is(time));
        assertThat(event7.type(), is(ClusterEvent.Type.INSTANCE_READY));
        assertThat(event7.subject(), is(cNode2));
    }

}
