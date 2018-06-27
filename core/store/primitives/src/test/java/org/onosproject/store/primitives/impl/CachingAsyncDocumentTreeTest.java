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
import org.onosproject.store.primitives.DefaultDocumentTree;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTree;
import org.onosproject.store.primitives.resources.impl.AtomixDocumentTreeService;
import org.onosproject.store.primitives.resources.impl.AtomixTestBase;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.DocumentPath;
import org.onosproject.store.service.DocumentTree;
import org.onosproject.store.service.Ordering;
import org.onosproject.store.service.Serializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for cached {@link AtomixDocumentTree}.
 */
public class CachingAsyncDocumentTreeTest extends AtomixTestBase<AtomixDocumentTree> {

    @Override
    protected RaftService createService() {
        return new AtomixDocumentTreeService(Ordering.NATURAL);
    }

    @Override
    protected AtomixDocumentTree createPrimitive(RaftProxy proxy) {
        return new AtomixDocumentTree(proxy);
    }

    /**
     * Tests that reads following events are not stale when cached.
     */
    @Test
    public void testCacheConsistency() throws Throwable {
        Serializer serializer = Serializer.using(KryoNamespaces.BASIC);

        DocumentTree<String> tree1 = new DefaultDocumentTree<>(
            new CachingAsyncDocumentTree<>(
                new DefaultDistributedDocumentTree<>(
                    "testCacheConsistency",
                    newPrimitive("testCacheConsistency"),
                    serializer)), 5000);
        DocumentTree<String> tree2 = new DefaultDocumentTree<>(
            new CachingAsyncDocumentTree<>(
                new DefaultDistributedDocumentTree<>(
                    "testCacheConsistency",
                    newPrimitive("testCacheConsistency"),
                    serializer)), 5000);

        CountDownLatch latch = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean();

        Executor executor = Executors.newSingleThreadExecutor();
        DocumentPath path = DocumentPath.from("root|foo");
        tree1.addListener(path, event -> executor.execute(() -> {
            // Check only the "baz" value since it's the last one written. If we check for "bar" on the "bar" event,
            // there's a race in the test wherein the cache can legitimately be updated to "baz" before the next read.
            if (event.newValue().get().value().equals("baz")) {
                try {
                    assertEquals(event.newValue().get().value(), tree1.get(path).value());
                } catch (AssertionError e) {
                    failed.set(true);
                }
                latch.countDown();
            }
        }));

        tree2.set(path, "bar");
        tree2.set(path, "baz");

        latch.await(10, TimeUnit.SECONDS);
        if (latch.getCount() == 1 || failed.get()) {
            fail();
        }
    }
}