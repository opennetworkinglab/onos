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

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.PortDescription;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static org.onosproject.net.AnnotationKeys.PORT_NAME;
import static org.onosproject.net.Port.Type.COPPER;
import static org.onosproject.net.PortNumber.portNumber;

/**
 * Created by root1 on 27/3/17.
 */
public class HuaweiXmlParser {

    private static final String DATA = "data";
    private static final String IFM = "ifm";
    private static final String IFS = "interfaces";
    private static final String IF = "interface";
    private static final String IF_TYPE = "ifPhyType";
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

    private static final String DEV_PARSE_ERR = "Unable to parse the received" +
            " xml reply for system details from the huawei device";
    private static final String INT_PARSE_ERR = "Unable to parse the received" +
            " xml reply for interface details from the huawei device";
    private static final String P_NAME_INVALID = "Invalid port name.";

    //TODO: All type of interfaces has to be added.
    private static final List INTERFACES = Arrays.asList(
            "MEth", "LoopBack", "Ethernet", "POS", "GigabitEthernet");

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
        Document doc;
        try {
            doc = DocumentHelper.parseText(xml);
        } catch (DocumentException e) {
            throw new IllegalArgumentException(INT_PARSE_ERR);
        }
        Element root = doc.getRootElement();
        Element parent = root.element(DATA).element(IFM).element(IFS);
        Iterator itr = parent.elementIterator(IF);

        while (itr.hasNext()) {
            Element ifElement = (Element) itr.next();
            addPorts(ifElement);
        }
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
}
