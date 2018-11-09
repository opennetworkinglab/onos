/*
 * Copyright 2016-present Open Networking Foundation
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
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.onlab.junit.TestTools;
import org.onlab.junit.TestUtils;
import org.onlab.util.ItemNotFoundException;
import org.onosproject.common.event.impl.TestEventDispatcher;
import org.onosproject.event.Event;
import org.onosproject.alarm.Alarm;
import org.onosproject.alarm.AlarmEntityId;
import org.onosproject.alarm.AlarmEvent;
import org.onosproject.alarm.AlarmId;
import org.onosproject.alarm.AlarmListener;
import org.onosproject.alarm.AlarmProvider;
import org.onosproject.alarm.AlarmProviderRegistry;
import org.onosproject.alarm.AlarmProviderService;
import org.onosproject.alarm.DefaultAlarm;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.provider.AbstractProvider;
import org.onosproject.store.service.TestStorageService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.TestCase.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.alarm.Alarm.SeverityLevel.CLEARED;
import static org.onosproject.alarm.Alarm.SeverityLevel.CRITICAL;
import static org.onosproject.net.NetTestTools.PID;

/**
 * Alarm manager test suite.
 */
public class AlarmManagerTest {

    private static final DeviceId DEVICE_ID = DeviceId.deviceId("foo:bar");

    private static final String UNIQUE_ID_1 = "unique_id_1";
    private static final String UNIQUE_ID_2 = "unique_id_2";
    private static final AlarmId A_ID = AlarmId.alarmId(DEVICE_ID, UNIQUE_ID_1);
    private static final AlarmId B_ID = AlarmId.alarmId(DEVICE_ID, UNIQUE_ID_2);
    private static final DefaultAlarm ALARM_A = new DefaultAlarm.Builder(A_ID,
            DEVICE_ID, "aaa", Alarm.SeverityLevel.CRITICAL, 0).build();

    private static final DefaultAlarm ALARM_A_CLEARED = new DefaultAlarm.Builder(ALARM_A)
            .clear().build();

    private static final DefaultAlarm ALARM_A_WITHSRC = new DefaultAlarm.Builder(
            ALARM_A).forSource(AlarmEntityId.alarmEntityId("port:foo")).build();

    private static final DefaultAlarm ALARM_B = new DefaultAlarm.Builder(B_ID,
            DEVICE_ID, "bbb", Alarm.SeverityLevel.CRITICAL, 0).build();


    private AlarmManager manager;
    private DistributedAlarmStore alarmStore;
    private AlarmProviderService providerService;
    private TestProvider provider;
    protected AlarmProviderRegistry registry;
    protected TestListener listener = new TestListener();
    private final MastershipService mastershipService = new MockMastershipService();
    protected final MockDeviceService deviceService = new MockDeviceService();

