/*
 * Copyright 2019-present Open Networking Foundation
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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.hash.Hashing;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cluster.PartitionId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncConsistentMultimap;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Versioned;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertThat;

public class PartitionedAsyncConsistentMultimapTest {

    private static PartitionedAsyncConsistentMultimap<String, String> partitionedAsyncConsistentMap;
    private static Map<PartitionId, AsyncConsistentMultimap<String, String>> partitions;
    private static List<PartitionId> sortedMemberPartitionIds;
    private static Serializer serializer;

    private static final String PARTITION_NAME = "PartitionManager";

    private static final String KEY1 = "AAA";
    private static final String VALUE1 = "one";
    private static final String KEY2 = "BBB";
    private static final String VALUE2 = "two";
    private static final String KEY3 = "CCC";
    private static final String VALUE3 = "three";
    private static final String KEY4  = "DDD";
    private static final String VALUE4 = "four";

    private final List<String> allKeys = Lists.newArrayList(KEY1, KEY2,
                                                            KEY3, KEY4);
    private final List<String> allValues = Lists.newArrayList(VALUE1, VALUE2,
                                                                VALUE3, VALUE4);

    @Before
    public void setUp() throws Exception {
        // Init maps and partitions
        AsyncConsistentMultimap<String, String> asyncMap1 = new AsyncConsistentMultimapMock<>();
        AsyncConsistentMultimap<String, String> asyncMap2 = new AsyncConsistentMultimapMock<>();
        AsyncConsistentMultimap<String, String> asyncMap3 = new AsyncConsistentMultimapMock<>();
        AsyncConsistentMultimap<String, String> asyncMap4 = new AsyncConsistentMultimapMock<>();
        PartitionId pid1 = PartitionId.from(1);
        PartitionId pid2 = PartitionId.from(2);
        PartitionId pid3 = PartitionId.from(3);
        PartitionId pid4 = PartitionId.from(4);
        partitions = new HashMap<>();
        serializer = Serializer.using(KryoNamespaces.BASIC);
        partitions.put(pid1, asyncMap1);
        partitions.put(pid2, asyncMap2);
        partitions.put(pid3, asyncMap3);
        partitions.put(pid4, asyncMap4);
        sortedMemberPartitionIds = Lists.newArrayList(partitions.keySet());
        Hasher<String> hasher = key -> {
            int hashCode = Hashing.sha256().hashBytes(serializer.encode(key)).asInt();
            return sortedMemberPartitionIds.get(Math.abs(hashCode) % partitions.size());
        };
        partitionedAsyncConsistentMap = new PartitionedAsyncConsistentMultimap<>(PARTITION_NAME,
                                                                                 partitions,
                                                                                 hasher);
    }

    @Test
    public void testPutAllRemoveAll() {
        // Init phase
        assertThat(partitionedAsyncConsistentMap.isEmpty().join(), is(true));
        assertThat(partitionedAsyncConsistentMap.size().join(), is(0));
        assertThat(partitionedAsyncConsistentMap.name(), is("PartitionManager"));
        // Test multi put
        Map<String, Collection<? extends String>> mapping = Maps.newHashMap();
        // First build the mappings having each key a different mapping
        allKeys.forEach(key -> {
            switch (key) {
                case KEY1:
                    mapping.put(key, Lists.newArrayList(allValues.subList(0, 1)));
                    break;
                case KEY2:
                    mapping.put(key, Lists.newArrayList(allValues.subList(0, 2)));
                    break;
                case KEY3:
                    mapping.put(key, Lists.newArrayList(allValues.subList(0, 3)));
                    break;
                default:
                    mapping.put(key, Lists.newArrayList(allValues.subList(0, 4)));
                    break;
            }
        });
        // Success
        assertThat(partitionedAsyncConsistentMap.putAll(mapping).join(), is(true));
        // Failure
        assertThat(partitionedAsyncConsistentMap.putAll(mapping).join(), is(false));
        // Verify operation
        assertThat(partitionedAsyncConsistentMap.size().join(), is(10));
        assertThat(partitionedAsyncConsistentMap.isEmpty().join(), is(false));
        // verify mapping is ok
        allKeys.forEach(key -> {
            switch (key) {
                case KEY1:
                    assertThat(Lists.newArrayList(Versioned.valueOrNull(partitionedAsyncConsistentMap.get(key).join())),
                               containsInAnyOrder(allValues.subList(0, 1).toArray()));
                    break;
                case KEY2:
                    assertThat(Lists.newArrayList(Versioned.valueOrNull(partitionedAsyncConsistentMap.get(key).join())),
                               containsInAnyOrder(allValues.subList(0, 2).toArray()));
                    break;
                case KEY3:
                    assertThat(Lists.newArrayList(Versioned.valueOrNull(partitionedAsyncConsistentMap.get(key).join())),
                               containsInAnyOrder(allValues.subList(0, 3).toArray()));
                    break;
                default:
                    assertThat(Lists.newArrayList(Versioned.valueOrNull(partitionedAsyncConsistentMap.get(key).join())),
                               containsInAnyOrder(allValues.subList(0, 4).toArray()));
                    break;
            }
        });
        // Success
        assertThat(partitionedAsyncConsistentMap.removeAll(mapping).join(), is(true));
        // Failure
        assertThat(partitionedAsyncConsistentMap.removeAll(mapping).join(), is(false));
        // Verify operation
        assertThat(partitionedAsyncConsistentMap.size().join(), is(0));
        assertThat(partitionedAsyncConsistentMap.isEmpty().join(), is(true));
    }
}
