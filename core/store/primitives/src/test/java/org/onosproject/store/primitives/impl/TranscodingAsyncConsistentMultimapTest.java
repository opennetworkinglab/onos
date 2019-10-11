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
import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.DeviceId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.AsyncConsistentMultimap;
import org.onosproject.store.service.AsyncConsistentMultimapAdapter;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.Versioned;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


public class TranscodingAsyncConsistentMultimapTest {

    private static Serializer serializer;
    private static AsyncConsistentMultimap<String, DeviceId> transcodingMap;
    private static AsyncConsistentMultimap<String, byte[]> baseMap;

    private static final String KEY1 = "Key1";
    private static final String KEY2 = "Key2";
    private static final String KEY3 = "Key3";
    private static final String KEY4 = "Key4";
    private static final DeviceId DEV1 = DeviceId.deviceId("Device1");
    private static final DeviceId DEV2 = DeviceId.deviceId("Device2");
    private static final DeviceId DEV3 = DeviceId.deviceId("foo");
    private static final DeviceId DEV4 = DeviceId.deviceId("bar");
    private final List<String> allKeys = Lists.newArrayList(KEY1, KEY2,
                                                            KEY3, KEY4);
    private final List<DeviceId> allValues = Lists.newArrayList(DEV1, DEV2,
                                                              DEV3, DEV4);

    @Before
    public void setUp() throws Exception {
        // Init base map and serializer
        serializer = Serializer.using(KryoNamespaces.API);
        baseMap = new AsyncConsistentMultimapMock();
        // Create the transcoding map
        transcodingMap =  DistributedPrimitives.newTranscodingMultimap(
                baseMap,
                Function.identity(),
                Function.identity(),
                serializer::encode,
                serializer::decode);
    }

    @Test
    public void testPutAllRemoveAll() throws Exception {
        // Init phase
        assertThat(transcodingMap.size().join(), is(0));
        assertThat(transcodingMap.isEmpty().join(), is(true));
        // Test multi put
        Map<String, Collection<? extends DeviceId>> mapping = Maps.newHashMap();
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
        assertThat(transcodingMap.putAll(mapping).join(), is(true));
        // Failure
        assertThat(transcodingMap.putAll(mapping).join(), is(false));
        // Verify operation
        assertThat(transcodingMap.size().join(), is(10));
        assertThat(transcodingMap.isEmpty().join(), is(false));
        // verify mapping is ok
        allKeys.forEach(key -> {
            switch (key) {
                case KEY1:
                    assertThat(Lists.newArrayList(Versioned.valueOrNull(transcodingMap.get(key).join())),
                               containsInAnyOrder(allValues.subList(0, 1).toArray()));
                    break;
                case KEY2:
                    assertThat(Lists.newArrayList(Versioned.valueOrNull(transcodingMap.get(key).join())),
                               containsInAnyOrder(allValues.subList(0, 2).toArray()));
                    break;
                case KEY3:
                    assertThat(Lists.newArrayList(Versioned.valueOrNull(transcodingMap.get(key).join())),
                               containsInAnyOrder(allValues.subList(0, 3).toArray()));
                    break;
                default:
                    assertThat(Lists.newArrayList(Versioned.valueOrNull(transcodingMap.get(key).join())),
                               containsInAnyOrder(allValues.subList(0, 4).toArray()));
                    break;
            }
        });
        // Success
        assertThat(transcodingMap.removeAll(mapping).join(), is(true));
        // Failure
        assertThat(transcodingMap.removeAll(mapping).join(), is(false));
        // Verify operation
        assertThat(transcodingMap.size().join(), is(0));
        assertThat(transcodingMap.isEmpty().join(), is(true));
    }

    // It uses a special internal map for bytes comparison - otherwise the equality cannot be verified
    public static class AsyncConsistentMultimapMock extends AsyncConsistentMultimapAdapter<String, byte[]> {
        private final Map<String, Set<byte[]>> baseMap = new HashMap<>();
        private static final int DEFAULT_CREATION_TIME = 0;
        private static final int DEFAULT_VERSION = 0;

        AsyncConsistentMultimapMock() { }

        Versioned<Collection<? extends byte[]>> makeVersioned(Collection<? extends byte[]> v) {
            return new Versioned<>(v, DEFAULT_VERSION, DEFAULT_CREATION_TIME);
        }

        @Override
        public CompletableFuture<Integer> size() {
            return CompletableFuture.completedFuture(baseMap.values().stream()
                                                             .map(Set::size)
                                                             .mapToInt(size -> size).sum());
        }

        @Override
        public CompletableFuture<Boolean> isEmpty() {
            return CompletableFuture.completedFuture(baseMap.isEmpty());
        }

        @Override
        public CompletableFuture<Boolean> putAll(Map<String, Collection<? extends byte[]>> mapping) {
            CompletableFuture<Boolean> result = CompletableFuture.completedFuture(false);
            for (Map.Entry<String, Collection<? extends byte[]>> entry : mapping.entrySet()) {
                Set<byte[]> values = baseMap.computeIfAbsent(
                        entry.getKey(), k -> Sets.newTreeSet(new ByteArrayComparator()));
                for (byte[] value : entry.getValue()) {
                    if (values.add(value)) {
                        result = CompletableFuture.completedFuture(true);
                    }
                }
            }
            return result;
        }

        @Override
        public CompletableFuture<Versioned<Collection<? extends byte[]>>> get(String key) {
            return CompletableFuture.completedFuture(makeVersioned(baseMap.get(key)));
        }

        @Override
        public CompletableFuture<Boolean> removeAll(Map<String, Collection<? extends byte[]>> mapping) {
            CompletableFuture<Boolean> result = CompletableFuture.completedFuture(false);
            for (Map.Entry<String, Collection<? extends byte[]>> entry : mapping.entrySet()) {
                Set<byte[]> values = baseMap.get(entry.getKey());
                if (values == null) {
                    return CompletableFuture.completedFuture(false);
                }
                for (byte[] value : entry.getValue()) {
                    if (values.remove(value)) {
                        result = CompletableFuture.completedFuture(true);
                    }
                }
                if (values.isEmpty()) {
                    baseMap.remove(entry.getKey());
                }
            }
            return result;
        }

        private static class ByteArrayComparator implements Comparator<byte[]> {
            @Override
            public int compare(byte[] o1, byte[] o2) {
                if (Arrays.equals(o1, o2)) {
                    return 0;
                } else {
                    for (int i = 0; i < o1.length && i < o2.length; i++) {
                        if (o1[i] < o2[i]) {
                            return -1;
                        } else if (o1[i] > o2[i]) {
                            return 1;
                        }
                    }
                    return o1.length > o2.length ? 1 : -1;
                }
            }
        }
    }

}