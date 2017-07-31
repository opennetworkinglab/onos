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

import io.atomix.protocols.raft.proxy.RaftProxy;
import io.atomix.protocols.raft.service.RaftService;
import org.junit.Test;
import org.onlab.util.Tools;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMap;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapService;
import org.onosproject.store.primitives.resources.impl.AtomixTestBase;
import org.onosproject.store.service.AsyncConsistentMap;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link AtomixConsistentMap}.
 */
public class NotNullConsistentMapTest extends AtomixTestBase<AtomixConsistentMap> {

    @Override
    protected RaftService createService() {
        return new AtomixConsistentMapService();
    }

    @Override
    protected AtomixConsistentMap createPrimitive(RaftProxy proxy) {
        return new AtomixConsistentMap(proxy);
    }

    /**
     * Tests not null values.
     */
    @Test
    public void testNotNullValues() throws Throwable {
        final byte[] rawFooValue = Tools.getBytesUtf8("Hello foo!");
        final byte[] rawBarValue = Tools.getBytesUtf8("Hello bar!");

        AsyncConsistentMap<String, byte[]> map =
                DistributedPrimitives.newNotNullMap(newPrimitive("testNotNullValues"));

        map.get("foo")
                .thenAccept(v -> assertNull(v)).join();
        map.put("foo", null)
                .thenAccept(v -> assertNull(v)).join();
        map.put("foo", rawFooValue).thenAccept(v -> assertNull(v)).join();
        map.get("foo").thenAccept(v -> {
            assertNotNull(v);
            assertTrue(Arrays.equals(v.value(), rawFooValue));
        }).join();
        map.put("foo", null).thenAccept(v -> {
            assertNotNull(v);
            assertTrue(Arrays.equals(v.value(), rawFooValue));
        }).join();
        map.get("foo").thenAccept(v -> assertNull(v)).join();
        map.replace("foo", rawFooValue, null)
                .thenAccept(replaced -> assertFalse(replaced)).join();
        map.replace("foo", null, rawBarValue)
                .thenAccept(replaced -> assertTrue(replaced)).join();
        map.get("foo").thenAccept(v -> {
            assertNotNull(v);
            assertTrue(Arrays.equals(v.value(), rawBarValue));
        }).join();
        map.replace("foo", rawBarValue, null)
                .thenAccept(replaced -> assertTrue(replaced)).join();
        map.get("foo").thenAccept(v -> assertNull(v)).join();
    }
}
