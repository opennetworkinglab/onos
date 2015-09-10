/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.store.consistent.impl;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

import org.junit.Test;
import org.onosproject.store.service.MapEvent;
import org.onosproject.store.service.Versioned;

/**
 * Unit tests for UpdateResult.
 */
public class UpdateResultTest {

    @Test
    public void testGetters() {
        Versioned<String> oldValue = new Versioned<>("a", 1);
        Versioned<String> newValue = new Versioned<>("b", 2);
        UpdateResult<String, String> ur =
                new UpdateResult<>(true, "foo", "k", oldValue, newValue);

        assertTrue(ur.updated());
        assertEquals("foo", ur.mapName());
        assertEquals("k", ur.key());
        assertEquals(oldValue, ur.oldValue());
        assertEquals(newValue, ur.newValue());
    }

    @Test
    public void testToMapEvent() {
        Versioned<String> oldValue = new Versioned<>("a", 1);
        Versioned<String> newValue = new Versioned<>("b", 2);
        UpdateResult<String, String> ur1 =
                new UpdateResult<>(true, "foo", "k", oldValue, newValue);
        MapEvent<String, String> event1 = ur1.toMapEvent();
        assertEquals(MapEvent.Type.UPDATE, event1.type());
        assertEquals("k", event1.key());
        assertEquals(newValue, event1.value());

        UpdateResult<String, String> ur2 =
                new UpdateResult<>(true, "foo", "k", null, newValue);
        MapEvent<String, String> event2 = ur2.toMapEvent();
        assertEquals(MapEvent.Type.INSERT, event2.type());
        assertEquals("k", event2.key());
        assertEquals(newValue, event2.value());

        UpdateResult<String, String> ur3 =
                new UpdateResult<>(true, "foo", "k", oldValue, null);
        MapEvent<String, String> event3 = ur3.toMapEvent();
        assertEquals(MapEvent.Type.REMOVE, event3.type());
        assertEquals("k", event3.key());
        assertEquals(oldValue, event3.value());

        UpdateResult<String, String> ur4 =
                new UpdateResult<>(false, "foo", "k", oldValue, oldValue);
        assertNull(ur4.toMapEvent());
    }

    @Test
    public void testMap() {
        Versioned<String> oldValue = new Versioned<>("a", 1);
        Versioned<String> newValue = new Versioned<>("b", 2);
        UpdateResult<String, String> ur1 =
                new UpdateResult<>(true, "foo", "k", oldValue, newValue);
        UpdateResult<Integer, Integer> ur2 = ur1.map(s -> s.length(), s -> s.length());

        assertEquals(ur2.updated(), ur1.updated());
        assertEquals(ur1.mapName(), ur2.mapName());
        assertEquals(new Integer(1), ur2.key());
        assertEquals(oldValue.map(s -> s.length()), ur2.oldValue());
        assertEquals(newValue.map(s -> s.length()), ur2.newValue());

        UpdateResult<String, String> ur3 =
                new UpdateResult<>(true, "foo", "k", null, newValue);
        UpdateResult<Integer, Integer> ur4 = ur3.map(s -> s.length(), s -> s.length());

        assertEquals(ur3.updated(), ur4.updated());
        assertEquals(ur3.mapName(), ur4.mapName());
        assertEquals(new Integer(1), ur4.key());
        assertNull(ur4.oldValue());
        assertEquals(newValue.map(s -> s.length()), ur4.newValue());
    }
}
