/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.persistence.impl;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.store.service.Serializer;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Test suite for Persistent Set.
 */
public class PersistentSetTest extends MapDBTest {

    private PersistentSet<Integer> set = null;

    @Before
    public void setUp() throws Exception {
        //Creates a set within it and a basic integer serializer
        set = new PersistentSet<>(new Serializer() {
            @Override
            public <T> byte[] encode(T object) {
                if (object == null) {
                    return null;
                }
                int num = (Integer) object;
                byte[] result = new byte[4];

                result[0] = (byte) (num >> 24);
                result[1] = (byte) (num >> 16);
                result[2] = (byte) (num >> 8);
                result[3] = (byte) num;
                return result;
            }

            @Override
            public <T> T decode(byte[] bytes) {
                if (bytes == null) {
                    return null;
                }
                int num = 0x00000000;

                num = num | bytes[0] << 24;
                num = num | bytes[1] << 16;
                num = num | bytes[2] << 8;
                num = num | bytes[3];

                return (T) Integer.valueOf(num);
            }

            @Override
            public <T> T copy(T object) {
                return decode(encode(object));
            }
        }, fakeDB, "set");

    }

    @Test
    public void testSize() throws Exception {
        //Check correct sizing throughout population
        for (int i = 0; i < 10; i++) {
            set.add(i);
            assertEquals("The set should have i + 1 entries.", i + 1, set.size());
        }
    }

    @Test
    public void testIsEmpty() throws Exception {
        //test empty condition
        assertTrue("The set should be initialized empty.", set.isEmpty());
        fillSet(5, this.set);
        assertFalse("The set should no longer be empty.", set.isEmpty());
        set.clear();
        assertTrue("The set should have been cleared.", set.isEmpty());
    }

    @Test
    public void testContains() throws Exception {
        //Test contains
        assertFalse("The set should not contain anything", set.contains(1));
        fillSet(10, this.set);
        for (int i = 0; i < 10; i++) {
            assertTrue("The set should contain all values 0-9.", set.contains(i));
        }
    }

    @Test
    public void testIterator() throws Exception {
        //Test iterator behavior (no order guarantees are made)
        Set<Integer> validationSet = Sets.newHashSet();
        fillSet(10, this.set);
        fillSet(10, validationSet);
        set.iterator().forEachRemaining(item -> assertTrue("Items were mismatched.", validationSet.remove(item)));
        //All values should have been seen and removed
        assertTrue("All entries in the validation set should have been removed.", validationSet.isEmpty());
    }

    @Test
    public void testToArray() throws Exception {
        //Test creation of a new array of the values
        fillSet(10, set);
        Object[] arr = set.toArray();
        assertEquals("The array should be of length 10.", 10, arr.length);
        for (int i = 0; i < 10; i++) {
            assertTrue("All elements of the array should be in the set.", set.contains(arr[i]));
        }
    }

    @Test
    public void testToArray1() throws Exception {
        //Test creation of a new array with the possibility of populating passed array if space allows
        Integer[] originalArray = new Integer[9];
        fillSet(9, set);
        //Test the case where the array and set match in size
        Object[] retArray = set.toArray(originalArray);
        assertSame("If the set can fit the array should be the one passed in.", originalArray, retArray);
        //Test the case where the passe array is too small to fit the set
        set.add(9);
        assertNotEquals("A new set should be generated if the contents will not fit in the passed set",
                        set.toArray(originalArray), originalArray);
        //Now test the case where there should be a null terminator
        set.clear();
        fillSet(5, set);
        assertNull("The character one after last should be null if the array is larger than the set.",
                   set.toArray(originalArray)[5]);
    }

    @Test
    public void testAdd() throws Exception {
        //Test of add
        for (int i = 0; i < 10; i++) {
            assertEquals("The size of the set is wrong.", i, set.size());
            assertTrue("The first add of an element should be true.", set.add(i));
            assertFalse("The second add of an element should be false.", set.add(i));
        }
    }

