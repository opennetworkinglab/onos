/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.faultmanagement.impl;

import com.google.common.collect.Sets;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.net.DeviceId;
import static org.hamcrest.Matchers.containsString;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import static org.onosproject.incubator.net.faultmanagement.alarm.Alarm.SeverityLevel.*;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmEntityId;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmId;
import org.onosproject.incubator.net.faultmanagement.alarm.DefaultAlarm;

public class AlarmsManagerTest {

    @Test
    public void testGettersWhenNoAlarms() {
        AlarmsManager am = new AlarmsManager();
        assertTrue("No alarms", am.getAlarms().isEmpty());
        assertTrue("No active alarms", am.getActiveAlarms().isEmpty());
        assertTrue("No alarms gives empty map per unknown device", am.getAlarmCounts(DeviceId.NONE).keySet().isEmpty());
        assertTrue("No alarms gives empty map", am.getAlarmCounts().keySet().isEmpty());

        assertEquals("Zero alarms for that device", 0, am.getAlarms(DeviceId.NONE).size());
        assertEquals("Zero major alarms", 0, am.getAlarms(Alarm.SeverityLevel.MAJOR).size());

        try {
            assertEquals("no alarms", 0, am.getAlarm(null));
        } catch (NullPointerException ex) {
            assertThat(ex.getMessage(),
                    containsString("cannot be null"));
        }

        try {
            assertEquals("no alarms", 0, am.getAlarm(AlarmId.alarmId(1)));
        } catch (ItemNotFoundException ex) {
            assertThat(ex.getMessage(),
                    containsString("not found"));
        }
    }

    @Test
    public void testAlarmUpdates() {
        AlarmsManager am = new AlarmsManager();
        assertTrue("no alarms", am.getAlarms().isEmpty());

        am.updateAlarms(new HashSet<>(), DEVICE_ID);
        assertTrue("still no alarms", am.getAlarms().isEmpty());
        Map<Alarm.SeverityLevel, Long> zeroAlarms = new CountsMapBuilder().create();
        assertEquals(zeroAlarms, am.getAlarmCounts());
        assertEquals(zeroAlarms, am.getAlarmCounts(DEVICE_ID));

        am.updateAlarms(Sets.newHashSet(ALARM_B, ALARM_A), DEVICE_ID);
        verifyGettingSetsOfAlarms(am, 2, 2);
        Map<Alarm.SeverityLevel, Long> critical2 = new CountsMapBuilder().with(CRITICAL, 2L).create();
        assertEquals(critical2, am.getAlarmCounts());
        assertEquals(critical2, am.getAlarmCounts(DEVICE_ID));

        am.updateAlarms(Sets.newHashSet(ALARM_A), DEVICE_ID);
        verifyGettingSetsOfAlarms(am, 2, 1);
        Map<Alarm.SeverityLevel, Long> critical1cleared1 =
                new CountsMapBuilder().with(CRITICAL, 1L).with(CLEARED, 1L).create();
        assertEquals(critical1cleared1, am.getAlarmCounts());
        assertEquals(critical1cleared1, am.getAlarmCounts(DEVICE_ID));

        // No change map when same alarms sent
        am.updateAlarms(Sets.newHashSet(ALARM_A), DEVICE_ID);
        verifyGettingSetsOfAlarms(am, 2, 1);
        assertEquals(critical1cleared1, am.getAlarmCounts());
        assertEquals(critical1cleared1, am.getAlarmCounts(DEVICE_ID));

        am.updateAlarms(Sets.newHashSet(ALARM_A, ALARM_A_WITHSRC), DEVICE_ID);
        verifyGettingSetsOfAlarms(am, 3, 2);
        Map<Alarm.SeverityLevel, Long> critical2cleared1 =
                new CountsMapBuilder().with(CRITICAL, 2L).with(CLEARED, 1L).create();
        assertEquals(critical2cleared1, am.getAlarmCounts());
        assertEquals(critical2cleared1, am.getAlarmCounts(DEVICE_ID));

        am.updateAlarms(Sets.newHashSet(), DEVICE_ID);
        verifyGettingSetsOfAlarms(am, 3, 0);
        assertEquals(new CountsMapBuilder().with(CLEARED, 3L).create(), am.getAlarmCounts(DEVICE_ID));

        assertEquals("No alarms for unknown devices", zeroAlarms, am.getAlarmCounts(DeviceId.NONE));
        assertEquals("No alarms for unknown devices", zeroAlarms, am.getAlarmCounts(DeviceId.deviceId("junk:junk")));

    }

    private void verifyGettingSetsOfAlarms(AlarmsManager am, int expectedTotal, int expectedActive) {
        assertEquals("Wrong total", expectedTotal, am.getAlarms().size());
        assertEquals("Wrong active count", expectedActive, am.getActiveAlarms().size());
    }
    private static final DeviceId DEVICE_ID = DeviceId.deviceId("foo:bar");
    private static final DefaultAlarm ALARM_A = new DefaultAlarm.Builder(
            DEVICE_ID, "aaa", Alarm.SeverityLevel.CRITICAL, 0).build();

    private static final DefaultAlarm ALARM_A_WITHSRC = new DefaultAlarm.Builder(
            ALARM_A).forSource(AlarmEntityId.alarmEntityId("port:foo")).build();

    private static final DefaultAlarm ALARM_B = new DefaultAlarm.Builder(
            DEVICE_ID, "bbb", Alarm.SeverityLevel.CRITICAL, 0).build();

    private static class CountsMapBuilder {

        private final Map<Alarm.SeverityLevel, Long> map = new HashMap<>();

        public CountsMapBuilder with(Alarm.SeverityLevel sev, Long count) {
            map.put(sev, count);
            return this;
        }

        public Map<Alarm.SeverityLevel, Long> create() {
            return Collections.unmodifiableMap(map);
        }
    }

}
