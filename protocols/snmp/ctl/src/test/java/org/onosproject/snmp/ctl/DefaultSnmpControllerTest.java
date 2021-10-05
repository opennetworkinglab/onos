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

package org.onosproject.snmp.ctl;

import com.btisystems.pronx.ems.core.snmp.ISnmpConfiguration;
import com.btisystems.pronx.ems.core.snmp.ISnmpConfigurationFactory;
import com.btisystems.pronx.ems.core.snmp.ISnmpSession;
import com.btisystems.pronx.ems.core.snmp.ISnmpSessionFactory;
import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.alarm.Alarm;
import org.onosproject.alarm.AlarmId;
import org.onosproject.alarm.DefaultAlarm;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * DefaultSnmpController test class.
 */
public class DefaultSnmpControllerTest {

    ISnmpSessionFactory mockSnmpSessionFactory = new MockISnmpSessionFactory();

    DefaultSnmpController snmpController = new DefaultSnmpController();

    DefaultSnmpDevice device = new DefaultSnmpDevice("1.1.1.1", 1, "test", "test");

    ISnmpSession snmpSession = new ISnmpSessionAdapter();

    long time = System.currentTimeMillis();
    DefaultAlarm alarm = new DefaultAlarm.Builder(
            AlarmId.alarmId(device.deviceId(), Long.toString(time)),
            device.deviceId(), "SNMP alarm retrieval failed",
            Alarm.SeverityLevel.CRITICAL,
            time).build();

    @Before
    public void setUp() {
        snmpController.factoryMap = Maps.newHashMap();
        snmpController.factoryMap.put(1, mockSnmpSessionFactory);
    }

    @Test
    public void testActivate() {
        snmpController.activate(null);
        assertTrue("Snmp session factory map should contain atleast one factory object",
                snmpController.factoryMap.size() > 0);
    }

    @Test
    public void testDeactivate() {
        snmpController.deactivate();
        assertEquals("Device map should be clear", 0, snmpController.getDevices().size());
        assertEquals("Session map should be clear", 0, snmpController.sessionMap.size());
    }

    @Test
    public void addDevice() {
        snmpController.addDevice(device);
        assertEquals("Controller should contain device", device, snmpController.getDevice(device.deviceId()));
    }

    /**
     * tests session creation and get from map if already exists.
     */
    @Test
    public void getNotExistingSession() throws Exception {
        addDevice();
        assertEquals("Session should be created", snmpSession, snmpController.getSession(device.deviceId()));
        assertEquals("Map should contain session", 1, snmpController.snmpDeviceMap.size());
        assertEquals("Session should be fetched from map", snmpSession, snmpController.getSession(device.deviceId()));
    }

    @Test
    public void removeDevice() {
        addDevice();
        snmpController.removeDevice(device.deviceId());
        assertNull("Device shoudl not be present", snmpController.getDevice(device.deviceId()));
    }

    @Test
    public void walkFailedAlarm() {
        assertEquals("Alarms should be equals", alarm, snmpController.buildWalkFailedAlarm(device.deviceId()));
    }

    public class MockISnmpSessionFactory implements ISnmpSessionFactory {

        @Override
        public ISnmpSession createSession(ISnmpConfiguration configuration, String ipAddress) throws IOException {
            new ISnmpSessionAdapter();
            return snmpSession;
        }

        @Override
        public ISnmpSession createSession(String ipAddress, String community)
                throws IOException {
            return snmpSession;
        }

        @Override
        public ISnmpSession createSession(String ipAddress, String community,
                                          String factoryName,
                                          ISnmpConfigurationFactory.AccessType accessType)
                throws IOException {
            return snmpSession;
        }
    }
}
