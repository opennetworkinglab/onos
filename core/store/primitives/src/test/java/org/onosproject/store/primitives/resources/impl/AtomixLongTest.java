/*
 * Copyright 2016-present Open Networking Laboratory
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

import io.atomix.Atomix;
import io.atomix.resource.ResourceType;
import io.atomix.variables.DistributedLong;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**git s
 * Unit tests for {@link AtomixCounter}.
 */
public class AtomixLongTest extends AtomixTestBase {

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

    @Test
    public void testBasicOperations() throws Throwable {
        basicOperationsTest();
    }

    protected void basicOperationsTest() throws Throwable {
        Atomix atomix = createAtomixClient();
        AtomixCounter along = new AtomixCounter("test-long-basic-operations",
                                                atomix.getLong("test-long").join());
        assertEquals(0, along.get().join().longValue());
        assertEquals(1, along.incrementAndGet().join().longValue());
        along.set(100).join();
        assertEquals(100, along.get().join().longValue());
        assertEquals(100, along.getAndAdd(10).join().longValue());
        assertEquals(110, along.get().join().longValue());
        assertFalse(along.compareAndSet(109, 111).join());
        assertTrue(along.compareAndSet(110, 111).join());
        assertEquals(100, along.addAndGet(-11).join().longValue());
        assertEquals(100, along.getAndIncrement().join().longValue());
        assertEquals(101, along.get().join().longValue());
    }
}
