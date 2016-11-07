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

package org.onosproject.drivers.fujitsu;

import com.google.common.collect.ImmutableList;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmConsumer;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmEntityId;
import org.onosproject.incubator.net.faultmanagement.alarm.DefaultAlarm;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.netconf.NetconfController;
import org.onosproject.mastership.MastershipService;

import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.TimeZone;

import java.nio.charset.StandardCharsets;
import java.io.ByteArrayInputStream;

import static org.onosproject.incubator.net.faultmanagement.alarm.Alarm.SeverityLevel;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.fujitsu.FujitsuVoltXmlUtility.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Fujitsu vOLT specific implementation to provide a list of current alarms.
 */
public class FujitsuVoltAlarmConsumer extends AbstractHandlerBehaviour implements AlarmConsumer {
    private final Logger log = getLogger(getClass());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private static final TimeZone ZONE =  TimeZone.getTimeZone("UTC");

    private static final String VOLT_ALERTS = "volt-alerts";
    private static final String OLT_ACTIVE_ALERTS = "olt-active-alerts";
    private static final String ALERT_INFO = "alert-info";
    private static final String ALERT_SEQNUM = "alert-seqnum";
    private static final String ALERT_TYPE = "alert-type";
    private static final String ALERT_TIME = "alert-time";
    private static final String ALERT_CLEAR = "alert-clear";
    private static final String RESOURCE_ID = "resource-id";
    private static final String SEVERITY = "severity";
    private static final String IP_ADDRESS = "ip-address";
    private static final String DATE = "date";
    private static final String TIME = "time";
    private static final String OLT_ACTIVE_ALERTS_KEY =
            "data." + VOLT_NE + ".volt-alerts.olt-active-alerts";
    private static final String SLASH = "/";
    private DeviceId ncDeviceId;

    private enum AlertResourceType {
        UNKNOWN,
        PONLINK,
        ONU,
        SYSTEM
    }

    private enum AlertSeverity {
        INFO("info", SeverityLevel.WARNING),
        MINOR("minor", SeverityLevel.MINOR),
        MAJOR("major", SeverityLevel.MAJOR),
        CRITICAL("critical", SeverityLevel.CRITICAL);

        private String text;
        private SeverityLevel level;

        AlertSeverity(String text, SeverityLevel level) {
            this.text = text;
            this.level = level;
        }

        public static SeverityLevel convertToAlarmSeverityLevel(String text) {
            for (AlertSeverity severity : AlertSeverity.values()) {
                if (text.equalsIgnoreCase(severity.text)) {
                    return severity.level;
                }
            }
            return SeverityLevel.INDETERMINATE;
        }
    }

    @Override
    public List<Alarm> consumeAlarms() {
        DriverHandler handler = handler();
        NetconfController controller = handler.get(NetconfController.class);
        MastershipService mastershipService = handler.get(MastershipService.class);
        ncDeviceId = handler.data().deviceId();
        checkNotNull(controller, "Netconf controller is null");

        if (!mastershipService.isLocalMaster(ncDeviceId)) {
            log.warn("Not master for {} Use {} to execute command",
                     ncDeviceId,
                     mastershipService.getMasterFor(ncDeviceId));
            return null;
        }

        dateFormat.setTimeZone(ZONE);
        List<Alarm> alarms = new ArrayList<>();
        try {
            StringBuilder request = new StringBuilder();
            request.append(VOLT_NE_OPEN + VOLT_NE_NAMESPACE)
                .append(ANGLE_RIGHT + NEW_LINE)
                .append(buildStartTag(VOLT_ALERTS))
                .append(buildEmptyTag(OLT_ACTIVE_ALERTS))
                .append(buildEndTag(VOLT_ALERTS))
                .append(VOLT_NE_CLOSE);

            String reply = controller.getDevicesMap()
                               .get(ncDeviceId)
                               .getSession()
                               .get(request.toString(), null);
            if (reply != null) {
                alarms = parseVoltActiveAlerts(XmlConfigParser.
                    loadXml(new ByteArrayInputStream(reply.getBytes(StandardCharsets.UTF_8))));
            }
        } catch (IOException e) {
            log.error("Error reading alarms for device {} exception {}", ncDeviceId, e);
        }

        return ImmutableList.copyOf(alarms);
    }