    @Test
    public void testRemove() throws Exception {
        //Test removal
        fillSet(10, set);
        for (int i = 0; i < 10; i++) {
            assertEquals("The size of the set is wrong.", 10 - i, set.size());
            assertTrue("The first removal should be true.", set.remove(i));
            assertFalse("The second removal should be false (item no longer contained).", set.remove(i));
        }
        assertTrue("All elements should have been removed.", set.isEmpty());
    }

    @Test
    public void testContainsAll() throws Exception {
        //Test contains with short circuiting
        Set<Integer> integersToCheck = Sets.newHashSet();
        fillSet(10, integersToCheck);
        fillSet(10, set);
        assertTrue("The sets should be identical so mutual subsets.", set.containsAll(integersToCheck));
        set.remove(9);
        assertFalse("The set should contain one fewer value.", set.containsAll(integersToCheck));
    }

    @Test
    public void testAddAll() throws Exception {
        //Test multi-adds with change checking
        Set<Integer> integersToCheck = Sets.newHashSet();
        fillSet(10, integersToCheck);
        assertFalse("Set should be empty and so integers to check should not be a subset.",
                    set.containsAll(integersToCheck));
        assertTrue("The set should have changed as a result of add all.", set.addAll(integersToCheck));
        assertFalse("The set should not have changed as a result of add all a second time.",
                    set.addAll(integersToCheck));
        assertTrue("The sets should now be equivalent.", set.containsAll(integersToCheck));
        assertTrue("The sets should now be equivalent.", integersToCheck.containsAll(set));
    }

    @Test
    public void testRetainAll() throws Exception {
        //Test ability to generate the intersection set
        Set<Integer> retainSet = Sets.newHashSet();
        fillSet(10, set);
        assertTrue("The set should have changed.", set.retainAll(retainSet));
        assertTrue("The set should have been emptied.", set.isEmpty());
        fillSet(10, set);
        fillSet(10, retainSet);
        Set<Integer> duplicateSet = new HashSet<>(set);
        assertFalse("The set should not have changed.", set.retainAll(retainSet));
        assertEquals("The set should be the same as the duplicate.", duplicateSet, set);
        retainSet.remove(9);
        assertTrue("The set should have changed.", set.retainAll(retainSet));
        duplicateSet.remove(9);
        assertEquals("The set should have had the nine element removed.", duplicateSet, set);
    }

    @Test
    public void testRemoveAll() throws Exception {
        //Test for mass removal and change checking
        Set<Integer> removeSet = Sets.newHashSet();
        fillSet(10, set);
        Set<Integer> duplicateSet = Sets.newHashSet(set);
        assertFalse("No elements should change.", set.removeAll(removeSet));
        assertEquals("Set should not have diverged from the duplicate.", duplicateSet, set);
        fillSet(5, removeSet);
        assertTrue("Elements should have been removed.", set.removeAll(removeSet));
        assertNotEquals("Duplicate set should no longer be equivalent.", duplicateSet, set);
        assertEquals("Five elements should have been removed from set.", 5, set.size());
        for (Integer item : removeSet) {
            assertFalse("No element of remove set should remain.", set.contains(item));
        }
    }

    @Test
    public void testClear() throws Exception {
        //Test set emptying
        assertTrue("The set should be initialized empty.", set.isEmpty());
        set.clear();
        assertTrue("Clear should have no effect on an empty set.", set.isEmpty());
        fillSet(10, set);
        assertFalse("The set should no longer be empty.", set.isEmpty());
        set.clear();
        assertTrue("The set should be empty after clear.", set.isEmpty());
    }

    /**
     * Populated the map with integers from (0) up to (numEntries - 1).
     *
     * @param numEntries number of entries to add
     */
    private void fillSet(int numEntries, Set<Integer> set) {
        checkNotNull(set);
        for (int i = 0; i < numEntries; i++) {
            set.add(i);
        }
    }
}
