/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onlab.util;

import java.util.Objects;

import org.junit.Test;

import com.google.common.collect.Maps;

import static org.junit.Assert.*;

/**
 * Unit tests for ExtendedSet.
 */
public class ExtendedSetTest {

    @Test
    public void testGet() {
        ExtendedSet<TestValue> set = new ExtendedSet<>(Maps.newConcurrentMap());
        TestValue e1 = new TestValue("foo", 1);
        set.add(e1);
        TestValue lookupValue = new TestValue("foo", 2);
        TestValue setEntry = set.get(lookupValue);
        assertEquals(e1, setEntry);
    }

    @Test
    public void testInsertOrReplace() {
        ExtendedSet<TestValue> set = new ExtendedSet<>(Maps.newConcurrentMap());
        TestValue small = new TestValue("foo", 1);
        TestValue medium = new TestValue("foo", 2);
        TestValue large = new TestValue("foo", 3);
        // input TestValue will replace existing TestValue if its value2() is greater
        // than existing entry's value2()
        assertTrue(set.insertOrReplace(small, existing -> existing.value2() < small.value2()));
        assertTrue(set.insertOrReplace(large, existing -> existing.value2() < large.value2()));
        assertFalse(set.insertOrReplace(medium, existing -> existing.value2() < medium.value2()));

        assertTrue(set.contains(small));
        assertTrue(set.contains(medium));
        assertTrue(set.contains(large));
    }

    @Test
    public void testConditionalRemove() {
        ExtendedSet<TestValue> set = new ExtendedSet<>(Maps.newConcurrentMap());
        TestValue small = new TestValue("foo", 1);
        TestValue medium = new TestValue("foo", 2);

        assertTrue(set.add(small));
        set.conditionalRemove(medium, existing -> existing.value2() < medium.value2);
        assertFalse(set.contains(small));
    }

    private class TestValue {
        private String value1;
        private int value2;

        public TestValue(String v1, int v2) {
            this.value1 = v1;
            this.value2 = v2;
        }

        public String value1() {
            return value1;
        }

        public int value2() {
            return value2;
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof TestValue) {
                TestValue that = (TestValue) other;
                return Objects.equals(value1, that.value1);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(value1);
        }
    }
}
