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

package org.onosproject.persistence.impl;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.store.service.Serializer;

import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Test suite for Persistent Map.
 */
public class PersistentMapTest extends MapDBTest {

    private PersistentMap<Integer, Integer> map = null;

    /**
     * Set up the database, create a map and a direct executor to handle it.
     *
     * @throws Exception if instantiation fails
     */
    @Before
    public void setUp() throws Exception {
        //Creates, a map within it and a basic integer serializer
        map = new PersistentMap<>(new Serializer() {
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
        }, fakeDB, "map");
    }

    @Test
    public void testRemove() throws Exception {
        //Checks removal and return values
        fillMap(10);
        assertEquals(10, map.size());
        for (int i = 0; i < 10; i++) {
            assertEquals("The previous value was wrong.", Integer.valueOf(i), map.remove(i));
            assertNull("The previous value was wrong.", map.remove(i));
            //(i+1) compensates for base zero.
            assertEquals("The size was wrong.", 10 - (i + 1), map.size());
        }
    }

    @Test
    public void testSize() throws Exception {
        //Checks size values throughout addition and removal
        for (int i = 0; i < 10; i++) {
            map.put(i, i);
            assertEquals("The map size is wrong.", i + 1, map.size());
        }
        for (int i = 0; i < 10; i++) {
            map.remove(i);
            assertEquals("The map size is wrong.", 9 - i, map.size());
        }
    }

    @Test
    public void testIsEmpty() throws Exception {
        //Checks empty condition
        //asserts that the map starts out empty
        assertTrue("Map should be empty", map.isEmpty());
        map.put(1, 1);
        assertFalse("Map shouldn't be empty.", map.isEmpty());
        map.remove(1);
        assertTrue("Map should be empty", map.isEmpty());
    }

    @Test
    public void testContains() throws Exception {
        //Checks both containsKey and containsValue be aware the implementations vary widely (value does not use mapDB
        //due to object '=='being an insufficient check)
        for (int i = 0; i < 10; i++) {
            assertFalse("Map should not contain the key", map.containsKey(i));
            assertFalse("Map should not contain the value", map.containsValue(i));
            map.put(i, i);
            assertTrue("Map should contain the key", map.containsKey(i));
            assertTrue("Map should contain the value", map.containsValue(i));
        }
    }

    @Test
    public void testGet() throws Exception {
        //Tests value retrieval and nonexistent key return values
        for (int i = 0; i < 10; i++) {
            map.put(i, i);
            for (int j = 0; j <= i; j++) {
                assertEquals("The value was wrong.", Integer.valueOf(j), map.get(j));
            }
        }
        assertNull("Null return value for nonexistent keys.", map.get(10));
    }

    @Test
    public void testPutAll() throws Exception {
        //Tests adding of an outside map
        Map<Integer, Integer> testMap = Maps.newHashMap();
        fillMap(10);
        map.putAll(testMap);
        for (int i = 0; i < 10; i++) {
            assertTrue("The map should contain the current 'i' value.", map.containsKey(i));
            assertTrue("The map should contain the current 'i' value.", map.containsValue(i));
        }
    }

    @Test
    public void testClear() throws Exception {
        //Tests clearing the map
        assertTrue("Map was initialized incorrectly, should be empty.", map.isEmpty());
        fillMap(10);
        assertFalse("Map should contain entries now.", map.isEmpty());
        map.clear();
        assertTrue("Map should have been cleared of entries.", map.isEmpty());

    }

    @Test
    public void testKeySet() throws Exception {
        //Tests key set generation
        fillMap(10);
        Set<Integer> keys = map.keySet();
        for (int i = 0; i < 10; i++) {
            assertTrue("The key set doesn't contain all keys 0-9", keys.contains(i));
        }
        assertEquals("The key set has an incorrect number of entries", 10, keys.size());
    }

    @Test
    public void testValues() throws Exception {
        //Tests value set generation
        fillMap(10);
        Set<Integer> values = (Set<Integer>) map.values();
        for (int i = 0; i < 10; i++) {
            assertTrue("The key set doesn't contain all keys 0-9", values.contains(i));
        }
        assertEquals("The key set has an incorrect number of entries", 10, values.size());
    }

    @Test
    public void testEntrySet() throws Exception {
        //Test entry set generation (violates abstraction by knowing the type of the returned entries)
        fillMap(10);
        Set<Map.Entry<Integer, Integer>> entries = map.entrySet();
        for (int i = 0; i < 10; i++) {
            assertTrue("The key set doesn't contain all keys 0-9", entries.contains(Maps.immutableEntry(i, i)));
        }
        assertEquals("The key set has an incorrect number of entries", 10, entries.size());
    }

    @Test public void testPut() throws Exception {
        //Tests insertion behavior (particularly the returning of previous value)
        fillMap(10);
        for (int i = 0; i < 10; i++) {
            assertEquals("Put should return the previous value", Integer.valueOf(i), map.put(i, i + 1));
        }
        assertNull(map.put(11, 11));
    }

    /**
     * Populated the map with pairs of integers from (0, 0) up to (numEntries - 1, numEntries -1).
     * @param numEntries number of entries to add
     */
    private void fillMap(int numEntries) {
        for (int i = 0; i < numEntries; i++) {
            map.put(i, i);
        }
    }
}
