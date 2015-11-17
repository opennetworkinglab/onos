/*
 * Copyright 2014 Open Networking Laboratory
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
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * This class tests the immutability, equality, and non-equality of
 * {@link AlarmId}.
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
        final AlarmId id1 = new AlarmId(ID_A);
        final AlarmId id2 = new AlarmId(ID_A);

        assertThat(id1, is(id2));
    }


    /**
     * Tests non-equality of {@link AlarmId}.
     */
    @Test
    public void testNonEquality() {
        final AlarmId id1 = new AlarmId(ID_A);
        final AlarmId id2 = new AlarmId(ID_B);

        assertThat(id1, is(not(id2)));
    }

    @Test
    public void valueOf() {
        final AlarmId id = new AlarmId(0xdeadbeefL);
        assertEquals("incorrect valueOf", id, AlarmId.valueOf(0xdeadbeefL));
    }

    /**
     * Tests the equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        final AlarmId id1 = new AlarmId(11111L);
        final AlarmId sameAsId1 = new AlarmId(11111L);
        final AlarmId id2 = new AlarmId(22222L);

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
        final AlarmId id1 = new AlarmId(ID_Z);
        assertEquals(id1.fingerprint(), ID_Z);

        // No default constructor so no need to test it !
    }
}
