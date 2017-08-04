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
package org.onosproject.store.primitives;

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.cluster.DefaultPartition;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.Partition;
import org.onosproject.cluster.PartitionId;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for the mastership event.
 */
public class PartitionEventTest {
    private final long time = System.currentTimeMillis();
    private final long time0 = 0;

    private final PartitionId pid1 = PartitionId.from(1);
    private final PartitionId pid2 = PartitionId.from(2);

    private final NodeId nid1 = NodeId.nodeId("10.0.0.1");
    private final NodeId nid2 = NodeId.nodeId("10.0.0.2");


    private final Partition p1 =
            new DefaultPartition(pid1, ImmutableSet.of(nid1));
    private final Partition p2 =
            new DefaultPartition(pid2, ImmutableSet.of(nid1, nid2));

    private final PartitionEvent event1 =
            new PartitionEvent(PartitionEvent.Type.UPDATED, p1, time);
    private final PartitionEvent sameAsEvent1 =
            new PartitionEvent(PartitionEvent.Type.UPDATED, p1, time);
    private final PartitionEvent event2 =
            new PartitionEvent(PartitionEvent.Type.OPENED, p1, time);
    private final PartitionEvent event3 =
            new PartitionEvent(PartitionEvent.Type.CLOSED, p1, time);
    private final PartitionEvent event4 =
            new PartitionEvent(PartitionEvent.Type.AVAILABLE, p1, time);
    private final PartitionEvent event5 =
            new PartitionEvent(PartitionEvent.Type.UNAVAILABLE, p1, time);
    private final PartitionEvent event6 =
            new PartitionEvent(PartitionEvent.Type.UPDATED, p2, time);
    private final PartitionEvent event7 =
            new PartitionEvent(PartitionEvent.Type.UPDATED, p1, time0);

    /**
     * Tests for proper operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void checkEquals() {
        new EqualsTester()
                .addEqualityGroup(event1, sameAsEvent1)
                .addEqualityGroup(event2)
                .addEqualityGroup(event3)
                .addEqualityGroup(event4)
                .addEqualityGroup(event5)
                .addEqualityGroup(event6)
                .addEqualityGroup(event7)
                .testEquals();
    }

    /**
     * Tests that objects are created properly.
     */
    @Test
    public void checkConstruction() {
        assertThat(event1.type(), is(PartitionEvent.Type.UPDATED));
        assertThat(event1.subject(), is(p1));
        assertThat(event1.time(), is(time));
    }

}
