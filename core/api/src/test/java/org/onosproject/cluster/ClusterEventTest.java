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
    private final ClusterEvent event8 =
            new ClusterEvent(ClusterEvent.Type.INSTANCE_ADDED, cNode2, ClusterEvent.InstanceType.ONOS);
    private final ClusterEvent event9 =
            new ClusterEvent(ClusterEvent.Type.INSTANCE_ADDED, cNode2, ClusterEvent.InstanceType.STORAGE);
    private final ClusterEvent event10 =
            new ClusterEvent(ClusterEvent.Type.INSTANCE_REMOVED, cNode2, ClusterEvent.InstanceType.ONOS);
    private final ClusterEvent event11 =
            new ClusterEvent(ClusterEvent.Type.INSTANCE_REMOVED, cNode2, ClusterEvent.InstanceType.STORAGE);
    private final ClusterEvent event12 =
            new ClusterEvent(ClusterEvent.Type.INSTANCE_ACTIVATED, cNode1, ClusterEvent.InstanceType.ONOS);
    private final ClusterEvent event13 =
            new ClusterEvent(ClusterEvent.Type.INSTANCE_ACTIVATED, cNode1, ClusterEvent.InstanceType.STORAGE);
    private final ClusterEvent event14 =
            new ClusterEvent(ClusterEvent.Type.INSTANCE_READY, cNode1, ClusterEvent.InstanceType.ONOS);
    private final ClusterEvent event15 =
            new ClusterEvent(ClusterEvent.Type.INSTANCE_READY, cNode1, ClusterEvent.InstanceType.STORAGE);
    private final ClusterEvent event16 =
            new ClusterEvent(ClusterEvent.Type.INSTANCE_DEACTIVATED, cNode1, ClusterEvent.InstanceType.ONOS);
    private final ClusterEvent event17 =
            new ClusterEvent(ClusterEvent.Type.INSTANCE_DEACTIVATED, cNode1, ClusterEvent.InstanceType.STORAGE);

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
                .addEqualityGroup(event8)
                .addEqualityGroup(event9)
                .addEqualityGroup(event10)
                .addEqualityGroup(event11)
                .addEqualityGroup(event12)
                .addEqualityGroup(event13)
                .addEqualityGroup(event14)
                .addEqualityGroup(event15)
                .addEqualityGroup(event16)
                .addEqualityGroup(event17)
                .testEquals();
    }

    /**
     * Tests that objects are created properly.
     */
    @Test
    public void checkConstruction() {
        assertThat(event1.type(), is(ClusterEvent.Type.INSTANCE_ADDED));
        assertThat(event1.subject(), is(cNode1));
        assertThat(event1.instanceType(), is(ClusterEvent.InstanceType.UNKNOWN));

        assertThat(event7.time(), is(time));
        assertThat(event7.type(), is(ClusterEvent.Type.INSTANCE_READY));
        assertThat(event7.subject(), is(cNode2));
        assertThat(event7.instanceType(), is(ClusterEvent.InstanceType.UNKNOWN));

        assertThat(event8.type(), is(ClusterEvent.Type.INSTANCE_ADDED));
        assertThat(event8.subject(), is(cNode2));
        assertThat(event8.instanceType(), is(ClusterEvent.InstanceType.ONOS));

        assertThat(event9.type(), is(ClusterEvent.Type.INSTANCE_ADDED));
        assertThat(event9.subject(), is(cNode2));
        assertThat(event9.instanceType(), is(ClusterEvent.InstanceType.STORAGE));

        assertThat(event10.type(), is(ClusterEvent.Type.INSTANCE_REMOVED));
        assertThat(event10.subject(), is(cNode2));
        assertThat(event10.instanceType(), is(ClusterEvent.InstanceType.ONOS));

        assertThat(event11.type(), is(ClusterEvent.Type.INSTANCE_REMOVED));
        assertThat(event11.subject(), is(cNode2));
        assertThat(event11.instanceType(), is(ClusterEvent.InstanceType.STORAGE));

        assertThat(event12.type(), is(ClusterEvent.Type.INSTANCE_ACTIVATED));
        assertThat(event12.subject(), is(cNode1));
        assertThat(event12.instanceType(), is(ClusterEvent.InstanceType.ONOS));

        assertThat(event13.type(), is(ClusterEvent.Type.INSTANCE_ACTIVATED));
        assertThat(event13.subject(), is(cNode1));
        assertThat(event13.instanceType(), is(ClusterEvent.InstanceType.STORAGE));

        assertThat(event14.type(), is(ClusterEvent.Type.INSTANCE_READY));
        assertThat(event14.subject(), is(cNode1));
        assertThat(event14.instanceType(), is(ClusterEvent.InstanceType.ONOS));

        assertThat(event15.type(), is(ClusterEvent.Type.INSTANCE_READY));
        assertThat(event15.subject(), is(cNode1));
        assertThat(event15.instanceType(), is(ClusterEvent.InstanceType.STORAGE));

        assertThat(event16.type(), is(ClusterEvent.Type.INSTANCE_DEACTIVATED));
        assertThat(event16.subject(), is(cNode1));
        assertThat(event16.instanceType(), is(ClusterEvent.InstanceType.ONOS));

        assertThat(event17.type(), is(ClusterEvent.Type.INSTANCE_DEACTIVATED));
        assertThat(event17.subject(), is(cNode1));
        assertThat(event17.instanceType(), is(ClusterEvent.InstanceType.STORAGE));
    }

}
