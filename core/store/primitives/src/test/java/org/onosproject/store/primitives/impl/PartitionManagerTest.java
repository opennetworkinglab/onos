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
package org.onosproject.store.primitives.impl;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.onosproject.cluster.DefaultPartition;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.Partition;
import org.onosproject.cluster.PartitionId;
import org.onosproject.core.Version;

import static org.junit.Assert.assertTrue;

/**
 * Partition manager test.
 */
public class PartitionManagerTest {

    @Test
    public void testComputeInitialIncompletePartition() throws Exception {
        Partition sourcePartition = new DefaultPartition(
                PartitionId.from(1),
                Version.version("1.0.0"),
                Arrays.asList(
                        NodeId.nodeId("1"),
                        NodeId.nodeId("2"),
                        NodeId.nodeId("3")));
        Version targetVersion = Version.version("1.0.1");
        List<NodeId> members = Arrays.asList(
                NodeId.nodeId("1"),
                NodeId.nodeId("2"));
        Partition forkedPartition = PartitionManager.computeInitialPartition(sourcePartition, targetVersion, members);
        assertTrue(forkedPartition.getMembers().size() == 2);
        assertTrue(forkedPartition.getMembers().contains(NodeId.nodeId("1")));
        assertTrue(forkedPartition.getMembers().contains(NodeId.nodeId("2")));
    }

    @Test
    public void testComputeInitialCompletePartition() throws Exception {
        Partition sourcePartition = new DefaultPartition(
                PartitionId.from(1),
                Version.version("1.0.0"),
                Arrays.asList(
                        NodeId.nodeId("3"),
                        NodeId.nodeId("4"),
                        NodeId.nodeId("5")));
        Version targetVersion = Version.version("1.0.1");
        List<NodeId> members = Arrays.asList(
                NodeId.nodeId("1"),
                NodeId.nodeId("2"),
                NodeId.nodeId("3"),
                NodeId.nodeId("4"),
                NodeId.nodeId("5"));
        Partition forkedPartition = PartitionManager.computeInitialPartition(sourcePartition, targetVersion, members);
        assertTrue(forkedPartition.getMembers().size() == 4);
        assertTrue(forkedPartition.getMembers().contains(NodeId.nodeId("1")));
        assertTrue(forkedPartition.getMembers().contains(NodeId.nodeId("3")));
        assertTrue(forkedPartition.getMembers().contains(NodeId.nodeId("4")));
        assertTrue(forkedPartition.getMembers().contains(NodeId.nodeId("5")));
    }

    @Test
    public void testComputeFinalIncompletePartition() throws Exception {
        Partition sourcePartition = new DefaultPartition(
                PartitionId.from(1),
                Version.version("1.0.0"),
                Arrays.asList(
                        NodeId.nodeId("1"),
                        NodeId.nodeId("2"),
                        NodeId.nodeId("3")));
        Version targetVersion = Version.version("1.0.1");
        List<NodeId> members = Arrays.asList(
                NodeId.nodeId("1"),
                NodeId.nodeId("2"));
        Partition forkedPartition = PartitionManager.computeFinalPartition(sourcePartition, targetVersion, members);
        assertTrue(forkedPartition.getMembers().size() == 2);
        assertTrue(forkedPartition.getMembers().contains(NodeId.nodeId("1")));
        assertTrue(forkedPartition.getMembers().contains(NodeId.nodeId("2")));
    }

    @Test
    public void testComputeFinalCompletePartition() throws Exception {
        Partition sourcePartition = new DefaultPartition(
                PartitionId.from(1),
                Version.version("1.0.0"),
                Arrays.asList(
                        NodeId.nodeId("3"),
                        NodeId.nodeId("4"),
                        NodeId.nodeId("5")));
        Version targetVersion = Version.version("1.0.1");
        List<NodeId> members = Arrays.asList(
                NodeId.nodeId("1"),
                NodeId.nodeId("2"),
                NodeId.nodeId("3"),
                NodeId.nodeId("4"),
                NodeId.nodeId("5"));
        Partition forkedPartition = PartitionManager.computeFinalPartition(sourcePartition, targetVersion, members);
        assertTrue(forkedPartition.getMembers().size() == 3);
        assertTrue(forkedPartition.getMembers().contains(NodeId.nodeId("3")));
        assertTrue(forkedPartition.getMembers().contains(NodeId.nodeId("4")));
        assertTrue(forkedPartition.getMembers().contains(NodeId.nodeId("5")));
    }

}
