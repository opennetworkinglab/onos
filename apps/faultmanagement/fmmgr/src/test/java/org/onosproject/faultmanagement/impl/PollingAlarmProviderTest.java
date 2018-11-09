/*
 * Copyright 2016-present Open Networking Foundation
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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ComponentContextAdapter;
import org.onlab.packet.ChassisId;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.NodeId;
import org.onosproject.alarm.Alarm;
import org.onosproject.alarm.AlarmConsumer;
import org.onosproject.alarm.AlarmId;
import org.onosproject.alarm.AlarmProvider;
import org.onosproject.alarm.AlarmProviderRegistry;
import org.onosproject.alarm.AlarmProviderRegistryAdapter;
import org.onosproject.alarm.AlarmProviderService;
import org.onosproject.alarm.DefaultAlarm;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipInfo;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.AbstractProjectableModel;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.Behaviour;
import org.onosproject.net.driver.Driver;
import org.onosproject.net.driver.DriverAdapter;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.osgi.service.component.ComponentContext;

import java.io.IOException;
import java.util.Collection;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.*;
import static org.onlab.junit.TestTools.assertAfter;

/**
 * Test for the polling alarm provider based on the driver subsystem.
 */
public class PollingAlarmProviderTest {

    private final DeviceService deviceService = new MockDeviceService();

    private final MastershipService mastershipService = new MockMastershipService();

    private final AlarmProviderRegistry providerRegistry = new MockDeviceProviderRegistry();

    private final AlarmProviderService alarmProviderService = new MockAlarmProviderService();

    private final ComponentConfigService cfgService = new ComponentConfigAdapter();

    private final ComponentContext context = new MockComponentContext();

    private static final DeviceId DEVICE_ID = DeviceId.deviceId("foo:1.1.1.1:1");

    private Device device = new MockDevice(ProviderId.NONE, DEVICE_ID, Device.Type.OTHER,
                                           "foo.inc", "0", "0", "0", null,
                                           DefaultAnnotations.builder().build());

    private final NodeId nodeId = NodeId.nodeId("fooNode");

    private final MastershipEvent mastershipEvent =
            new MastershipEvent(MastershipEvent.Type.MASTER_CHANGED, DEVICE_ID,
                                new MastershipInfo(1, Optional.of(nodeId), ImmutableMap.of()));

    private final DeviceEvent deviceEvent =
            new DeviceEvent(DeviceEvent.Type.DEVICE_AVAILABILITY_CHANGED, device);

    private static final String UNIQUE_ID_1 = "unique_id_1";
    private static final AlarmId A_ID = AlarmId.alarmId(DEVICE_ID, UNIQUE_ID_1);
    private static final DefaultAlarm ALARM = new DefaultAlarm.Builder(A_ID,
            DEVICE_ID, "aaa", Alarm.SeverityLevel.CRITICAL, 0).build();

    private final Driver driver = new MockDriver();

    private PollingAlarmProvider provider = new PollingAlarmProvider();
    private Set<DeviceListener> deviceListeners = new HashSet<>();
    private Set<MastershipListener> mastershipListeners = new HashSet<>();
    private HashMap<DeviceId, Collection<Alarm>> alarmStore = new HashMap<>();

    @Before
    public void setUp() {
        provider.providerRegistry = providerRegistry;
        provider.deviceService = deviceService;
        provider.mastershipService = mastershipService;
        provider.cfgService = cfgService;
        AbstractProjectableModel.setDriverService(null, new DriverServiceAdapter());
        provider.activate(context);
    }

    @Test
    public void activate() throws Exception {
        assertFalse("Provider should be registered", providerRegistry.getProviders().contains(provider.id()));
        assertEquals("Device listener should be added", 1, deviceListeners.size());
        assertEquals("Incorrect alarm provider service", alarmProviderService, provider.providerService);
        assertEquals("Mastership listener should be added", 1, mastershipListeners.size());
        assertEquals("Incorrect polling frequency", 1, provider.alarmPollFrequencySeconds);
        assertFalse("Executor should be running", provider.alarmsExecutor.isShutdown());
        provider.activate(null);
        assertEquals("Incorrect polling frequency, should be default", 60, provider.alarmPollFrequencySeconds);
    }

    @Test
    public void deactivate() throws Exception {
        provider.deactivate();
        assertEquals("Device listener should be removed", 0, deviceListeners.size());
        assertEquals("Mastership listener should be removed", 0, mastershipListeners.size());
        assertFalse("Provider should not be registered", providerRegistry.getProviders().contains(provider.id()));
        assertTrue(provider.alarmsExecutor.isShutdown());
        assertNull(provider.providerService);
    }

    @Test
    public void modified() throws Exception {
        provider.modified(null);
        assertEquals("Incorrect polling frequency", 1, provider.alarmPollFrequencySeconds);
        provider.activate(null);
        provider.modified(context);
        assertEquals("Incorrect polling frequency", 1, provider.alarmPollFrequencySeconds);
    }

