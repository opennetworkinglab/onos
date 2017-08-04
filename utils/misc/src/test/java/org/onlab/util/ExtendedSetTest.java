/*
 * Copyright 2015-present Open Networking Foundation
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;

import org.junit.Ignore;
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

        assertTrue(set.add(small));
        set.conditionalRemove(medium, existing -> existing.value2() > medium.value2);
        assertTrue(set.contains(small));

    }

    @Test
    public void testIsEmpty() {
        ExtendedSet<TestValue> nonemptyset = new ExtendedSet<>(Maps.newConcurrentMap());
        TestValue val = new TestValue("foo", 1);
        assertTrue(nonemptyset.add(val));
        assertTrue(nonemptyset.contains(val));
        assertFalse(nonemptyset.isEmpty());

        ExtendedSet<TestValue> emptyset = new ExtendedSet<>(Maps.newConcurrentMap());
        assertTrue(emptyset.isEmpty());

    }

    @Test
    public void testClear() {
        ExtendedSet<TestValue> set = new ExtendedSet<>(Maps.newConcurrentMap());
        TestValue val = new TestValue("foo", 1);
        assertTrue(set.add(val));
        assertTrue(set.contains(val));
        set.clear();
        assertFalse(set.contains(val));
    }

    @Test
    public void testSize() {
        ExtendedSet<TestValue> nonemptyset = new ExtendedSet<>(Maps.newConcurrentMap());
        TestValue val = new TestValue("foo", 1);
        assertTrue(nonemptyset.add(val));
        assertTrue(nonemptyset.contains(val));
        assertEquals(1, nonemptyset.size());
        TestValue secval = new TestValue("goo", 2);
        assertTrue(nonemptyset.add(secval));
        assertTrue(nonemptyset.contains(secval));
        assertEquals(2, nonemptyset.size());

        ExtendedSet<TestValue> emptyset = new ExtendedSet<>(Maps.newConcurrentMap());
        assertEquals(0, emptyset.size());
    }

    @Test
    public void testIterator() {
        ExtendedSet<TestValue> set = new ExtendedSet<>(Maps.newConcurrentMap());
        TestValue val = new TestValue("foo", 1);
        assertTrue(set.add(val));
        TestValue nextval = new TestValue("goo", 2);
        assertTrue(set.add(nextval));
        assertTrue(set.contains(nextval));
        Iterator<TestValue> iterator = set.iterator();
        assertEquals(val, iterator.next());
        assertTrue(iterator.hasNext());
        assertEquals(nextval, iterator.next());
    }

    @Test
    public void testToArray() {
        ExtendedSet<TestValue> set = new ExtendedSet<>(Maps.newConcurrentMap());
        TestValue val = new TestValue("foo", 1);
        assertTrue(set.add(val));
        TestValue nextval = new TestValue("goo", 2);
        assertTrue(set.add(nextval));
        Object[] array = set.toArray();
        TestValue[] valarray = {val, nextval};
        assertArrayEquals(valarray, array);
        assertTrue(set.toArray(new TestValue[0])[0] instanceof TestValue);

    }

    @Test
    public void testContainsAll() {
        ExtendedSet<TestValue> set = new ExtendedSet<>(Maps.newConcurrentMap());
        TestValue val = new TestValue("foo", 1);
        assertTrue(set.add(val));
        TestValue nextval = new TestValue("goo", 2);
        assertTrue(set.add(nextval));
        ArrayList<TestValue> vals = new ArrayList<TestValue>();
        vals.add(val);
        vals.add(nextval);
        assertTrue(set.containsAll(vals));
    }

    @Test
    public void testRemove() {
        ExtendedSet<TestValue> set = new ExtendedSet<>(Maps.newConcurrentMap());
        TestValue val = new TestValue("foo", 1);
        assertTrue(set.add(val));
        TestValue nextval = new TestValue("goo", 2);
        assertTrue(set.add(nextval));
        assertTrue(set.remove(val));
        assertFalse(set.contains(val));
        assertTrue(set.remove(nextval));
        assertFalse(set.contains(nextval));
    }

    @Test
    public void testAddAll() {
        ExtendedSet<TestValue> nonemptyset = new ExtendedSet<>(Maps.newConcurrentMap());
        TestValue val = new TestValue("foo", 1);
        assertTrue(nonemptyset.add(val));
        TestValue nextval = new TestValue("goo", 2);
        TestValue finalval = new TestValue("shoo", 3);
        TestValue extremeval = new TestValue("who", 4);
        assertTrue(nonemptyset.add(extremeval));
        ArrayList<TestValue> vals = new ArrayList<TestValue>();
        vals.add(nextval);
        vals.add(finalval);
        vals.add(extremeval);
        assertTrue(nonemptyset.addAll(vals));
        assertTrue(nonemptyset.contains(nextval));
        assertTrue(nonemptyset.contains(finalval));

        ExtendedSet<TestValue> emptyset = new ExtendedSet<>(Maps.newConcurrentMap());
        vals = new ArrayList<TestValue>();
        vals.add(val);
        vals.add(nextval);
        vals.add(finalval);
        assertTrue(emptyset.addAll(vals));
        assertTrue(emptyset.contains(val));
        assertTrue(emptyset.contains(nextval));
        assertTrue(emptyset.contains(finalval));
    }

    @Test
    public void testRemoveAll() {
        ExtendedSet<TestValue> set = new ExtendedSet<>(Maps.newConcurrentMap());
        TestValue val = new TestValue("foo", 1);
        assertTrue(set.add(val));
        TestValue nextval = new TestValue("goo", 2);
        assertTrue(set.add(nextval));
        TestValue finalval = new TestValue("shoo", 3);
        assertTrue(set.add(finalval));
        ArrayList<TestValue> vals = new ArrayList<TestValue>();
        vals.add(nextval);
        vals.add(finalval);
        vals.add(new TestValue("who", 4));
        assertTrue(set.removeAll(vals));
        assertFalse(set.contains(nextval));
        assertFalse(set.contains(finalval));
    }

    @Test
    @Ignore("retainAll appears to violate the documented semantics because it does" +
            " not properly remove the items that are not in the Collection parameter.")
    public void testRetainAll() {
        ExtendedSet<TestValue> set = new ExtendedSet<>(Maps.newConcurrentMap());
        TestValue small = new TestValue("foo", 1);
        assertTrue(set.add(small));
        TestValue medium = new TestValue("goo", 2);
        assertTrue(set.add(medium));
        TestValue large = new TestValue("shoo", 3);
        assertTrue(set.add(large));
        TestValue extreme = new TestValue("who", 4);
        assertTrue(set.add(extreme));
        ArrayList<TestValue> firstvals = new ArrayList<TestValue>();
        firstvals.add(medium);
        firstvals.add(extreme);
        set.retainAll(firstvals);
        assertTrue(set.contains(medium));
        assertTrue(set.contains(extreme));
        assertFalse(set.contains(small));
        assertFalse(set.contains(large));
        ArrayList<TestValue> secondval = new ArrayList<TestValue>();
        secondval.add(medium);
        set.retainAll(secondval);
        assertFalse(set.contains(extreme));
        assertTrue(set.contains(medium));
    }

    @Test
    public void testAddFailure() {
        ExtendedSet<TestValue> set = new ExtendedSet<>(Maps.newConcurrentMap());
        TestValue val = new TestValue("foo", 1);
        assertTrue(set.add(val));
        assertFalse(set.add(val));
    }

    @Test
    public void testRemoveFailure() {
        ExtendedSet<TestValue> set = new ExtendedSet<TestValue>(Maps.newConcurrentMap());
        TestValue val = new TestValue("foo", 1);
        assertFalse(set.remove(val));
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
