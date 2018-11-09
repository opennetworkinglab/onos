/*
 * Copyright 2018 Open Networking Foundation
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

package org.onosproject.drivers.polatis.snmp;

import org.onlab.packet.IpAddress;

import org.onosproject.alarm.Alarm;
import org.onosproject.alarm.AlarmId;
import org.onosproject.alarm.DefaultAlarm;
import org.onosproject.alarm.DeviceAlarmConfig;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import org.snmp4j.CommandResponderEvent;
import org.snmp4j.PDU;
import org.snmp4j.smi.OID;

import org.slf4j.Logger;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.onosproject.alarm.Alarm.SeverityLevel;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Polatis specific implementation to provide asynchronous alarms via SNMP.
 */
public class PolatisAlarmConfig extends AbstractHandlerBehaviour implements DeviceAlarmConfig {
    private final Logger log = getLogger(getClass());

    private DeviceId deviceId;
    private static final OID SNMP_TRAP_OID = new OID(".1.3.6.1.6.3.1.1.4.1");
    private static final OID OPM_ALARM_OID = new OID(".1.3.6.1.4.1.26592.2.3.3.0.3");
    private static final OID ALARM_STATUS_OID = new OID(".1.3.6.1.4.1.26592.2.6.2.3.3");
    private static final OID ALARM_ID_OID = new OID(".1.3.6.1.4.1.26592.2.6.2.3.2");
    private static final OID ALARM_PORT_OID = new OID(".1.3.6.1.4.1.26592.2.3.3.1.1");
    private static final OID ALARM_PORT_LABEL_OID = new OID(".1.3.6.1.4.1.26592.2.3.3.1.2");
    private static final OID SYSUPTIME_OID = new OID(".1.3.6.1.2.1.1.3");
    private static final String CLEARED = "cleared";

    @Override
    public boolean configureDevice(IpAddress address, int port, String protocol) {
        // TODO: Implement me
        return false;
    }

    @Override
    public <T> Set<Alarm> translateAlarms(List<T> unparsedAlarms) {
        deviceId = handler().data().deviceId();
        Set<Alarm> alarms = new HashSet<>();
        for (T alarm : unparsedAlarms) {
            if (alarm instanceof CommandResponderEvent) {
                CommandResponderEvent alarmEvent = (CommandResponderEvent) alarm;
                PDU pdu = alarmEvent.getPDU();
                if (pdu != null) {
                    String alarmType = pdu.getVariable(SNMP_TRAP_OID).toString();
                    if (alarmType.equals(OPM_ALARM_OID.toString())) {
                        String label = pdu.getVariable(ALARM_PORT_LABEL_OID).toString();
                        int port = pdu.getVariable(ALARM_PORT_OID).toInt();
                        String uniqueIdentifier = "LOS" + port;
                        String status = pdu.getVariable(ALARM_STATUS_OID).toString();
                        String alarmMessage = "Loss of Service alarm " + status + " for fibre " + port;
                        SeverityLevel alarmLevel = SeverityLevel.MAJOR;
                        long timeRaised = 0;
                        DefaultAlarm.Builder alarmBuilder = new DefaultAlarm.Builder(
                                AlarmId.alarmId(deviceId, uniqueIdentifier),
                                deviceId, alarmMessage, alarmLevel, timeRaised);
                        if (status.equals(CLEARED)) {
                            long now = System.currentTimeMillis();
                            alarmBuilder.clear().withTimeUpdated(now).withTimeCleared(now);
                        }
                        alarms.add(alarmBuilder.build());
                    }
                }
            }
        }
        return alarms;
    }
}
