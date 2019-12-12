/*
 * Copyright 2019-present Open Networking Foundation
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
 *
 *
 * This work was done in Nokia Bell Labs Paris
 *
 */

package org.onosproject.drivers.odtn;

import org.onlab.osgi.DefaultServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.alarm.Alarm;
import org.onosproject.alarm.AlarmId;
import org.onosproject.alarm.AlarmService;
import org.onosproject.alarm.DefaultAlarm;
import org.onosproject.alarm.DeviceAlarmConfig;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfDeviceOutputEvent;
import org.slf4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.onosproject.alarm.Alarm.SeverityLevel;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * OpenConfig alarm translator for Nokia devices.
 */
public class NokiaOpenConfigAlarmConfig
        extends AbstractHandlerBehaviour implements DeviceAlarmConfig {
    private final Logger log = getLogger(getClass());

    private DeviceId deviceId;
    private AlarmService alarmService = DefaultServiceDirectory.getService(AlarmService.class);
    private static final String XML_PATH = "/system/alarms/alarm/";

    @Override
    public boolean configureDevice(IpAddress address, int port, String protocol) {
        return false;
    }

    @Override
    public <T> Set<Alarm> translateAlarms(List<T> unparsedAlarms) {

        boolean categoryFound = false;
        deviceId = handler().data().deviceId();
        Set<Alarm> alarms = new HashSet<>();
        for (T alarm : unparsedAlarms) {
            categoryFound = false;
            if (alarm instanceof NetconfDeviceOutputEvent) {
                NetconfDeviceOutputEvent event = (NetconfDeviceOutputEvent) alarm;
                if (event.type() == NetconfDeviceOutputEvent.Type.DEVICE_NOTIFICATION) {
                    String message = event.getMessagePayload();
                    if (message.contains("<update>")) {
                        categoryFound = true;
                        DefaultAlarm newAlarm = treatUpdate(message);
                        if (newAlarm != null) {
                            alarms.add(newAlarm);
                        }
                    }
                    if (message.contains("<delete>")) {
                        categoryFound = true;
                        treatDelete(message);
                    }
                    if (!categoryFound) {
                        log.debug("[translateAlarms] Appropriate category wasn't found, " +
                                          "you have something else \n {} ", message);
                    }
                }
            }
        }

        return alarms;
    }


    /**
     * Gets <update> body out of the XML notification.
     * @param notification - XML notification obtained from the device.
     * @return - <update> body of the notification.
     */
    private String getUpdateDataFromNotification(String notification) {

        String data = null;
        int begin = notification.indexOf("<update>");
        int end = notification.indexOf("</update>");
        if (begin != -1 && end != -1) {
            data = notification.substring(begin + "<update>".length(), end);
        } else {
            data = notification;
        }

        return data;
    }

    /**
     * Gets <delete> body out of XML notification.
     * @param notification - XML notification obtained from the device.
     * @return - <delete> body of the notification.
     */
    private String getDeleteDataFromNotification(String notification) {

        String data = null;
        int begin = notification.indexOf("<delete>");
        int end = notification.indexOf("</delete>");

        if (begin != -1 && end != -1) {
            data = notification.substring(begin + "<delete>".length(), end);
        } else {
            data = notification;
        }

        return data;
    }

    /**
     * Treats alarm in case it contains <update> tag.
     * @param message - XML-encoded notification obtained from the device.
     * @return - composed alarm.
     */
    private DefaultAlarm treatUpdate(String message) {

        return buildAlarm(getUpdateDataFromNotification(message));
    }

    /**
     * Composes an Alarm from XML string.
     * @param message - XML string obtained form the device.
     * @return - composed alarm as a DefaultAlarm instance.
     */
    private DefaultAlarm buildAlarm(String message) {

        try {

            InputSource src = new InputSource(new StringReader(message));

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(src);

            XPathFactory xpathFactory = XPathFactory.newInstance();
            XPath xpath = xpathFactory.newXPath();

            String severity = null;
            severity = xpath.evaluate(XML_PATH +
                                              "state/severity", document);

            // treating alarm according to the severity level
            if ((severity != null) & (severity != "")) {
                // Setting flag to match correct severity
                boolean severityFound = false;

                // Extracting parameters
                try {
                    String id = xpath.evaluate(XML_PATH + "id", document);
                    String source = xpath.evaluate(XML_PATH +
                                                           "state/resource", document);
                    String description = xpath.evaluate(XML_PATH +
                                                                "state/text", document);
                    long timeStamp = Long.parseLong(xpath.evaluate(XML_PATH +
                                                                           "state/time-created", document));

                    if (severity.contains("CRITICAL")) {

                        return (new DefaultAlarm.Builder(AlarmId.alarmId(id),
                                                         deviceId, description + ":" + source,
                                                         SeverityLevel.CRITICAL, timeStamp).
                                withServiceAffecting(true).build());
                    }
                    if (severity.contains("MAJOR")) {

                        return (new DefaultAlarm.Builder(AlarmId.alarmId(id),
                                                         deviceId, description + ":" + source,
                                                         SeverityLevel.MAJOR, timeStamp).
                                withServiceAffecting(true).build());
                    }
                    if (severity.contains("MINOR")) {

                        return (new DefaultAlarm.Builder(AlarmId.alarmId(id),
                                                         deviceId, description + ":" + source,
                                                         SeverityLevel.MINOR, timeStamp).build());
                    }
                    if (!severityFound) {
                        // treating alarm as an unknown one?

                        return (new DefaultAlarm.Builder(AlarmId.alarmId(id),
                                                         deviceId, description + ":" + source,
                                                         SeverityLevel.INDETERMINATE,
                                                         timeStamp).build());
                    }
                } catch (Exception e) {
                    log.error("[translateAlarms] Exception was caught during the alarm processing\n {}", e);
                }
            }
        } catch (Exception e) {
            log.error("[translateAlarms] something went wrong during notification parsing \n {}", e);
        }

        return null;
    }

    /**
     * This method searches for similar alarm inside the Alarm store of ONOS.
     * @param message - XML String obtained from the device.
     * @return - alarm (if was found in the store) or null.
     */
    private Alarm findAlarm(String message) {

        Collection<Alarm> alarms = alarmService.getAlarms(deviceId);
        Alarm alarm = buildAlarm(message);
        if ((alarms.contains(alarm)) & (alarm != null)) {
            log.debug("Alarm was found \n {}", alarm);
            return alarm;
        }

        log.debug("Alarm was NOT found \n {}", alarm);
        return null;
    }

    /**
     * Treats message in case it contains <delete> tag.
     * @param message - XML-encoded notification obtained from the device.
     * @return - composed alarm.
     */
    private void treatDelete(String message) {

        Alarm existingAlarm = findAlarm(getDeleteDataFromNotification(message));
        if (existingAlarm != null) {
            alarmService.updateBookkeepingFields(existingAlarm.id(), true,
                                                 true, null);
            log.debug("[treatDelete] Existing alarm was updated (CLEARED)");
        } else {
            log.debug("[treatDelete] We found something new here \n {} \n", message);
        }

    }


}