    @Test
    public void alarmsPresent() throws IOException {
        assertAfter(1100, () -> {
            assertTrue("Alarms should be added", alarmStore.containsKey(DEVICE_ID));
            assertTrue("Alarms should be added", alarmStore.get(DEVICE_ID).contains(ALARM));
        });
    }

    @Test
    public void mastershipListenerEvent() throws Exception {
        assertTrue("Incorrect relevant event", provider.mastershipListener
                .isRelevant(mastershipEvent));
        provider.mastershipListener.event(mastershipEvent);
        assertAfter(1100, () -> {
            assertTrue("Alarms should be added", alarmStore.containsKey(DEVICE_ID));
        });
    }

    @Test
    public void deviceListenerEvent() throws Exception {
        assertTrue("Incorrect relevant event", provider.deviceListener
                .isRelevant(deviceEvent));
        provider.deviceListener.event(deviceEvent);
        assertAfter(1100, () -> {
            assertTrue("Alarms should be added", alarmStore.containsKey(DEVICE_ID));
        });
    }

    //TODO add test for modified context and event handling form device listener.

    private class MockDeviceService extends DeviceServiceAdapter {

        @Override
        public Device getDevice(DeviceId did) {
            if (did.equals(DEVICE_ID)) {
                return device;
            }
            return null;
        }

        @Override
        public Iterable<Device> getAvailableDevices() {
            return ImmutableSet.of(device);
        }

        @Override
        public boolean isAvailable(DeviceId did) {
            return did.equals(DEVICE_ID);
        }

        @Override
        public void addListener(DeviceListener listener) {
            deviceListeners.add(listener);
        }

        @Override
        public void removeListener(DeviceListener listener) {
            deviceListeners.remove(listener);
        }
    }

    private class MockMastershipService extends MastershipServiceAdapter {

        @Override
        public boolean isLocalMaster(DeviceId deviceId) {
            return true;
        }

        @Override
        public void addListener(MastershipListener listener) {
            mastershipListeners.add(listener);
        }

        @Override
        public void removeListener(MastershipListener listener) {
            mastershipListeners.remove(listener);
        }
    }

    private class MockDeviceProviderRegistry extends AlarmProviderRegistryAdapter {

        Set<ProviderId> providers = new HashSet<>();

        @Override
        public AlarmProviderService register(AlarmProvider provider) {
            return alarmProviderService;
        }

        @Override
        public void unregister(AlarmProvider provider) {
            providers.remove(provider.id());
        }

        @Override
        public Set<ProviderId> getProviders() {
            return providers;
        }

    }

    private class MockAlarmProviderService implements AlarmProviderService {

        @Override
        public void updateAlarmList(DeviceId deviceId, Collection<Alarm> alarms) {
            if (alarmStore.containsKey(deviceId)) {
                Collection<Alarm> deviceAlarms = alarmStore.get(deviceId);
                deviceAlarms.addAll(alarms);
                alarmStore.put(deviceId, deviceAlarms);
            } else {
                alarmStore.put(deviceId, alarms);
            }

        }

        @Override
        public AlarmProvider provider() {
            return null;
        }
    }

    private class MockComponentContext extends ComponentContextAdapter {
        @Override
        public Dictionary getProperties() {
            return new MockDictionary();
        }
    }

    private class MockDictionary extends Dictionary {

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public Enumeration keys() {
            return null;
        }

        @Override
        public Enumeration elements() {
            return null;
        }

        @Override
        public Object get(Object key) {
            if (OsgiPropertyConstants.POLL_FREQUENCY_SECONDS.equals(key)) {
                return "1";
            }
            return null;
        }

        @Override
        public Object put(Object key, Object value) {
            return null;
        }

        @Override
        public Object remove(Object key) {
            return null;
        }
    }

    private class MockDevice extends DefaultDevice {
        /**
         * Creates a network element attributed to the specified provider.
         *
         * @param providerId   identity of the provider
         * @param id           device identifier
         * @param type         device type
         * @param manufacturer device manufacturer
         * @param hwVersion    device HW version
         * @param swVersion    device SW version
         * @param serialNumber device serial number
         * @param chassisId    chassis id
         * @param annotations  optional key/value annotations
         */
        public MockDevice(ProviderId providerId, DeviceId id, Type type,
                          String manufacturer, String hwVersion, String swVersion,
                          String serialNumber, ChassisId chassisId, Annotations... annotations) {
            super(providerId, id, type, manufacturer, hwVersion, swVersion, serialNumber,
                  chassisId, annotations);
        }

        @Override
        protected Driver locateDriver() {
            return driver;
        }

        @Override
        public Driver driver() {
            return driver;
        }
    }

    private class MockDriver extends DriverAdapter {
        @Override
        public <T extends Behaviour> T createBehaviour(DriverHandler handler, Class<T> behaviourClass) {
            return (T) new TestAlarmConsumer();
        }
    }

    private class TestAlarmConsumer extends AbstractHandlerBehaviour implements AlarmConsumer {

        @Override
        public List<Alarm> consumeAlarms() {
            return ImmutableList.of(ALARM);
        }
    }

}