    /**
     * Parses XML string to get controller information.
     *
     * @param cfg a hierarchical configuration
     * @return a list of alarms
     */
    private List<Alarm> parseVoltActiveAlerts(HierarchicalConfiguration cfg) {
        List<Alarm> alarms = new ArrayList<>();
        List<HierarchicalConfiguration> fields =
                cfg.configurationsAt(OLT_ACTIVE_ALERTS_KEY);

        for (HierarchicalConfiguration sub : fields) {
            List<HierarchicalConfiguration> childFields =
                    sub.configurationsAt(ALERT_INFO);

            for (HierarchicalConfiguration child : childFields) {
                try {
                    int seqNum = Integer.parseInt(child.getString(ALERT_SEQNUM));
                    boolean cleared = Boolean.parseBoolean(child.getString(ALERT_CLEAR));
                    String alertType = child.getString(ALERT_TYPE);
                    String severity = child.getString(SEVERITY);

                    List<HierarchicalConfiguration> idFields =
                            child.configurationsAt(RESOURCE_ID);
                    if (idFields.isEmpty()) {
                        log.error("{} does not exsit: SQ={}, TYPE={}, SEV={}, CLEARED={}",
                                RESOURCE_ID, seqNum, alertType, severity, cleared);
                        continue;
                    }
                    String alarmSrc = formAlarmSource(idFields);
                    if (alarmSrc == null) {
                        log.error("Cannot build description: SQ={}, TYPE={}, SEV={}, CLEARED={}",
                                seqNum, alertType, severity, cleared);
                        continue;
                    }
                    long timeRaised = getTimeRaised(child);
                    log.debug("VOLT: ACTIVE ALERT: SQ={}, TYPE={}, SEV={}, CLEARED={}, TIME={}",
                            seqNum, alertType, severity, cleared, timeRaised);

                    SeverityLevel alarmLevel =
                            AlertSeverity.convertToAlarmSeverityLevel(severity);
                    if (alarmLevel.equals(SeverityLevel.INDETERMINATE)) {
                        log.warn("Unknown severity: {}", severity);
                    }
                    DefaultAlarm.Builder alarmBuilder = new DefaultAlarm.Builder(
                            ncDeviceId, alertType.toUpperCase(), alarmLevel, timeRaised)
                            .forSource(AlarmEntityId.alarmEntityId(alarmSrc));
                    alarms.add(alarmBuilder.build());
                } catch (NumberFormatException e) {
                    log.error("Non-number exception {}", e);
                } catch (Exception e) {
                    log.error("Exception {}", e);
                }
            }
        }
        return alarms;
    }

    /**
     * Builds alarm source with resource information.
     * @param idFields a hierarchical configuration
     * @return formed alarm description
     */
    private String formAlarmSource(List<HierarchicalConfiguration> idFields) {
        AlertResourceType resourceType = AlertResourceType.UNKNOWN;
        StringBuilder alarmSrc = new StringBuilder();
        String ipAddr = null;
        int pon = ZERO;
        int onu = ZERO;

        for (HierarchicalConfiguration id : idFields) {
            String value;
            try {
                value = id.getString(PONLINK_ID);
                if (value == null) {
                    resourceType = AlertResourceType.SYSTEM;
                    ipAddr = id.getString(IP_ADDRESS);
                } else {
                    pon = Integer.parseInt(value);
                    value = id.getString(ONU_ID);
                    if (value == null) {
                        resourceType = AlertResourceType.PONLINK;
                    } else {
                        resourceType = AlertResourceType.ONU;
                        onu = Integer.parseInt(value);
                    }
                }
            } catch (NumberFormatException e) {
                log.error("Non-number resource-id exception {}", e);
                return null;
            }
        }

        alarmSrc.append("other:");
        alarmSrc.append(resourceType.name()).append(SLASH);
        switch (resourceType) {
            case PONLINK:
                alarmSrc.append(pon);
                break;
            case ONU:
                alarmSrc.append(pon).append(HYPHEN).append(onu);
                break;
            case SYSTEM:
                if (ipAddr != null) {
                    alarmSrc.append(ipAddr);
                }
                break;
            default:
                break;
        }
        return alarmSrc.toString();
    }

    /**
     * Converts time and date information from device.
     * @param cfg a hierarchical configuration
     * @return converted time from device or system time
     */
    private long getTimeRaised(HierarchicalConfiguration cfg) {
        String strDate;
        String strTime;
        long timeRaised;

        List<HierarchicalConfiguration> timeFields =
                cfg.configurationsAt(ALERT_TIME);
        if (timeFields.isEmpty()) {
            log.debug("{} does not exsit", ALERT_TIME);
        } else {
            for (HierarchicalConfiguration child : timeFields) {
                strDate = child.getString(DATE);
                strTime = child.getString(TIME);
                if ((strDate != null) && (strTime != null)) {
                    try {
                        Date date = dateFormat.parse(strDate + SPACE + strTime);
                        timeRaised = date.getTime();
                        log.debug("{} {} coverted to {}", strDate, strTime, timeRaised);
                        return timeRaised;
                    } catch (ParseException e) {
                        log.error("Cannot parse exception {} {} {}", strDate, strTime, e);
                    }
                } else {
                    log.error("{} or {} does not exsit", DATE, TIME);
                }
            }
        }
        // Use the system's time instead.
        return System.currentTimeMillis();
    }

}
