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
package org.onosproject.incubator.net.faultmanagement.alarm;

import com.google.common.testing.EqualsTester;
import static org.hamcrest.Matchers.containsString;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * This class tests the immutability, equality, and non-equality of {@link AlarmId}.
 */
public class AlarmIdTest {

    private static final long ID_A = 1L;
    private static final long ID_B = 2L;
    private static final long ID_Z = 987654321L;

    /**
     * Tests the immutability of {@link AlarmId}.
     */
    @Test
    public void intentIdFollowsGuidelineForImmutableObject() {
        assertThatClassIsImmutable(AlarmId.class);
    }

    /**
     * Tests equality of {@link AlarmId}.
     */
    @Test
    public void testEquality() {
        final AlarmId id1 = AlarmId.alarmId(ID_A);
        final AlarmId id2 = AlarmId.alarmId(ID_A);

        assertThat(id1, is(id2));
    }

    /**
     * Tests non-equality of {@link AlarmId}.
     */
    @Test
    public void testNonEquality() {
        final AlarmId id1 = AlarmId.alarmId(ID_A);
        final AlarmId id2 = AlarmId.alarmId(ID_B);

        assertThat(id1, is(not(id2)));
    }

    @Test
    public void valueOf() {
        final AlarmId id = AlarmId.alarmId(0xdeadbeefL);
        assertEquals("incorrect valueOf", id, AlarmId.alarmId(0xdeadbeefL));
    }

    /**
     * Tests the equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        final AlarmId id1 = AlarmId.alarmId(11111L);
        final AlarmId sameAsId1 = AlarmId.alarmId(11111L);
        final AlarmId id2 = AlarmId.alarmId(22222L);

        new EqualsTester()
                .addEqualityGroup(id1, sameAsId1)
                .addEqualityGroup(id2)
                .testEquals();
    }

    /**
     * Tests construction of an AlarmId object.
     */
    @Test
    public void testConstruction() {
        final AlarmId id1 = AlarmId.alarmId(ID_Z);
        assertEquals(id1.fingerprint(), ID_Z);

        // No default constructor so no need to test it !
        assertEquals(0L, AlarmId.NONE.fingerprint());
        try {
            final AlarmId bad = AlarmId.alarmId(0L);
            fail("0 is a Reserved value but we created " + bad);
        } catch (IllegalArgumentException ex) {
            assertThat(ex.getMessage(),
                    containsString("id must be non-zero"));
        }

    }
}
