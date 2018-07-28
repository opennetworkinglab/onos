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
package org.onosproject.store.atomix.primitives.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.onosproject.store.LogicalTimestamp;
import org.onosproject.store.Timestamp;
import org.onosproject.store.atomix.primitives.impl.MapValue.Digest;

/**
 * Unit tests for MapValue.
 */
public class MapValueTest {

    @Test
    public void testConstruction() {
        Timestamp ts = new LogicalTimestamp(10);
        MapValue<String> mv = new MapValue<>("foo", ts);
        assertEquals("foo", mv.get());
        assertEquals(ts, mv.timestamp());
        assertTrue(mv.isAlive());
    }

    @Test
    public void testDigest() {
        Timestamp ts = new LogicalTimestamp(10);
        MapValue<String> mv = new MapValue<>("foo", ts);
        Digest actual = mv.digest();
        Digest expected = new MapValue.Digest(ts, false);
        assertEquals(actual, expected);
    }

    @SuppressWarnings("SelfComparison")
    @Test
    public void testComparison() {
        Timestamp ts1 = new LogicalTimestamp(9);
        Timestamp ts2 = new LogicalTimestamp(10);
        Timestamp ts3 = new LogicalTimestamp(11);
        MapValue<String> mv1 = new MapValue<>("foo", ts1);
        MapValue<String> mv2 = new MapValue<>("foo", ts2);
        MapValue<String> mv3 = new MapValue<>("foo", ts3);
        assertTrue(mv2.isNewerThan(mv1));
        assertFalse(mv1.isNewerThan(mv3));

        assertTrue(mv3.isNewerThan(ts2));
        assertFalse(mv1.isNewerThan(ts2));

        assertTrue(mv1.compareTo(mv2) < 0);
        assertTrue(mv1.compareTo(mv1) == 0);
        assertTrue(mv3.compareTo(mv2) > 0);
    }

    @Test
    public void testTombstone() {
        Timestamp ts1 = new LogicalTimestamp(9);
        MapValue<String> mv = MapValue.tombstone(ts1);
        assertTrue(mv.isTombstone());
        assertFalse(mv.isAlive());
        assertNull(mv.get());
        assertEquals(ts1, mv.timestamp());
    }
}
