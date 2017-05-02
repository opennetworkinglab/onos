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
import org.junit.Test;
import org.onosproject.net.DeviceId;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * This class tests the immutability, equality, and non-equality of {@link AlarmId}.
 */
public class AlarmIdTest {

    private static final DeviceId DEVICE_ID = DeviceId.deviceId("foo:bar");
    private static final String UNIQUE_ID_1 = "unique_id_1";
    private static final AlarmId ID_A = AlarmId.alarmId(DEVICE_ID, UNIQUE_ID_1);

    private static final String UNIQUE_ID_2 = "unique_id_2";

    private static final String UNIQUE_ID_3 = "unique_id_3";
    private static final AlarmId ID_Z = AlarmId.alarmId(DEVICE_ID, UNIQUE_ID_3);

    private static final String ID_STRING = "foo:bar:unique_id_3";

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
        final AlarmId id1 = AlarmId.alarmId(DEVICE_ID, UNIQUE_ID_1);
        final AlarmId id2 = AlarmId.alarmId(DEVICE_ID, UNIQUE_ID_1);

        assertThat(id1, is(id2));
    }

    /**
     * Tests non-equality of {@link AlarmId}.
     */
    @Test
    public void testNonEquality() {
        final AlarmId id1 = AlarmId.alarmId(DEVICE_ID, UNIQUE_ID_1);
        final AlarmId id2 = AlarmId.alarmId(DEVICE_ID, UNIQUE_ID_2);

        assertThat(id1, is(not(id2)));
    }

    @Test
    public void valueOf() {
        final AlarmId id = AlarmId.alarmId(DEVICE_ID, UNIQUE_ID_1);
        assertEquals("incorrect valueOf", id, ID_A);
    }

    /**
     * Tests the equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        final AlarmId id1 = AlarmId.alarmId(DEVICE_ID, UNIQUE_ID_1);
        final AlarmId sameAsId1 = AlarmId.alarmId(DEVICE_ID, UNIQUE_ID_1);
        final AlarmId id2 = AlarmId.alarmId(DEVICE_ID, UNIQUE_ID_2);

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
        final AlarmId id1 = AlarmId.alarmId(DEVICE_ID, UNIQUE_ID_3);
        assertEquals(id1.toString(), ID_Z.toString());

        final AlarmId idString = AlarmId.alarmId(ID_STRING);
        assertEquals(id1, idString);

    }
}
