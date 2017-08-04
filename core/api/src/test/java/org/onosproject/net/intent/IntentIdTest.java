/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.intent;

import org.junit.Test;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * This class tests the immutability, equality, and non-equality of
 * {@link IntentId}.
 */
public class IntentIdTest {
    /**
     * Tests the immutability of {@link IntentId}.
     */
    @Test
    public void intentIdFollowsGuidelineForImmutableObject() {
        assertThatClassIsImmutable(IntentId.class);
    }

    /**
     * Tests equality of {@link IntentId}.
     */
    @Test
    public void testEquality() {
        IntentId id1 = new IntentId(1L);
        IntentId id2 = new IntentId(1L);

        assertThat(id1, is(id2));
    }

    /**
     * Tests non-equality of {@link IntentId}.
     */
    @Test
    public void testNonEquality() {
        IntentId id1 = new IntentId(1L);
        IntentId id2 = new IntentId(2L);

        assertThat(id1, is(not(id2)));
    }

    @Test
    public void valueOf() {
        IntentId id = new IntentId(0xdeadbeefL);
        assertEquals("incorrect valueOf", id, IntentId.valueOf(0xdeadbeefL));
    }

    /**
     * Tests the equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        final IntentId id1 = new IntentId(11111L);
        final IntentId sameAsId1 = new IntentId(11111L);
        final IntentId id2 = new IntentId(22222L);

        new EqualsTester()
                .addEqualityGroup(id1, sameAsId1)
                .addEqualityGroup(id2)
                .testEquals();
    }

    /**
     * Tests construction of an IntentId object.
     */
    @Test
    public void testConstruction() {
        final IntentId id1 = new IntentId(987654321L);
        assertEquals(id1.fingerprint(), 987654321L);

        final IntentId emptyId = new IntentId();
        assertEquals(emptyId.fingerprint(), 0L);
    }
}
