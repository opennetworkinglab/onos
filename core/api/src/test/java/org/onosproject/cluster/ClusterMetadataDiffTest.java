/*
 * Copyright 2016-present Open Networking Laboratory
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.onlab.packet.IpAddress;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Unit tests for ClusterMetadataDiff.
 */
public class ClusterMetadataDiffTest {

    @Test
    public void testDiffNoChange() {
        PartitionId pid1 = PartitionId.from(1);
        NodeId nid1 = NodeId.nodeId("10.0.0.1");
        ControllerNode n1 = new DefaultControllerNode(nid1, IpAddress.valueOf("10.0.0.1"), 9876);
        Partition p1 = new DefaultPartition(pid1, ImmutableSet.of(nid1));
        ClusterMetadata md1 = new ClusterMetadata("foo", ImmutableSet.of(n1), ImmutableSet.of(p1));
        ClusterMetadataDiff diff = new ClusterMetadataDiff(md1, md1);
        assertTrue(diff.nodesAdded().isEmpty());
        assertTrue(diff.nodesRemoved().isEmpty());
        assertEquals(diff.partitionDiffs().size(), 1);
        assertEquals(diff.partitionDiffs().keySet(), Sets.newHashSet(pid1));
        PartitionDiff pdiff = diff.partitionDiffs().get(pid1);
        assertFalse(pdiff.hasChanged());
    }

    @Test
    public void testDiffForScaleUp() {
        PartitionId pid1 = PartitionId.from(1);
        NodeId nid1 = NodeId.nodeId("10.0.0.1");
        NodeId nid2 = NodeId.nodeId("10.0.0.2");
        ControllerNode n1 = new DefaultControllerNode(nid1, IpAddress.valueOf("10.0.0.1"), 9876);
        ControllerNode n2 = new DefaultControllerNode(nid2, IpAddress.valueOf("10.0.0.2"), 9876);
        Partition p1 = new DefaultPartition(pid1, ImmutableSet.of(nid1));
        Partition p12 = new DefaultPartition(pid1, ImmutableSet.of(nid1, nid2));
        ClusterMetadata md1 = new ClusterMetadata("foo", ImmutableSet.of(n1), ImmutableSet.of(p1));
        ClusterMetadata md12 = new ClusterMetadata("foo", ImmutableSet.of(n1, n2), ImmutableSet.of(p12));
        ClusterMetadataDiff diff = new ClusterMetadataDiff(md1, md12);
        assertEquals(diff.nodesAdded(), Sets.newHashSet(n2));
        assertTrue(diff.nodesRemoved().isEmpty());
        assertEquals(diff.partitionDiffs().size(), 1);
        assertEquals(diff.partitionDiffs().keySet(), Sets.newHashSet(pid1));
        PartitionDiff pdiff = diff.partitionDiffs().get(pid1);
        assertTrue(pdiff.hasChanged());
        assertFalse(pdiff.isAdded(nid1));
        assertTrue(pdiff.isAdded(nid2));
        assertFalse(pdiff.isRemoved(nid1));
        assertFalse(pdiff.isAdded(nid1));
    }

    @Test
    public void testDiffForScaleDown() {
        PartitionId pid1 = PartitionId.from(1);
        NodeId nid1 = NodeId.nodeId("10.0.0.1");
        NodeId nid2 = NodeId.nodeId("10.0.0.2");
        ControllerNode n1 = new DefaultControllerNode(nid1, IpAddress.valueOf("10.0.0.1"), 9876);
        ControllerNode n2 = new DefaultControllerNode(nid2, IpAddress.valueOf("10.0.0.2"), 9876);
        Partition p1 = new DefaultPartition(pid1, ImmutableSet.of(nid1));
        Partition p12 = new DefaultPartition(pid1, ImmutableSet.of(nid1, nid2));
        ClusterMetadata md1 = new ClusterMetadata("foo", ImmutableSet.of(n1), ImmutableSet.of(p1));
        ClusterMetadata md12 = new ClusterMetadata("foo", ImmutableSet.of(n1, n2), ImmutableSet.of(p12));
        ClusterMetadataDiff diff = new ClusterMetadataDiff(md12, md1);
        assertEquals(diff.nodesRemoved(), Sets.newHashSet(nid2));
        assertTrue(diff.nodesAdded().isEmpty());
        assertEquals(diff.partitionDiffs().size(), 1);
        assertEquals(diff.partitionDiffs().keySet(), Sets.newHashSet(pid1));
        PartitionDiff pdiff = diff.partitionDiffs().get(pid1);
        assertTrue(pdiff.hasChanged());
        assertTrue(pdiff.isRemoved(nid2));
        assertFalse(pdiff.isAdded(nid2));
        assertFalse(pdiff.isRemoved(nid1));
        assertFalse(pdiff.isAdded(nid1));
    }
}
