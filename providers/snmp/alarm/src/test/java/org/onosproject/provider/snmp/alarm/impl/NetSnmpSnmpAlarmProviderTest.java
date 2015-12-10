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

import com.btisystems.mibbler.mibs.netsnmp.interfaces.mib_2.interfaces.iftable.IIfEntry;
import com.btisystems.mibbler.mibs.netsnmp.netsnmp.mib_2.interfaces.IfTable;
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


public class NetSnmpSnmpAlarmProviderTest {
    private NetSnmpAlarmProvider alarmProvider;
    private ISnmpSession mockSession;
    private IfTable interfaceTable;

    public NetSnmpSnmpAlarmProviderTest() {
    }

    @Before
    public void setUp() {
        mockSession = createMock(ISnmpSession.class);

        alarmProvider = new NetSnmpAlarmProvider();
    }

    @Test
    public void shouldWalkDevice() throws UnknownHostException, IOException {
        expect(mockSession.getAddress()).andReturn(InetAddress.getLoopbackAddress());
        expect(mockSession.walkDevice(isA(NetworkDevice.class),
                eq(Arrays.asList(new OID[]{
                    NetSnmpAlarmProvider.CLASS_REGISTRY.getClassToOidMap().get(IfTable.class)}))))
                .andReturn(null);

        replay(mockSession);

        assertNotNull(alarmProvider.getAlarms(mockSession, DeviceId.deviceId("snmp:1.1.1.1")));

        verify(mockSession);
    }

    @Test
    public void shouldFindAlarms() throws UnknownHostException, IOException {
        interfaceTable = new IfTable();
        interfaceTable.createEntry("1");
        IIfEntry entry = interfaceTable.getEntry("1");
        entry.setIfDescr("eth1");
        entry.setIfAdminStatus(1);
        entry.setIfOperStatus(2);

        Capture<NetworkDevice> networkDeviceCapture = new Capture<>();

        expect(mockSession.getAddress()).andReturn(InetAddress.getLoopbackAddress());
        expect(mockSession.walkDevice(capture(networkDeviceCapture),
                eq(Arrays.asList(new OID[]{
                    NetSnmpAlarmProvider.CLASS_REGISTRY.getClassToOidMap().get(IfTable.class)}))))
                .andAnswer(() -> {
                    networkDeviceCapture.getValue().getRootObject().setObject(interfaceTable);
                    return null;
        });

        replay(mockSession);

        Collection<Alarm> alarms = alarmProvider.getAlarms(mockSession, DeviceId.deviceId("snmp:1.1.1.1"));
        assertEquals(1, alarms.size());
        assertEquals("Link Down.", alarms.iterator().next().description());
        verify(mockSession);
    }

    @Test
    public void shouldHandleException() throws UnknownHostException, IOException {
        expect(mockSession.getAddress()).andReturn(InetAddress.getLoopbackAddress());
        expect(mockSession.walkDevice(isA(NetworkDevice.class),
                eq(Arrays.asList(new OID[]{
                    NetSnmpAlarmProvider.CLASS_REGISTRY.getClassToOidMap().get(IfTable.class)}))))
                .andThrow(new IOException());

        replay(mockSession);

        assertNotNull(alarmProvider.getAlarms(mockSession, DeviceId.deviceId("snmp:1.1.1.1")));

        verify(mockSession);
    }

}
