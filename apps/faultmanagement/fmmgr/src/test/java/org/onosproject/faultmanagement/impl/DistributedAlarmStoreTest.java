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

package org.onosproject.faultmanagement.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmId;
import org.onosproject.incubator.net.faultmanagement.alarm.DefaultAlarm;
import org.onosproject.net.DeviceId;
import org.onosproject.store.service.TestStorageService;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Distributed Alarm store test suite.
 */
public class DistributedAlarmStoreTest {
    private DistributedAlarmStore alarmStore;
    private static final DeviceId DEVICE_ID = DeviceId.deviceId("foo:bar");
    private static final String UNIQUE_ID_1 = "unique_id_1";
    private static final AlarmId A_ID = AlarmId.alarmId(DEVICE_ID, UNIQUE_ID_1);
    private static final DefaultAlarm ALARM_A = new DefaultAlarm.Builder(A_ID,
            DEVICE_ID, "aaa", Alarm.SeverityLevel.CRITICAL, 0).build();

    /**
     * Sets up the device key store and the storage service test harness.
     */
    @Before
    public void setUp() {
        alarmStore = new DistributedAlarmStore();
        alarmStore.storageService = new TestStorageService();
        alarmStore.setDelegate(event -> {
        });
        alarmStore.activate();
    }

    /**
     * Tears down the device key store.
     */
    @After
    public void tearDown() {
        alarmStore.deactivate();
    }

    /**
     * Tests adding, removing and getting.
     */
    @Test
    public void basics() {
        alarmStore.createOrUpdateAlarm(ALARM_A);
        assertTrue("There should be one alarm in the set.",
                   alarmStore.getAlarms().contains(ALARM_A));
        assertTrue("The same alarm should be returned.",
                   alarmStore.getAlarms(DEVICE_ID).contains(ALARM_A));
        assertTrue("The alarm should be the same",
                   alarmStore.getAlarm(ALARM_A.id()).equals(ALARM_A));
        alarmStore.removeAlarm(ALARM_A.id());
        assertFalse("There should be no alarm in the set.",
                    alarmStore.getAlarms().contains(ALARM_A));
    }

}
