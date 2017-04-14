/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.drivers.huawei;

import com.google.common.collect.Lists;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.net.Port.Type.COPPER;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Representation of Huawei XML parser.
 */
public final class HuaweiXmlParser {

    private static final String DATA = "data";
    private static final String IFM = "ifm";
    private static final String IFS = "interfaces";
    private static final String IF = "interface";
    private static final String IF_TYPE = "ifPhyType";
    private static final String IF_STATS = "ifStatistics";
    private static final String IF_STAT = "ifPhyStatus";
    private static final String IF_NUM = "ifNumber";
    private static final String IF_SPEED = "ifOperSpeed";
    private static final String IF_NAME = "ifName";
    private static final String UP = "up";
    private static final String DYN_INFO = "ifDynamicInfo";
    private static final String DELIMITER = "/";
    private static final String SYS = "system";
    private static final String SYS_INFO = "systemInfo";
    private static final String SYS_NAME = "sysName";
    private static final String PDT_VER = "productVer";
    private static final String PLATFORM_VER = "platformVer";
    private static final String SYS_ID = "sysObjectId";
    private static final String P_RCVD = "receivePacket";
    private static final String P_SENT = "sendPacket";
    private static final String B_RCVD = "receiveByte";
    private static final String B_SENT = "sendByte";
    private static final String RX_DROP = "rcvDropPacket";
    private static final String TX_DROP = "sendDropPacket";
    private static final String RX_ERROR = "rcvErrorPacket";
    private static final String TX_ERROR = "sendErrorPacket";

    private static final String DEV_PARSE_ERR = "Unable to parse the received" +
            " xml reply for system details from the huawei device";
    private static final String INT_PARSE_ERR = "Unable to parse the received" +
            " xml reply for interface details from the huawei device";
    private static final String P_NAME_INVALID = "Invalid port name.";

    //TODO: All type of interfaces has to be added.
    private static final List INTERFACES = Arrays.asList(
            "MEth", "Ethernet", "POS", "GigabitEthernet");

    private List<PortDescription> ports = new ArrayList<>();
    private String xml;
    private int portInc;
    private String[] info = new String[4];

    /**
     * Constructs huawei XML parser with xml reply.
     *
     * @param xml xml reply
     */
    public HuaweiXmlParser(String xml) {
        this.xml = xml;
    }

    /**
     * Returns the system info.
     *
     * @return system info
     */
    String[] getInfo() {
        return info;
    }

    /**
     * Returns the port list.
     *
     * @return port list
     */
    List<PortDescription> getPorts() {
        return ports;
    }

    /**
     * Parses system info xml reply.
     */
    void parseSysInfo() {
        Document doc;
        try {
            doc = DocumentHelper.parseText(xml);
        } catch (DocumentException e) {
            throw new IllegalArgumentException(DEV_PARSE_ERR);
        }

        Element root = doc.getRootElement();
        Element parent = root.element(DATA).element(SYS).element(SYS_INFO);
        info[0] = parent.element(SYS_NAME).getText();
        info[1] = parent.element(PDT_VER).getText();
        info[2] = parent.element(PLATFORM_VER).getText();
        info[3] = parent.element(SYS_ID).getText();
    }

    /**
     * Parses interface xml reply and creates ports to be updated.
     */
    void parseInterfaces() {
        Iterator itr = getInterfaceIterator();
        while (itr.hasNext()) {
            Element ifElement = (Element) itr.next();
            addPorts(ifElement);
        }
    }

    private Iterator getInterfaceIterator() {
        Document doc;
        try {
            doc = DocumentHelper.parseText(xml);
        } catch (DocumentException e) {
            throw new IllegalArgumentException(INT_PARSE_ERR);
        }
        Element root = doc.getRootElement();
        Element parent = root.element(DATA).element(IFM).element(IFS);
        return parent.elementIterator(IF);
    }

