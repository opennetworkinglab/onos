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

import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onlab.packet.IpAddress;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit tests for the Cluster Metadata event.
 */
public class ClusterMetadataEventTest {
    private final long time1 = System.currentTimeMillis() - 100;
    private final long time = System.currentTimeMillis();
    private final PartitionId pid1 = PartitionId.from(1);
    private final PartitionId pid2 = PartitionId.from(2);
    private final NodeId nid1 = NodeId.nodeId("10.0.0.1");
    private final NodeId nid2 = NodeId.nodeId("10.0.0.2");
    private final ControllerNode n1 =
            new DefaultControllerNode(nid1, IpAddress.valueOf("10.0.0.1"), 9876);
    private final ControllerNode n2 =
            new DefaultControllerNode(nid2, IpAddress.valueOf("10.0.0.2"), 9876);
    private final Partition p1 = new DefaultPartition(pid1, ImmutableSet.of(nid1));
    private final Partition p2 = new DefaultPartition(pid2, ImmutableSet.of(nid1, nid2));
    private final Partition p3 = new DefaultPartition(pid2, ImmutableSet.of(nid2));
    private final ClusterMetadata metadata1 =
            new ClusterMetadata("foo", ImmutableSet.of(n1), ImmutableSet.of(p1));
    private final ClusterMetadata metadata2 =
            new ClusterMetadata("bar", ImmutableSet.of(n1, n2), ImmutableSet.of(p1, p2));
    private final ClusterMetadata metadata3 =
            new ClusterMetadata("baz", ImmutableSet.of(n2), ImmutableSet.of(p3));

    private final ClusterMetadataEvent event1 =
            new ClusterMetadataEvent(ClusterMetadataEvent.Type.METADATA_CHANGED, metadata1, time1);
    private final ClusterMetadataEvent sameAsEvent1 =
            new ClusterMetadataEvent(ClusterMetadataEvent.Type.METADATA_CHANGED, metadata1, time1);
    private final ClusterMetadataEvent event2 =
            new ClusterMetadataEvent(ClusterMetadataEvent.Type.METADATA_CHANGED, metadata2, time);
    private final ClusterMetadataEvent sameAsEvent2 =
            new ClusterMetadataEvent(ClusterMetadataEvent.Type.METADATA_CHANGED, metadata2, time);

    /**
     * Tests for proper operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void checkEquals() throws Exception {
        // ensure event3 will have different timestamp from `time`
        Thread.sleep(1);
        ClusterMetadataEvent event3 =
                new ClusterMetadataEvent(ClusterMetadataEvent.Type.METADATA_CHANGED, metadata3);

        new EqualsTester()
                .addEqualityGroup(event1, sameAsEvent1)
                .addEqualityGroup(event2, sameAsEvent2)
                .addEqualityGroup(event3)
                .testEquals();
    }

    /**
     * Tests that objects are created properly.
     */
    @Test
    public void checkConstruction() {
        assertThat(event1.type(), is(ClusterMetadataEvent.Type.METADATA_CHANGED));
        assertThat(event1.subject(), is(metadata1));

        assertThat(event2.time(), is(time));
        assertThat(event2.type(), is(ClusterMetadataEvent.Type.METADATA_CHANGED));
        assertThat(event2.subject(), is(metadata2));
    }

}
