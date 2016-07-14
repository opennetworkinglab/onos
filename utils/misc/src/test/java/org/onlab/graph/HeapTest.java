/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onlab.graph;

import com.google.common.collect.Ordering;
import com.google.common.testing.EqualsTester;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Comparator;

import static com.google.common.collect.ImmutableList.of;
import static org.junit.Assert.*;

/**
 * Heap data structure tests.
 */
public class HeapTest {

    private ArrayList<Integer> data =
            new ArrayList<>(of(6, 4, 5, 9, 8, 3, 2, 1, 7, 0));

    private static final Comparator<Integer> MIN = Ordering.natural().reverse();
    private static final Comparator<Integer> MAX = Ordering.natural();

    @Test
    public void equality() {
        new EqualsTester()
                .addEqualityGroup(new Heap<>(data, MIN),
                                  new Heap<>(data, MIN))
                .addEqualityGroup(new Heap<>(data, MAX))
                .testEquals();
    }

    @Test
    public void empty() {
        Heap<Integer> h = new Heap<>(new ArrayList<Integer>(), MIN);
        assertTrue("should be empty", h.isEmpty());
        assertEquals("incorrect size", 0, h.size());
        assertNull("no item expected", h.extreme());
        assertNull("no item expected", h.extractExtreme());
    }

    @Test
    public void insert() {
        Heap<Integer> h = new Heap<>(data, MIN);
        assertEquals("incorrect size", 10, h.size());
        h.insert(3);
        assertEquals("incorrect size", 11, h.size());
    }

    @Test
    public void minQueue() {
        Heap<Integer> h = new Heap<>(data, MIN);
        assertFalse("should not be empty", h.isEmpty());
        assertEquals("incorrect size", 10, h.size());
        assertEquals("incorrect extreme", (Integer) 0, h.extreme());

        for (int i = 0, n = h.size(); i < n; i++) {
            assertEquals("incorrect element", (Integer) i, h.extractExtreme());
        }
        assertTrue("should be empty", h.isEmpty());
    }

    @Test
    public void maxQueue() {
        Heap<Integer> h = new Heap<>(data, MAX);
        assertFalse("should not be empty", h.isEmpty());
        assertEquals("incorrect size", 10, h.size());
        assertEquals("incorrect extreme", (Integer) 9, h.extreme());

        for (int i = h.size(); i > 0; i--) {
            assertEquals("incorrect element", (Integer) (i - 1), h.extractExtreme());
        }
        assertTrue("should be empty", h.isEmpty());
    }

    @Test
    public void iterator() {
        Heap<Integer> h = new Heap<>(data, MIN);
        assertTrue("should have next element", h.iterator().hasNext());
    }

}
