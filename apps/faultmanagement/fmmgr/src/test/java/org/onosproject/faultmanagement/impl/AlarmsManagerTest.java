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
package org.onosproject.faultmanagement.impl;

import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmEntityId;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmId;
import org.onosproject.incubator.net.faultmanagement.alarm.DefaultAlarm;
import org.onosproject.net.DeviceId;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.incubator.net.faultmanagement.alarm.Alarm.SeverityLevel.CLEARED;
import static org.onosproject.incubator.net.faultmanagement.alarm.Alarm.SeverityLevel.CRITICAL;

/**
 * Alarm manager test suite.
 */
public class AlarmsManagerTest {

    private static final DeviceId DEVICE_ID = DeviceId.deviceId("foo:bar");
    private static final DefaultAlarm ALARM_A = new DefaultAlarm.Builder(
            DEVICE_ID, "aaa", Alarm.SeverityLevel.CRITICAL, 0).build();

    private static final DefaultAlarm ALARM_A_WITHSRC = new DefaultAlarm.Builder(
            ALARM_A).forSource(AlarmEntityId.alarmEntityId("port:foo")).build();

    private static final DefaultAlarm ALARM_B = new DefaultAlarm.Builder(
            DEVICE_ID, "bbb", Alarm.SeverityLevel.CRITICAL, 0).build();

    private AlarmsManager am;

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        am = new AlarmsManager();
    }

    @Test
    public void deactivate() throws Exception {
        am.updateAlarms(DEVICE_ID, ImmutableSet.of(ALARM_B, ALARM_A));
        verifyGettingSetsOfAlarms(am, 2, 2);
        am.deactivate(null);
        assertEquals("Alarms should be purged", 0, am.alarms.size());
    }

    @Test
    public void testGettersWhenNoAlarms() {

        assertTrue("No alarms should be present", am.getAlarms().isEmpty());
        assertTrue("No active alarms should be present", am.getActiveAlarms().isEmpty());
        assertTrue("The map should be empty per unknown device",
                   am.getAlarmCounts(DeviceId.NONE).keySet().isEmpty());
        assertTrue("The counts should be empty", am.getAlarmCounts().keySet().isEmpty());

        assertEquals("Incorrect number of alarms for unknown device",
                     0, am.getAlarms(DeviceId.NONE).size());
        assertEquals("Incorrect number of major alarms for unknown device",
                     0, am.getAlarms(Alarm.SeverityLevel.MAJOR).size());

        exception.expect(NullPointerException.class);
        am.getAlarm(null);

        exception.expect(ItemNotFoundException.class);
        am.getAlarm(AlarmId.alarmId(1));
    }

    @Test
    public void testAlarmUpdates() {

        assertTrue("No alarms should be present", am.getAlarms().isEmpty());
        am.updateAlarms(DEVICE_ID, ImmutableSet.of());
        assertTrue("No alarms should be present", am.getAlarms().isEmpty());
        Map<Alarm.SeverityLevel, Long> zeroAlarms = new CountsMapBuilder().create();
        assertEquals("No alarms count should be present", zeroAlarms, am.getAlarmCounts());
        assertEquals("No alarms count should be present", zeroAlarms, am.getAlarmCounts(DEVICE_ID));

        am.updateAlarms(DEVICE_ID, ImmutableSet.of(ALARM_B, ALARM_A));
        verifyGettingSetsOfAlarms(am, 2, 2);
        Map<Alarm.SeverityLevel, Long> critical2 = new CountsMapBuilder().with(CRITICAL, 2L).create();
        assertEquals("A critical should be present", critical2, am.getAlarmCounts());
        assertEquals("A critical should be present", critical2, am.getAlarmCounts(DEVICE_ID));

        am.updateAlarms(DEVICE_ID, ImmutableSet.of(ALARM_A));
        verifyGettingSetsOfAlarms(am, 2, 1);
        Map<Alarm.SeverityLevel, Long> critical1cleared1 =
                new CountsMapBuilder().with(CRITICAL, 1L).with(CLEARED, 1L).create();
        assertEquals("A critical should be present and cleared", critical1cleared1,
                     am.getAlarmCounts());
        assertEquals("A critical should be present and cleared", critical1cleared1,
                     am.getAlarmCounts(DEVICE_ID));

        // No change map when same alarms sent
        am.updateAlarms(DEVICE_ID, ImmutableSet.of(ALARM_A));
        verifyGettingSetsOfAlarms(am, 2, 1);
        assertEquals("Map should not be changed for same alarm", critical1cleared1,
                     am.getAlarmCounts());
        assertEquals("Map should not be changed for same alarm", critical1cleared1,
                     am.getAlarmCounts(DEVICE_ID));

        am.updateAlarms(DEVICE_ID, ImmutableSet.of(ALARM_A, ALARM_A_WITHSRC));
        verifyGettingSetsOfAlarms(am, 3, 2);
        Map<Alarm.SeverityLevel, Long> critical2cleared1 =
                new CountsMapBuilder().with(CRITICAL, 2L).with(CLEARED, 1L).create();
        assertEquals("A critical should be present", critical2cleared1, am.getAlarmCounts());
        assertEquals("A critical should be present", critical2cleared1, am.getAlarmCounts(DEVICE_ID));

        am.updateAlarms(DEVICE_ID, ImmutableSet.of());
        verifyGettingSetsOfAlarms(am, 3, 0);
        assertEquals(new CountsMapBuilder().with(CLEARED, 3L).create(), am.getAlarmCounts(DEVICE_ID));

        assertEquals("The counts should be empty for unknown devices", zeroAlarms,
                     am.getAlarmCounts(DeviceId.NONE));
        assertEquals("The counts should be empty for unknown devices", zeroAlarms,
                     am.getAlarmCounts(DeviceId.deviceId("junk:junk")));

    }

    private void verifyGettingSetsOfAlarms(AlarmsManager am, int expectedTotal, int expectedActive) {
        assertEquals("Incorrect total alarms", expectedTotal, am.getAlarms().size());
        assertEquals("Incorrect active alarms count", expectedActive, am.getActiveAlarms().size());
    }

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
