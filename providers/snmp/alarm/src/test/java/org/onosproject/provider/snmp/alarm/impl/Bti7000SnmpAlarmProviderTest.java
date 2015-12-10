/*
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
package org.onosproject.provider.snmp.alarm.impl;

import com.btisystems.mibbler.mibs.bti7000.bti7000_13_2_0.btisystems.btiproducts.bti7000.objects.conditions.ActAlarmTable;
import com.btisystems.mibbler.mibs.bti7000.interfaces.btisystems.btiproducts.bti7000.objects.conditions.actalarmtable.IActAlarmEntry;
import com.btisystems.pronx.ems.core.model.NetworkDevice;
import com.btisystems.pronx.ems.core.snmp.ISnmpSession;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import org.easymock.Capture;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.net.DeviceId;
import org.snmp4j.smi.OID;


public class Bti7000SnmpAlarmProviderTest {
    private Bti7000SnmpAlarmProvider alarmProvider;
    private ISnmpSession mockSession;
    private ActAlarmTable alarmsTable;

    public Bti7000SnmpAlarmProviderTest() {
    }

    @Before
    public void setUp() {
        mockSession = createMock(ISnmpSession.class);

        alarmProvider = new Bti7000SnmpAlarmProvider();
    }

    @Test
    public void shouldWalkDevice() throws UnknownHostException, IOException {
        expect(mockSession.getAddress()).andReturn(InetAddress.getLoopbackAddress());
        expect(mockSession.walkDevice(isA(NetworkDevice.class),
                eq(Arrays.asList(new OID[]{
                    Bti7000SnmpAlarmProvider.CLASS_REGISTRY.getClassToOidMap().get(ActAlarmTable.class)}))))
                .andReturn(null);

        replay(mockSession);

        assertNotNull(alarmProvider.getAlarms(mockSession, DeviceId.deviceId("snmp:1.1.1.1")));

        verify(mockSession);
    }

    @Test
    public void shouldFindAlarms() throws UnknownHostException, IOException {
        alarmsTable = new ActAlarmTable();
        alarmsTable.createEntry("14.1.3.6.1.4.1.18070.2.2.2.2.20.0.1.13.1.3.6.1.4.1."
                + "18070.2.2.1.4.14.1.7.49.46.55.46.50.46.53");
        IActAlarmEntry entry = alarmsTable.getEntries().values().iterator().next();
        entry.setActAlarmDescription("XFP Missing.");
        entry.setActAlarmDateAndTime("07:df:0c:01:03:0d:30:00");
        entry.setActAlarmSeverity(1);

        Capture<NetworkDevice> networkDeviceCapture = new Capture<>();

        expect(mockSession.getAddress()).andReturn(InetAddress.getLoopbackAddress());
        expect(mockSession.walkDevice(capture(networkDeviceCapture),
                eq(Arrays.asList(new OID[]{
                    Bti7000SnmpAlarmProvider.CLASS_REGISTRY.getClassToOidMap().get(ActAlarmTable.class)}))))
                .andAnswer(() -> {
                    networkDeviceCapture.getValue().getRootObject().setObject(alarmsTable);
                    return null;
        });

        replay(mockSession);

        Collection<Alarm> alarms = alarmProvider.getAlarms(mockSession, DeviceId.deviceId("snmp:1.1.1.1"));
        assertEquals(1, alarms.size());
        assertEquals("XFP Missing.", alarms.iterator().next().description());
        verify(mockSession);
    }

    @Test
    public void shouldHandleException() throws UnknownHostException, IOException {
        expect(mockSession.getAddress()).andReturn(InetAddress.getLoopbackAddress());
        expect(mockSession.walkDevice(isA(NetworkDevice.class),
                eq(Arrays.asList(new OID[]{
                    Bti7000SnmpAlarmProvider.CLASS_REGISTRY.getClassToOidMap().get(ActAlarmTable.class)}))))
                .andThrow(new IOException());

        replay(mockSession);

        assertNotNull(alarmProvider.getAlarms(mockSession, DeviceId.deviceId("snmp:1.1.1.1")));

        verify(mockSession);
    }

}
