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

package org.onosproject.drivers.polatis.netconf;

import com.google.common.collect.ImmutableList;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.alarm.Alarm;
import org.onosproject.alarm.AlarmConsumer;
import org.onosproject.alarm.AlarmId;
import org.onosproject.alarm.DefaultAlarm;
import org.onosproject.alarm.XmlEventParser;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.mastership.MastershipService;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.onosproject.alarm.Alarm.SeverityLevel;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.configsAt;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.xmlEmpty;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_DATA_SYSTEMALARMS;
import static org.onosproject.drivers.polatis.netconf.PolatisNetconfUtility.KEY_SYSTEMALARMS_XMLNS;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Polatis specific implementation to provide a list of current alarms.
 */
public class PolatisAlarmConsumer extends AbstractHandlerBehaviour implements AlarmConsumer {
    private final Logger log = getLogger(getClass());

    private static final String ALARM_TIME = "alarm-time";
    private static final String ALARM_TYPE = "alarm-type";
    private static final String ALARM_TYPE_LOS = "NOTIF_PORT_POWER";
    private static final String ALARM_MESSAGE = "alarm-message";

    private DeviceId deviceId;

    @Override
    public List<Alarm> consumeAlarms() {
        DriverHandler handler = handler();
        NetconfController controller = handler.get(NetconfController.class);
        checkNotNull(controller, "Netconf controller is null");

        MastershipService mastershipService = handler.get(MastershipService.class);
        deviceId = handler.data().deviceId();

        List<Alarm> alarms = new ArrayList<>();
        if (!mastershipService.isLocalMaster(deviceId)) {
            log.warn("Not master for {} Use {} to execute command",
                     deviceId,
                     mastershipService.getMasterFor(deviceId));
            return ImmutableList.copyOf(alarms);
        }

        try {
            String request = xmlEmpty(KEY_SYSTEMALARMS_XMLNS);
            String reply = controller.getDevicesMap()
                               .get(deviceId)
                               .getSession()
                               .get(request, null);
            if (reply != null) {
                alarms = parseAlarms(reply);
            }
        } catch (NetconfException e) {
            log.error("Error reading alarms for device {} exception {}", deviceId, e);
        }

        return ImmutableList.copyOf(alarms);
    }

    private List<Alarm> parseAlarms(String content) {
        List<HierarchicalConfiguration> subtrees = configsAt(content, KEY_DATA_SYSTEMALARMS);
        List<Alarm> alarms = new ArrayList<>();
        for (HierarchicalConfiguration alarm : subtrees) {
            alarms.add(parseAlarm(alarm));
        }
        return alarms;
    }

    private Alarm parseAlarm(HierarchicalConfiguration cfg) {
        boolean cleared = false;
        String alarmType = cfg.getString(ALARM_TYPE);
        String alarmMessage = cfg.getString(ALARM_MESSAGE);
        SeverityLevel alarmLevel = SeverityLevel.INDETERMINATE;
        if (alarmType.equals(ALARM_TYPE_LOS)) {
            alarmLevel = SeverityLevel.MAJOR;
        }
        long timeRaised = getTimeRaised(cfg);
        DefaultAlarm.Builder alarmBuilder = new DefaultAlarm.Builder(
                AlarmId.alarmId(deviceId, alarmMessage),
                deviceId, alarmMessage, alarmLevel, timeRaised);
        return alarmBuilder.build();
    }

    private long getTimeRaised(HierarchicalConfiguration cfg) {
        String alarmTime = cfg.getString(ALARM_TIME);
        return XmlEventParser.getEventTime(alarmTime);
    }
}