    private final Device device = new MockDevice(DEVICE_ID);

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        alarmStore = new DistributedAlarmStore();
        TestUtils.setField(alarmStore, "storageService", new TestStorageService());
        alarmStore.activate();
        manager = new AlarmManager();
        registry = manager;
        manager.addListener(listener);
        NetTestTools.injectEventDispatcher(manager, new TestEventDispatcher());
        manager.deviceService = deviceService;
        manager.mastershipService = mastershipService;
        manager.store = alarmStore;
        manager.activate();
        provider = new TestProvider();
        providerService = registry.register(provider);
    }

    @Test
    public void deactivate() throws Exception {
        providerService.updateAlarmList(DEVICE_ID, ImmutableSet.of(ALARM_B, ALARM_A));
        verifyGettingSetsOfAlarms(manager, 2, 2);
        alarmStore.deactivate();
        manager.removeListener(listener);
        manager.deactivate();
        NetTestTools.injectEventDispatcher(manager, null);
        assertFalse("Store should not have delegate", alarmStore.hasDelegate());
    }

    @Test
    public void testGettersWhenNoAlarms() {

        assertTrue("No alarms should be present", manager.getAlarms().isEmpty());
        assertTrue("No active alarms should be present", manager.getActiveAlarms().isEmpty());
        assertTrue("The map should be empty per unknown device",
                   manager.getAlarmCounts(DeviceId.NONE).keySet().isEmpty());
        assertTrue("The counts should be empty", manager.getAlarmCounts().keySet().isEmpty());

        assertEquals("Incorrect number of alarms for unknown device",
                     0, manager.getAlarms(DeviceId.NONE).size());
        assertEquals("Incorrect number of major alarms for unknown device",
                     0, manager.getAlarms(Alarm.SeverityLevel.MAJOR).size());

        exception.expect(NullPointerException.class);
        manager.getAlarm(null);

        exception.expect(ItemNotFoundException.class);
        manager.getAlarm(AlarmId.alarmId(DEVICE_ID, "unique_3"));
    }

    @Test
    public void testAlarmUpdates() throws InterruptedException {

        assertTrue("No alarms should be present", manager.getAlarms().isEmpty());
        providerService.updateAlarmList(DEVICE_ID, ImmutableSet.of());
        assertTrue("No alarms should be present", manager.getAlarms().isEmpty());
        Map<Alarm.SeverityLevel, Long> zeroAlarms = new CountsMapBuilder().create();
        assertEquals("No alarms count should be present", zeroAlarms, manager.getAlarmCounts());
        assertEquals("No alarms count should be present", zeroAlarms, manager.getAlarmCounts(DEVICE_ID));

        providerService.updateAlarmList(DEVICE_ID, ImmutableSet.of(ALARM_B, ALARM_A));
        verifyGettingSetsOfAlarms(manager, 2, 2);
        validateEvents(AlarmEvent.Type.CREATED, AlarmEvent.Type.CREATED);
        Map<Alarm.SeverityLevel, Long> critical2 = new CountsMapBuilder().with(CRITICAL, 2L).create();
        assertEquals("A critical should be present", critical2, manager.getAlarmCounts());
        assertEquals("A critical should be present", critical2, manager.getAlarmCounts(DEVICE_ID));

        Alarm updated = manager.updateBookkeepingFields(ALARM_A.id(), true, false, null);
//        providerService.updateAlarmList(DEVICE_ID, ImmutableSet.of(ALARM_A));
        verifyGettingSetsOfAlarms(manager, 2, 1);
        validateEvents(AlarmEvent.Type.UPDATED);
        Map<Alarm.SeverityLevel, Long> critical1cleared1 =
                new CountsMapBuilder().with(CRITICAL, 1L).with(CLEARED, 1L).create();
        assertEquals("A critical should be present and cleared", critical1cleared1,
                     manager.getAlarmCounts());
        assertEquals("A critical should be present and cleared", critical1cleared1,
                     manager.getAlarmCounts(DEVICE_ID));

        // No change map when same alarms sent
        providerService.updateAlarmList(DEVICE_ID, ImmutableSet.of(updated));
        verifyGettingSetsOfAlarms(manager, 2, 1);
        validateEvents();
        assertEquals("Map should not be changed for same alarm", critical1cleared1,
                     manager.getAlarmCounts());
        assertEquals("Map should not be changed for same alarm", critical1cleared1,
                     manager.getAlarmCounts(DEVICE_ID));

        providerService.updateAlarmList(DEVICE_ID, ImmutableSet.of(updated, ALARM_A_WITHSRC));
        verifyGettingSetsOfAlarms(manager, 2, 2);
        validateEvents(AlarmEvent.Type.UPDATED);
        Map<Alarm.SeverityLevel, Long> critical2cleared1 =
                new CountsMapBuilder().with(CRITICAL, 2L).create();
        assertEquals("A critical should be present", critical2cleared1, manager.getAlarmCounts());
        assertEquals("A critical should be present", critical2cleared1, manager.getAlarmCounts(DEVICE_ID));

        providerService.updateAlarmList(DEVICE_ID, ImmutableSet.of());
        verifyGettingSetsOfAlarms(manager, 2, 2);
        validateEvents();
        assertEquals(new CountsMapBuilder().with(CRITICAL, 2L).create(),
                     manager.getAlarmCounts(DEVICE_ID));

        assertEquals("The counts should be empty for unknown devices", zeroAlarms,
                     manager.getAlarmCounts(DeviceId.NONE));
        assertEquals("The counts should be empty for unknown devices", zeroAlarms,
                     manager.getAlarmCounts(DeviceId.deviceId("junk:junk")));

    }

    @Test
    public void testRemoveWhenDeviceRemoved() {
        providerService.updateAlarmList(DEVICE_ID, ImmutableSet.of(ALARM_B, ALARM_A));
        verifyGettingSetsOfAlarms(manager, 2, 2);
        validateEvents(AlarmEvent.Type.CREATED, AlarmEvent.Type.CREATED);
        Map<Alarm.SeverityLevel, Long> critical2 = new CountsMapBuilder().with(CRITICAL, 2L).create();
        assertEquals("A critical should be present", critical2, manager.getAlarmCounts());
        assertEquals("A critical should be present", critical2, manager.getAlarmCounts(DEVICE_ID));
        deviceService.deviceListener.event(new DeviceEvent(DeviceEvent.Type.DEVICE_REMOVED, device));
        Map<Alarm.SeverityLevel, Long> zeroAlarms = new CountsMapBuilder().create();
        assertEquals("The counts should be empty for removed device", zeroAlarms,
                manager.getAlarmCounts(DEVICE_ID));

    }

    private void verifyGettingSetsOfAlarms(AlarmManager am, int expectedTotal, int expectedActive) {
        assertEquals("Incorrect total alarms", expectedTotal, am.getAlarms().size());
        assertEquals("Incorrect active alarms count", expectedActive, am.getActiveAlarms().size());
    }

    /**
     * Method to validate that actual versus expected device key events were
     * received correctly.
     *
     * @param types expected device key events.
     */
    private void validateEvents(Enum... types) {
        TestTools.assertAfter(100, () -> {
            int i = 0;
            assertEquals("wrong events received", types.length, listener.events.size());
            for (Event event : listener.events) {
                assertEquals("incorrect event type", types[i], event.type());
                i++;
            }
            listener.events.clear();
        });
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


    private class MockDeviceService extends DeviceServiceAdapter {
        DeviceListener deviceListener = null;

        @Override
        public void addListener(DeviceListener listener) {
            this.deviceListener = listener;
        }

        @Override
        public void removeListener(DeviceListener listener) {
            this.deviceListener = null;
        }
    }


    private class TestProvider extends AbstractProvider implements AlarmProvider {
        private DeviceId deviceReceived;
        private MastershipRole roleReceived;

        public TestProvider() {
            super(PID);
        }

        @Override
        public void triggerProbe(DeviceId deviceId) {
        }
    }

    private class MockMastershipService extends MastershipServiceAdapter {
        int test = 0;
        @Override
        public boolean isLocalMaster(DeviceId deviceId) {
            return true;
        }
    }

    /**
     * Test listener class to receive alarm events.
     */
    private static class TestListener implements AlarmListener {

        protected List<AlarmEvent> events = Lists.newArrayList();

        @Override
        public void event(AlarmEvent event) {
            events.add(event);
        }

    }

    private class MockDevice extends DefaultDevice {

        MockDevice(DeviceId id) {
            super(null, id, null, null, null, null, null,
                    null, DefaultAnnotations.EMPTY);
        }

    }
}