    /**
     * Adds port information to the port list from the xml reply.
     *
     * @param ifElement interface element
     */
    private void addPorts(Element ifElement) {
        String ifType = ifElement.element(IF_TYPE).getText();

        if (INTERFACES.contains(ifType)) {
            Element info = ifElement.element(DYN_INFO);
            String status = info.element(IF_STAT).getText();
            String port = getPortNum(ifElement.element(IF_NUM).getText());
            String speed = info.element(IF_SPEED).getText();
            String ifName = ifElement.element(IF_NAME).getText();

            boolean isEnabled = false;
            if (status.equals(UP)) {
                isEnabled = true;
            }

            Long portSpeed = 0L;
            if (!speed.isEmpty()) {
                portSpeed = Long.valueOf(speed);
            }

            DefaultAnnotations annotations = DefaultAnnotations.builder()
                    .set(PORT_NAME, ifName).build();
            ports.add(new DefaultPortDescription(portNumber(port), isEnabled,
                                                 COPPER, portSpeed,
                                                 annotations));
        }
    }

    /**
     * Returns port number from the port name. As many type of port can have
     * same port number, each port number will be prepended with a incrementing
     * number to make it unique in the list.
     *
     * @param portName port name
     * @return port number
     */
    private String getPortNum(String portName) {
        String port;
        if (!portName.contains(DELIMITER)) {
            portInc++;
            port = String.valueOf(portInc) + portName;
        } else if (portName.indexOf(DELIMITER) > 0) {
            try {
                port = portName.substring(
                        portName.lastIndexOf(DELIMITER) + 1);
                portInc++;
                port = String.valueOf(portInc) + port;
            } catch (StringIndexOutOfBoundsException e) {
                throw new IllegalArgumentException(P_NAME_INVALID);
            }
        } else {
            throw new IllegalArgumentException(P_NAME_INVALID);
        }
        return port;
    }

    /**
     * Returns port statistics information for a device.
     *
     * @param deviceId device for which port statistics to be fetched
     * @return port statistics
     */
    Collection<PortStatistics> parsePortsStatistics(DeviceId deviceId) {
        Collection<PortStatistics> pss = Lists.newArrayList();
        Iterator itr = getInterfaceIterator();
        while (itr.hasNext()) {
            Element ifElement = (Element) itr.next();
            pss.add(getPortStatistics(ifElement, deviceId));
        }
        return pss;
    }

    private PortStatistics getPortStatistics(Element ifElement, DeviceId id) {
        String ifType = ifElement.element(IF_TYPE).getText();

        DefaultPortStatistics.Builder builder = DefaultPortStatistics.builder();

        if (INTERFACES.contains(ifType)) {
            int port = Integer.parseInt(getPortNum(ifElement.element(IF_NUM)
                                                           .getText()));
            Element statInfo = ifElement.element(IF_STATS);
            long packetReceived = Long.parseLong(statInfo.element(P_RCVD)
                                                         .getText());
            long packetSent = Long.parseLong(statInfo.element(P_SENT).getText());
            long bytesReceived = Long.parseLong(statInfo.element(B_RCVD)
                                                        .getText());
            long bytesSent = Long.parseLong(statInfo.element(B_SENT).getText());
            long packetsRxDropped = Long.parseLong(statInfo.element(RX_DROP)
                                                           .getText());
            long packetsTxDropped = Long.parseLong(statInfo.element(TX_DROP)
                                                           .getText());
            long packetsRxErrors = Long.parseLong(statInfo.element(RX_ERROR)
                                                          .getText());
            long packetsTxErrors = Long.parseLong(statInfo.element(TX_ERROR)
                                                          .getText());

            return builder.setDeviceId(id)
                    .setPort(port)
                    .setPacketsReceived(packetReceived)
                    .setPacketsSent(packetSent)
                    .setBytesReceived(bytesReceived)
                    .setBytesSent(bytesSent)
                    .setPacketsRxDropped(packetsRxDropped)
                    .setPacketsRxErrors(packetsRxErrors)
                    .setPacketsTxDropped(packetsTxDropped)
                    .setPacketsTxErrors(packetsTxErrors).build();
        }
        return builder.build();
    }
}
