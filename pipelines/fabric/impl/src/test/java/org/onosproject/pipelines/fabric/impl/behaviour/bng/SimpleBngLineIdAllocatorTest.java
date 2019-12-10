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
 * limitations under the License.%
 */

package org.onosproject.pipelines.fabric.impl.behaviour.bng;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.fail;

/**
 * Tests for SimpleBngLineIdAllocator.
 */
public class SimpleBngLineIdAllocatorTest {

    private static final int SIZE = 10;

    @Test
    public void allocateAndReleaseTest() throws FabricBngLineIdAllocator.IdExhaustedException {
        var allocator = new SimpleBngLineIdAllocator(SIZE);

        var id1 = allocator.allocate(new MockAttachment(1));
        var sameAsId1 = allocator.allocate(new MockAttachment(1));

        var id2 = allocator.allocate(new MockAttachment(2));

        assertEquals(allocator.allocatedCount(), 2);
        assertEquals(allocator.freeCount(), SIZE - allocator.allocatedCount());

        assertEquals(id1, sameAsId1);
        assertNotEquals(id1, id2);

        allocator.release(new MockAttachment(1));
        assertEquals(allocator.allocatedCount(), 1);
        assertEquals(allocator.freeCount(), SIZE - allocator.allocatedCount());

        allocator.release(id2);
        assertEquals(allocator.allocatedCount(), 0);
        assertEquals(allocator.freeCount(), SIZE);
    }

    @Test
    public void exhaustionTest() throws FabricBngLineIdAllocator.IdExhaustedException {
        var allocator = new SimpleBngLineIdAllocator(SIZE);
        var equalTester = new EqualsTester();
        for (int i = 0; i < SIZE; i++) {
            // Add ID to equality group to later make sure that all IDs are
            // different.
            equalTester.addEqualityGroup(
                    allocator.allocate(new MockAttachment(i)));
        }

        assertEquals(allocator.allocatedCount(), SIZE);
        assertEquals(allocator.freeCount(), 0);
        equalTester.testEquals();

        try {
            allocator.allocate(new MockAttachment(SIZE + 1));
            fail("IdExhaustedException not thrown");
        } catch (FabricBngLineIdAllocator.IdExhaustedException e) {
            // Expected.
        }

        for (int i = 0; i < SIZE; i++) {
            allocator.release(new MockAttachment(i));
        }

        assertEquals(allocator.allocatedCount(), 0);
        assertEquals(allocator.freeCount(), SIZE);
    }

}