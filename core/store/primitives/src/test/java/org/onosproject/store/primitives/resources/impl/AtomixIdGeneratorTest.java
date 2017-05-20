/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.store.primitives.resources.impl;

import java.util.concurrent.CompletableFuture;

import io.atomix.resource.ResourceType;
import io.atomix.variables.DistributedLong;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Unit test for {@code AtomixIdGenerator}.
 */
public class AtomixIdGeneratorTest extends AtomixTestBase {

    @BeforeClass
    public static void preTestSetup() throws Throwable {
        createCopycatServers(3);
    }

    @AfterClass
    public static void postTestCleanup() throws Exception {
        clearTests();
    }

    @Override
    protected ResourceType resourceType() {
        return new ResourceType(DistributedLong.class);
    }

    /**
     * Tests generating IDs.
     */
    @Test
    public void testNextId() throws Throwable {
        AtomixIdGenerator idGenerator1 = new AtomixIdGenerator("testNextId",
                createAtomixClient().getLong("testNextId").join());
        AtomixIdGenerator idGenerator2 = new AtomixIdGenerator("testNextId",
                createAtomixClient().getLong("testNextId").join());

        CompletableFuture<Long> future11 = idGenerator1.nextId();
        CompletableFuture<Long> future12 = idGenerator1.nextId();
        CompletableFuture<Long> future13 = idGenerator1.nextId();
        assertEquals(Long.valueOf(1), future11.join());
        assertEquals(Long.valueOf(2), future12.join());
        assertEquals(Long.valueOf(3), future13.join());

        CompletableFuture<Long> future21 = idGenerator1.nextId();
        CompletableFuture<Long> future22 = idGenerator1.nextId();
        CompletableFuture<Long> future23 = idGenerator1.nextId();
        assertEquals(Long.valueOf(6), future23.join());
        assertEquals(Long.valueOf(5), future22.join());
        assertEquals(Long.valueOf(4), future21.join());

        CompletableFuture<Long> future31 = idGenerator2.nextId();
        CompletableFuture<Long> future32 = idGenerator2.nextId();
        CompletableFuture<Long> future33 = idGenerator2.nextId();
        assertEquals(Long.valueOf(1001), future31.join());
        assertEquals(Long.valueOf(1002), future32.join());
        assertEquals(Long.valueOf(1003), future33.join());
    }

    /**
     * Tests generating IDs.
     */
    @Test
    public void testNextIdBatchRollover() throws Throwable {
        AtomixIdGenerator idGenerator1 = new AtomixIdGenerator("testNextIdBatchRollover",
                createAtomixClient().getLong("testNextIdBatchRollover").join(), 2);
        AtomixIdGenerator idGenerator2 = new AtomixIdGenerator("testNextIdBatchRollover",
                createAtomixClient().getLong("testNextIdBatchRollover").join(), 2);

        CompletableFuture<Long> future11 = idGenerator1.nextId();
        CompletableFuture<Long> future12 = idGenerator1.nextId();
        CompletableFuture<Long> future13 = idGenerator1.nextId();
        assertEquals(Long.valueOf(1), future11.join());
        assertEquals(Long.valueOf(2), future12.join());
        assertEquals(Long.valueOf(3), future13.join());

        CompletableFuture<Long> future21 = idGenerator2.nextId();
        CompletableFuture<Long> future22 = idGenerator2.nextId();
        CompletableFuture<Long> future23 = idGenerator2.nextId();
        assertEquals(Long.valueOf(5), future21.join());
        assertEquals(Long.valueOf(6), future22.join());
        assertEquals(Long.valueOf(7), future23.join());

        CompletableFuture<Long> future14 = idGenerator1.nextId();
        CompletableFuture<Long> future15 = idGenerator1.nextId();
        CompletableFuture<Long> future16 = idGenerator1.nextId();
        assertEquals(Long.valueOf(4), future14.join());
        assertEquals(Long.valueOf(9), future15.join());
        assertEquals(Long.valueOf(10), future16.join());
    }
}
