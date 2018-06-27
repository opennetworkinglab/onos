/*
 * Copyright 2018-present Open Networking Foundation
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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.atomix.protocols.raft.proxy.RaftProxy;
import io.atomix.protocols.raft.service.RaftService;
import org.junit.Test;
import org.onosproject.store.primitives.DefaultConsistentMap;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMap;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentMapService;
import org.onosproject.store.primitives.resources.impl.AtomixTestBase;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.Serializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for cached {@link AtomixConsistentMap}.
 */
public class CachingAsyncConsistentMapTest extends AtomixTestBase<AtomixConsistentMap> {

    @Override
    protected RaftService createService() {
        return new AtomixConsistentMapService();
    }

    @Override
    protected AtomixConsistentMap createPrimitive(RaftProxy proxy) {
        return new AtomixConsistentMap(proxy);
    }

    /**
     * Tests that reads following events are not stale when cached.
     */
    @Test
    public void testCacheConsistency() throws Throwable {
        Serializer serializer = Serializer.using(KryoNamespaces.BASIC);

        ConsistentMap<String, String> map1 = new DefaultConsistentMap<>(
            new CachingAsyncConsistentMap<>(
                new TranscodingAsyncConsistentMap<>(
                    newPrimitive("testCacheConsistency"),
                    k -> k,
                    k -> k,
                    v -> serializer.encode(v),
                    v -> serializer.decode(v))), 5000);
        ConsistentMap<String, String> map2 = new DefaultConsistentMap<>(
            new CachingAsyncConsistentMap<>(
                new TranscodingAsyncConsistentMap<>(
                    newPrimitive("testCacheConsistency"),
                    k -> k,
                    k -> k,
                    v -> serializer.encode(v),
                    v -> serializer.decode(v))), 5000);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean();

        Executor executor = Executors.newSingleThreadExecutor();
        map1.addListener(event -> {
            // Check only the "baz" value since it's the last one written. If we check for "bar" on the "bar" event,
            // there's a race in the test wherein the cache can legitimately be updated to "baz" before the next read.
            if (event.newValue().value().equals("baz")) {
                try {
                    assertEquals(event.newValue().value(), map1.get("foo").value());
                } catch (AssertionError e) {
                    failed.set(true);
                }
                latch.countDown();
            }
        }, executor);

        map2.put("foo", "bar");
        map2.put("foo", "baz");

        latch.await(10, TimeUnit.SECONDS);
        if (latch.getCount() == 1 || failed.get()) {
            fail();
        }
    }
}