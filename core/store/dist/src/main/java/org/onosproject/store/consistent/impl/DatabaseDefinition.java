/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.consistent.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.onosproject.store.cluster.impl.NodeInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Partitioned database configuration.
 */
public class DatabaseDefinition {
    private Map<String, Set<NodeInfo>> partitions;
    private Set<NodeInfo> nodes;

    /**
     * Creates a new DatabaseDefinition.
     *
     * @param partitions partition map
     * @param nodes      set of nodes
     * @return database definition
     */
    public static DatabaseDefinition from(Map<String, Set<NodeInfo>> partitions,
                                          Set<NodeInfo> nodes) {
        checkNotNull(partitions);
        checkNotNull(nodes);
        DatabaseDefinition definition = new DatabaseDefinition();
        definition.partitions = ImmutableMap.copyOf(partitions);
        definition.nodes = ImmutableSet.copyOf(nodes);
        return definition;
    }

    /**
     * Creates a new DatabaseDefinition using default partitions.
     *
     * @param nodes set of nodes
     * @return database definition
     */
    public static DatabaseDefinition from(Set<NodeInfo> nodes) {
        return from(generateDefaultPartitions(nodes), nodes);
    }

    /**
     * Returns the map of database partitions.
     *
     * @return db partition map
     */
    public Map<String, Set<NodeInfo>> getPartitions() {
        return partitions;
    }

    /**
     * Returns the set of nodes.
     *
     * @return nodes
     */
    public Set<NodeInfo> getNodes() {
        return nodes;
    }


    /**
     * Generates set of default partitions using permutations of the nodes.
     *
     * @param nodes information about cluster nodes
     * @return default partition map
     */
    private static Map<String, Set<NodeInfo>> generateDefaultPartitions(Set<NodeInfo> nodes) {
        List<NodeInfo> sorted = new ArrayList<>(nodes);
        Collections.sort(sorted, (o1, o2) -> o1.getId().compareTo(o2.getId()));
        Map<String, Set<NodeInfo>> partitions = Maps.newHashMap();

        int length = nodes.size();
        int count = 3;
        for (int i = 0; i < length; i++) {
            Set<NodeInfo> set = new HashSet<>(count);
            for (int j = 0; j < count; j++) {
                set.add(sorted.get((i + j) % length));
            }
            partitions.put("p" + (i + 1), set);
        }
        return partitions;
    }

}