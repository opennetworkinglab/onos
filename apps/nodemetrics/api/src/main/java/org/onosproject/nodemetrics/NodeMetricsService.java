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

package org.onosproject.nodemetrics;

import org.onosproject.cluster.NodeId;

import java.util.Map;

/**
 * Nodemetrics Application is to fetch Controller Resource metrics.
 * The Control Resource metrics includes Memory,
 * CPU usage and Disk usage of All the cluster nodes.
 */
public interface NodeMetricsService {
    /**
     * Returns may memory information of all Cluster nodes.
     * @return map
     */
    Map<NodeId, NodeMemoryUsage> memory();

    /**
     * Returns may disk information of all Cluster nodes.
     * @return map
     */
    Map<NodeId, NodeDiskUsage> disk();

    /**
     * Returns may CPU information of all Cluster nodes.
     * @return map object, NodeId as key and NodeCpu information as value.
     */
    Map<NodeId, NodeCpuUsage> cpu();

    /**
     * Get the memory information of Specific Cluster node.
     * @param nodeid to get Memory information of that respective cluster node.
     * @return NodememoryUsage object.
     */
    NodeMemoryUsage memory(NodeId nodeid);

    /**
     * Get the disk information of Specific Cluster node.
     * @param nodeid to get disk information of that respective cluster node.
     * @return NodeDiskUsage object.
     */
    NodeDiskUsage disk(NodeId nodeid);

    /**
     * Get the CPU information of Specific Cluster node.
     * @param nodeid to get CPU information of that respective cluster node.
     * @return NodeCpuUsage object.
     */
    NodeCpuUsage cpu(NodeId nodeid);
}
