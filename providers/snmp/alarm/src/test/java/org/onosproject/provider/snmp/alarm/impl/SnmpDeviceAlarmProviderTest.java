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

import com.btisystems.pronx.ems.core.snmp.ISnmpConfiguration;
import com.btisystems.pronx.ems.core.snmp.ISnmpSession;
import com.btisystems.pronx.ems.core.snmp.ISnmpSessionFactory;
import java.io.IOException;
import java.util.HashSet;
import org.easymock.EasyMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmEvent;
import org.onosproject.net.DeviceId;

public class SnmpDeviceAlarmProviderTest {
    private SnmpAlarmProviderService alarmProvider;
    private ISnmpSessionFactory mockSessionFactory;
    private ISnmpSession mockSession;
    private SnmpDeviceAlarmProvider mockProvider;
    private AlarmEvent alarmEvent;

    public SnmpDeviceAlarmProviderTest() {}

    @Before
    public void setUp() {
        mockSessionFactory = EasyMock.createMock(ISnmpSessionFactory.class);
        mockSession = EasyMock.createMock(ISnmpSession.class);
        mockProvider = EasyMock.createMock(SnmpDeviceAlarmProvider.class);

        alarmProvider = new SnmpAlarmProviderService() {
            @Override
            protected ISnmpSessionFactory getSessionFactory() {
                return mockSessionFactory;
            }
        };

        alarmProvider.addAlarmListener((AlarmEvent event) -> {
            alarmEvent = event;
        });
    }

    @Test
    public void shouldPopulateAlarmsForNetSnmp() throws IOException {
        alarmProvider.providers.put("1.2.3.4", mockProvider);
        expect(mockSessionFactory.createSession(EasyMock.isA(ISnmpConfiguration.class),
                EasyMock.eq("1.1.1.1"))).andReturn(mockSession);
        expect(mockSession.identifyDevice()).andReturn("1.2.3.4");
        expect(mockProvider.getAlarms(mockSession, DeviceId.deviceId("snmp:1.1.1.1:161")))
                .andReturn(new HashSet<>());

        mockSession.close();
        EasyMock.expectLastCall().once();

        replay(mockSessionFactory, mockSession, mockProvider);

        alarmProvider.triggerProbe(DeviceId.deviceId("snmp:1.1.1.1:161"));

        verify(mockSessionFactory, mockSession, mockProvider);
        Assert.assertNotNull(alarmEvent);
    }

}
