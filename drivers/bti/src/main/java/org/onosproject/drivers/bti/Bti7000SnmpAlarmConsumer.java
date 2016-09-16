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

package org.onosproject.drivers.bti;

import com.btisystems.mibbler.mibs.bti7000.bti7000_13_2_0.I_Device;
import com.btisystems.mibbler.mibs.bti7000.bti7000_13_2_0._OidRegistry;
import com.btisystems.mibbler.mibs.bti7000.bti7000_13_2_0.btisystems.btiproducts.bti7000.objects.conditions.ActAlarmTable;
import com.btisystems.mibbler.mibs.bti7000.interfaces.btisystems.btiproducts.bti7000.objects.conditions.IActAlarmTable;
import com.btisystems.pronx.ems.core.model.ClassRegistry;
import com.btisystems.pronx.ems.core.model.IClassRegistry;
import com.btisystems.pronx.ems.core.model.NetworkDevice;
import com.btisystems.pronx.ems.core.snmp.ISnmpSession;
import com.google.common.collect.ImmutableList;
import org.apache.commons.lang.StringUtils;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmConsumer;
import org.onosproject.incubator.net.faultmanagement.alarm.AlarmEntityId;
import org.onosproject.incubator.net.faultmanagement.alarm.DefaultAlarm;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.snmp.SnmpController;
import org.slf4j.Logger;
import org.snmp4j.smi.OctetString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * BTI 7000 specific implementation to provide a list of current alarms.
 */
public class Bti7000SnmpAlarmConsumer extends AbstractHandlerBehaviour implements AlarmConsumer {
    private final Logger log = getLogger(getClass());
    protected static final IClassRegistry CLASS_REGISTRY = new ClassRegistry(_OidRegistry.oidRegistry, I_Device.class);

    static final int ALARM_SEVERITY_MINOR = 2;
    static final int ALARM_SEVERITY_MAJOR = 3;
    static final int ALARM_SEVERITY_CRITICAL = 4;


    private Alarm.SeverityLevel mapAlarmSeverity(int intAlarmSeverity) {
        Alarm.SeverityLevel mappedSeverity;
        switch (intAlarmSeverity) {
            case ALARM_SEVERITY_MINOR:
                mappedSeverity = Alarm.SeverityLevel.MINOR;
                break;
            case ALARM_SEVERITY_MAJOR:
                mappedSeverity = Alarm.SeverityLevel.MAJOR;
                break;
            case ALARM_SEVERITY_CRITICAL:
                mappedSeverity = Alarm.SeverityLevel.CRITICAL;
                break;
            default:
                mappedSeverity = Alarm.SeverityLevel.MINOR;
                log.warn("Unexpected alarm severity: {}", intAlarmSeverity);
        }
        return mappedSeverity;
    }

    /**
     * Converts an SNMP string representation into a {@link Date} object,
     * and applies time zone conversion to provide the time on the local machine, ie PSM server.
     *
     * @param actAlarmDateAndTime MIB-II DateAndTime formatted. May optionally contain
     *                            a timezone offset in 3 extra bytes
     * @param sysInfoTimeZone     Must be supplied if actAlarmDateAndTime is just local time (with no timezone)
     * @param swVersion           Must be supplied if actAlarmDateAndTime is just local time (with no timezone)
     * @return adjusted {@link Date} or a simple conversion if other fields are null.
     */
    public static Date getLocalDateAndTime(String actAlarmDateAndTime, String sysInfoTimeZone,
                                           String swVersion) {
        if (StringUtils.isBlank(actAlarmDateAndTime)) {
            return null;
        }

        GregorianCalendar decodedDateAndTimeCal = btiMakeCalendar(OctetString.fromHexString(actAlarmDateAndTime));
        if ((sysInfoTimeZone == null) || (swVersion == null)) {
            return decodedDateAndTimeCal.getTime();
        }

        TimeZone javaTimeZone = getTimeZone();
        decodedDateAndTimeCal.setTimeZone(javaTimeZone);

        GregorianCalendar localTime = new GregorianCalendar();
        localTime.setTimeInMillis(decodedDateAndTimeCal.getTimeInMillis());

        return localTime.getTime();
    }

