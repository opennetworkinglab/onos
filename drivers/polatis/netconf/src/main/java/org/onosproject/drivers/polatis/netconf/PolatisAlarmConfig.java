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

import org.onlab.packet.IpAddress;

import org.onosproject.alarm.Alarm;
import org.onosproject.alarm.AlarmId;
import org.onosproject.alarm.DefaultAlarm;
import org.onosproject.alarm.DeviceAlarmConfig;
import org.onosproject.alarm.XmlEventParser;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfDeviceOutputEvent;

import org.slf4j.Logger;

import org.xml.sax.SAXException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Polatis specific implementation to provide asynchronous alarms via NETCONF.
 */
public class PolatisAlarmConfig extends AbstractHandlerBehaviour implements DeviceAlarmConfig {
    private final Logger log = getLogger(getClass());

    private DeviceId deviceId;
    private static final String ALARM_TYPE_LOS = "port-power-alarm";

    @Override
    public boolean configureDevice(IpAddress address, int port, String protocol) {
        return false;
    }

    @Override
    public <T> Set<Alarm> translateAlarms(List<T> unparsedAlarms) {
        deviceId = handler().data().deviceId();
        Set<Alarm> alarms = new HashSet<>();
        for (T alarm : unparsedAlarms) {
            if (alarm instanceof NetconfDeviceOutputEvent) {
                NetconfDeviceOutputEvent event = (NetconfDeviceOutputEvent) alarm;
                if (event.type() == NetconfDeviceOutputEvent.Type.DEVICE_NOTIFICATION) {
                    String message = event.getMessagePayload();
                    InputStream in = new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8));
                    try {
                        Document doc = XmlEventParser.createDocFromMessage(in);
                        long timeStamp = XmlEventParser.getEventTime(doc);
                        Node descriptionNode = XmlEventParser.getDescriptionNode(doc);
                        while (descriptionNode != null) {
                            if (descriptionNode.getNodeType() == Node.ELEMENT_NODE) {
                                String nodeName = descriptionNode.getNodeName();
                                if (nodeName.equals(ALARM_TYPE_LOS)) {
                                    Node portIdNode = descriptionNode.getChildNodes().item(1);
                                    String portId = portIdNode.getTextContent();
                                    String description = "Loss of Service alarm raised for fibre " + portId;
                                    alarms.add(new DefaultAlarm.Builder(AlarmId.alarmId(deviceId,
                                                description), deviceId, description,
                                                Alarm.SeverityLevel.MAJOR, timeStamp).build());
                                    descriptionNode = null;
                                }
                            } else {
                                descriptionNode = descriptionNode.getNextSibling();
                            }
                        }
                    } catch (SAXException | IOException | ParserConfigurationException |
                            UnsupportedOperationException | IllegalArgumentException e) {
                        log.error("Exception thrown translating message from {}.", deviceId, e);
                    }
                }
            }
        }
        return alarms;
    }
}
