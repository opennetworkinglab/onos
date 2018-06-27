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

import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.atomix.protocols.raft.proxy.RaftProxy;
import io.atomix.protocols.raft.service.RaftService;
import org.junit.Test;
import org.onosproject.store.primitives.DefaultConsistentMultimap;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimap;
import org.onosproject.store.primitives.resources.impl.AtomixConsistentSetMultimapService;
import org.onosproject.store.primitives.resources.impl.AtomixTestBase;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMultimap;
import org.onosproject.store.service.Serializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for cached {@link AtomixConsistentSetMultimap}.
 */
public class CachingAsyncConsistentMultimapTest extends AtomixTestBase<AtomixConsistentSetMultimap> {

    @Override
    protected RaftService createService() {
        return new AtomixConsistentSetMultimapService();
    }

    @Override
    protected AtomixConsistentSetMultimap createPrimitive(RaftProxy proxy) {
        return new AtomixConsistentSetMultimap(proxy);
    }

    /**
     * Tests that reads following events are not stale when cached.
     */
    @Test
    public void testCacheConsistency() throws Throwable {
        Serializer serializer = Serializer.using(KryoNamespaces.BASIC);

        ConsistentMultimap<String, String> multimap1 = new DefaultConsistentMultimap<>(
            new CachingAsyncConsistentMultimap<>(
                new TranscodingAsyncConsistentMultimap<>(
                    newPrimitive("testCacheConsistency"),
                    k -> k,
                    k -> k,
                    v -> serializer.decode(v),
                    v -> serializer.encode(v))), 5000);
        ConsistentMultimap<String, String> multimap2 = new DefaultConsistentMultimap<>(
            new CachingAsyncConsistentMultimap<>(
                new TranscodingAsyncConsistentMultimap<>(
                    newPrimitive("testCacheConsistency"),
                    k -> k,
                    k -> k,
                    v -> serializer.decode(v),
                    v -> serializer.encode(v))), 5000);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean();

        Executor executor = Executors.newSingleThreadExecutor();
        multimap1.addListener(event -> {
            if (event.newValue().equals("baz")) {
                Collection<? extends String> values = multimap1.get("foo").value();
                try {
                    assertEquals(2, values.size());
                } catch (AssertionError e) {
                    failed.set(true);
                }
                latch.countDown();
            }
        }, executor);

        multimap2.put("foo", "bar");
        multimap2.put("foo", "baz");

        latch.await(10, TimeUnit.SECONDS);
        if (latch.getCount() == 1 || failed.get()) {
            fail();
        }
    }
}
