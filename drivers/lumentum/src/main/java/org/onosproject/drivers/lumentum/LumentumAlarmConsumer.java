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

package org.onosproject.drivers.lumentum;

import com.google.common.collect.ImmutableList;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmConsumer;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmId;
import org.onosproject.incubator.net.faultmanagement.alarm.DefaultAlarm;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.snmp.SnmpController;
import org.onosproject.snmp.SnmpDevice;
import org.slf4j.Logger;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.TreeEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.incubator.net.faultmanagement.alarm.Alarm.SeverityLevel;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Alarm Consumer for Lumentum devices.
 */
public class LumentumAlarmConsumer extends AbstractHandlerBehaviour implements AlarmConsumer {

    private final Logger log = getLogger(getClass());

    private static final String ALARM_TABLE = ".1.3.6.1.4.1.46184.1.3.2";
    private static final String ALARM_ID = ".1.3.6.1.4.1.46184.1.3.2.1.1";
    private static final OID ALARMS_TABLE_OID = new OID(ALARM_TABLE);
    private static final OID ALARMS_ID_OID = new OID(ALARM_ID);
    private LumentumSnmpDevice snmp;

    @Override
    public List<Alarm> consumeAlarms() {

        SnmpController controller = checkNotNull(handler().get(SnmpController.class));
        List<Alarm> alarms = new ArrayList<>();
        DeviceId deviceId = handler().data().deviceId();
        SnmpDevice device = controller.getDevice(deviceId);
        try {
            snmp = new LumentumSnmpDevice(device.getSnmpHost(), device.getSnmpPort());
        } catch (IOException e) {
            log.error("Failed to connect to device: ", e);
        }

        // Gets the alarm table and for each entry get the ID and create the proper alarm.
        snmp.get(ALARMS_TABLE_OID)
                .forEach(alarm -> snmp.get(ALARMS_ID_OID).forEach(alarmIdEvent -> {
                    int alarmId = getAlarmId(alarmIdEvent);
                    alarms.add(new DefaultAlarm.Builder(deviceId, getMessage(alarmId),
                                                        getSeverity(alarmId),
                                                        System.currentTimeMillis())
                                       .withId(AlarmId.alarmId(deviceId, String.valueOf(alarmId)))
                                       .build());
                }));
        return ImmutableList.copyOf(alarms);
    }

    //Walks the tree and retrieves the alarmId
    private int getAlarmId(TreeEvent treeEvents) {
        VariableBinding[] varBindings = treeEvents.getVariableBindings();
        for (VariableBinding varBinding : varBindings) {
            return varBinding.getVariable().toInt();
        }
        return -1;
    }

    //Returns the severity level.
    private SeverityLevel getSeverity(int alarmId) {
        switch (alarmId) {
            case 14:
                return SeverityLevel.INDETERMINATE;
            default:
                return SeverityLevel.MAJOR;
        }
    }

    //Returns a string message based on the id of the alarm as per .mib file.
    private String getMessage(int alarmId) {
        switch (alarmId) {
            case 1:
                return "Port Los";
            case 2:
                return "Port Degrade";
            case 3:
                return "Port High Power";
            case 4:
                return "Ta Failure";
            case 5:
                return "Force Apr";
            case 6:
                return "Force Shutoff";
            case 7:
                return "Gain Oor";
            case 8:
                return "Low Orl";
            case 9:
                return "Apr";
            case 10:
                return "Los Shutoff";
            case 11:
                return "Amp Degrade";
            case 12:
                return "Channel Los";
            case 13:
                return "Channel Degrade";
            case 14:
                return "Unsupported";
            default:
                return "Unknown";
        }
    }
}
