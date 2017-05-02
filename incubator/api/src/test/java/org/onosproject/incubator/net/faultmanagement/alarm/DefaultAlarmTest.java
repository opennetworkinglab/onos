/*
 * Copyright 2015-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.incubator.net.faultmanagement.alarm;

import org.junit.Test;
import org.onosproject.net.DeviceId;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

public class DefaultAlarmTest {

    private static final AlarmEntityId ALARM_ENTITY_ID = AlarmEntityId.alarmEntityId("port:bar");
    private static final DeviceId DEVICE_ID = DeviceId.deviceId("foo:bar");
    private static final String UNIQUE_ID_1 = "unique_id_1";
    private static final AlarmId ALARM_ID = AlarmId.alarmId(DEVICE_ID, UNIQUE_ID_1);
    private static final String UNIQUE_ID_2 = "unique_id_1";
    private static final AlarmId ALARM_ID_2 = AlarmId.alarmId(DEVICE_ID, UNIQUE_ID_2);

    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(DefaultAlarm.class);
    }

    /**
     * Checks the construction of a DefaultAlarm object.
     */
    @Test
    public void testConstruction() {
        final DefaultAlarm a = generate();
        assertThat(a, is(notNullValue()));
        final DefaultAlarm b = new DefaultAlarm.Builder(a).build();
        assertEquals(a, b);
    }

    @Test
    public void testEquals() {
        final DefaultAlarm a = new DefaultAlarm.Builder(ALARM_ID_2,
                DeviceId.NONE, "desc", Alarm.SeverityLevel.MINOR, 3).build();
        final DefaultAlarm b = new DefaultAlarm.Builder(ALARM_ID,
                DeviceId.NONE, "desc", Alarm.SeverityLevel.MINOR, a.timeRaised() + 1)
                .withTimeUpdated(a.timeUpdated() + 1).build();
        assertEquals("id or timeRaised or timeUpdated may differ", a, b);

        assertNotEquals(a, new DefaultAlarm.Builder(a).withAcknowledged(!a.acknowledged()).build());
        assertNotEquals(a, new DefaultAlarm.Builder(a).withManuallyClearable(!a.manuallyClearable()).build());
        assertNotEquals(a, new DefaultAlarm.Builder(a).withServiceAffecting(!a.serviceAffecting()).build());
        assertNotEquals(a, new DefaultAlarm.Builder(a).withAssignedUser("Changed" + a.assignedUser()).build());

    }

    @Test
    public void testClear() {
        final DefaultAlarm active = generate();
        final DefaultAlarm cleared = new DefaultAlarm.Builder(active).clear().build();
        assertNotEquals(active, cleared);
        assertThat(cleared.timeRaised(), is(active.timeRaised()));
        assertThat(cleared.severity(), is(Alarm.SeverityLevel.CLEARED));
        assertThat(cleared.timeUpdated(), greaterThan(active.timeUpdated()));
        assertNotNull(cleared.timeCleared());

    }

    @Test
    public void testId() {
        final DefaultAlarm a = generate();
        final DefaultAlarm b = new DefaultAlarm.Builder(a).build();

        assertEquals("id ignored in equals", a, b);
        assertEquals(ALARM_ID, a.id());
        assertEquals(ALARM_ID, b.id());
        assertEquals(ALARM_ENTITY_ID, b.source());

    }

    private static DefaultAlarm generate() {
        return new DefaultAlarm.Builder(ALARM_ID,
                DeviceId.NONE, "desc", Alarm.SeverityLevel.MINOR, 3).forSource(ALARM_ENTITY_ID).build();
    }
}