    /**
     * This method is similar to SNMP4J approach with some fixes for the 11-bytes version (ie the one with timezone
     * offset).
     * <p>
     * For original makeCalendar refer @see http://www.snmp4j.org/agent/doc/org/snmp4j/agent/mo/snmp/DateAndTime.html
     * <p>
     * Creates a <code>GregorianCalendar</code> from a properly formatted SNMP4J DateAndTime <code>OctetString</code>.
     *
     * @param dateAndTimeValue an OctetString conforming to the DateAndTime TC.
     * @return the corresponding <code>GregorianCalendar</code> instance.
     */
    public static GregorianCalendar btiMakeCalendar(OctetString dateAndTimeValue) {
        int year = (dateAndTimeValue.get(0) & 0xFF) * 256
                + (dateAndTimeValue.get(1) & 0xFF);
        int month = (dateAndTimeValue.get(2) & 0xFF);
        int date = (dateAndTimeValue.get(3) & 0xFF);
        int hour = (dateAndTimeValue.get(4) & 0xFF);
        int minute = (dateAndTimeValue.get(5) & 0xFF);
        int second = (dateAndTimeValue.get(6) & 0xFF);
        int deci = (dateAndTimeValue.get(7) & 0xFF);
        GregorianCalendar gc =
                new GregorianCalendar(year, month - 1, date, hour, minute, second);
        gc.set(Calendar.MILLISECOND, deci * 100);

        if (dateAndTimeValue.length() == 11) {
            char directionOfOffset = (char) dateAndTimeValue.get(8);
            int hoursOffset = directionOfOffset == '+'
                    ? dateAndTimeValue.get(9) : -dateAndTimeValue.get(9);
            org.joda.time.DateTimeZone offset =
                    org.joda.time.DateTimeZone.forOffsetHoursMinutes(hoursOffset, dateAndTimeValue.get(10));
            org.joda.time.DateTime dt =
                    new org.joda.time.DateTime(year, month, date, hour, minute, second, offset);
            return dt.toGregorianCalendar();
        }
        return gc;
    }

    private static TimeZone getTimeZone() {
        return Calendar.getInstance().getTimeZone();
    }

    @Override
    public List<Alarm> consumeAlarms() {
        SnmpController controller = checkNotNull(handler().get(SnmpController.class));
        ISnmpSession session;
        List<Alarm> alarms = new ArrayList<>();
        DeviceId deviceId = handler().data().deviceId();
        try {
            session = controller.getSession(deviceId);
            log.debug("Getting alarms for BTI 7000 device at {}", deviceId);
            NetworkDevice networkDevice = new NetworkDevice(CLASS_REGISTRY,
                                                            session.getAddress().getHostAddress());
            session.walkDevice(networkDevice, Collections.singletonList(
                    CLASS_REGISTRY.getClassToOidMap().get(ActAlarmTable.class)));

            IActAlarmTable deviceAlarms = (IActAlarmTable) networkDevice.getRootObject()
                    .getEntity(CLASS_REGISTRY.getClassToOidMap().get(ActAlarmTable.class));
            if ((deviceAlarms != null) && (deviceAlarms.getActAlarmEntry() != null)
                    && (!deviceAlarms.getActAlarmEntry().isEmpty())) {

                deviceAlarms.getActAlarmEntry().values().forEach((alarm) -> {
                    DefaultAlarm.Builder alarmBuilder = new DefaultAlarm.Builder(
                            deviceId, alarm.getActAlarmDescription(),
                            mapAlarmSeverity(alarm.getActAlarmSeverity()),
                            getLocalDateAndTime(alarm.getActAlarmDateAndTime(), null, null).getTime())
                            .forSource(AlarmEntityId.alarmEntityId("other:" + alarm.getActAlarmInstanceIdx()));
                    alarms.add(alarmBuilder.build());
                });

            }
            log.debug("Conditions retrieved: {}", deviceAlarms);

        } catch (IOException ex) {
            log.error("Error reading alarms for device {}.", deviceId, ex);
            alarms.add(controller.buildWalkFailedAlarm(deviceId));

        }

        return ImmutableList.copyOf(alarms);
    }
}
