/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.drivers.ciena.waveserverai.netconf;

import com.google.common.collect.ImmutableList;
import org.onlab.packet.ChassisId;
import org.onosproject.drivers.netconf.TemplateManager;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Discovers the device and ports from a Ciena WaveServer Ai Netconf device.
 */

public class CienaWaveserverAiDeviceDescription extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {
    private static final TemplateManager TEMPLATE_MANAGER = new TemplateManager();

    private final Logger log = getLogger(getClass());

    public CienaWaveserverAiDeviceDescription() {
        log.info("Loaded handler behaviour CienaWaveserverAiDeviceDescription.");
    }

    static {
        TEMPLATE_MANAGER.load(CienaWaveserverAiDeviceDescription.class,
                             "/templates/requests/%s.j2",
                             "discoverDeviceDetails", "discoverPortDetails");
    }

    @Override
    public DeviceDescription discoverDeviceDetails() {
        log.debug("Adding description for Waveserver Ai device");

        NetconfSession session = getNetconfSession();
        Device device = getDevice(handler().data().deviceId());

        try {
            XPath xp = XPathFactory.newInstance().newXPath();

            Node node = TEMPLATE_MANAGER.doRequest(session, "discoverDeviceDetails");
            String chassisId = xp.evaluate("waveserver-chassis/mac-addresses/chassis/base/text()", node);
            chassisId = chassisId.replace(":", "");
            SparseAnnotations annotationDevice = DefaultAnnotations.builder()
                    .set("name", xp.evaluate("waveserver-system/host-name/current-host-name/text()", node))
                    .build();
            return new DefaultDeviceDescription(device.id().uri(),
                                                Device.Type.OTN,
                                                "Ciena",
                                                "WaverserverAi",
                                                xp.evaluate("waveserver-software/status/active-version/text()",
                                                            node),
                                                xp.evaluate("waveserver-chassis/identification/serial-number/text()",
                                                            node),
                                                new ChassisId(Long.valueOf(chassisId, 16)),
                                                (SparseAnnotations) annotationDevice);
        } catch (NetconfException | XPathExpressionException e) {
            log.error("Unable to retrieve device information for device {}, {}", device.chassisId(), e);
        }

        return new DefaultDeviceDescription(device.id().uri(), Device.Type.OTN, "Ciena", "WaverserverAi", "unknown",
                                            "unknown", device.chassisId());
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        log.info("Adding ports for Waveserver Ai device");
        List<PortDescription> ports = new ArrayList<>();

        Device device = getDevice(handler().data().deviceId());
        NetconfSession session = getNetconfSession();

        try {
            XPath xp = XPathFactory.newInstance().newXPath();
            Node nodeListItem;

            Node node = TEMPLATE_MANAGER.doRequest(session, "discoverPortDetails");
            NodeList nodeList = (NodeList) xp.evaluate("waveserver-ports/ports", node, XPathConstants.NODESET);
            int count = nodeList.getLength();
            for (int i = 0; i < count; ++i) {
                nodeListItem = nodeList.item(i);
                DefaultAnnotations annotationPort = DefaultAnnotations.builder()
                        .set(AnnotationKeys.PORT_NAME, xp.evaluate("port-id/text()", nodeListItem))
                        .set(AnnotationKeys.PROTOCOL, xp.evaluate("id/type/text()", nodeListItem))
                        .build();
                String port = xp.evaluate("port-id/text()", nodeListItem);
                ports.add(DefaultPortDescription.builder()
                          .withPortNumber(PortNumber.portNumber(
                                  portIdConvert(port), port))
                          .isEnabled(portStateConvert(
                                  xp.evaluate("state/operational-state/text()", nodeListItem)))
                          .portSpeed(portSpeedToLong(xp.evaluate("id/speed/text()", nodeListItem)))
                          .type(Port.Type.PACKET)
                          .annotations(annotationPort)
                          .build());
            }
        } catch (NetconfException | XPathExpressionException e) {
            log.error("Unable to retrieve port information for device {}, {}", device.chassisId(), e);
        }
        return ImmutableList.copyOf(ports);
    }

    /**
     * Returns the Device of the deviceId.
     *
     * @return device
     */
    private Device getDevice(DeviceId deviceId) {
        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        return deviceService.getDevice(deviceId);
    }

    /**
     * Returns the NETCONF session of the device.
     *
     * @return session
     */
    private NetconfSession getNetconfSession() {
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfDevice ncDevice = controller.getDevicesMap().get(handler().data().deviceId());
        if (ncDevice == null) {
            log.error(
                    "Internal ONOS Error. Device has been marked as reachable, "
                            + "but deviceID {} is not in Devices Map. Continuing with empty description",
                    handler().data().deviceId());
        }
        return controller.getDevicesMap().get(handler().data().deviceId())
                .getSession();
    }

    /**
     * Convert port speed in Gbps tp port speed in Mbps.
     *
     * @param speed
     *            port speed as string
     * @return port speed
     */
    public static Long portSpeedToLong(String speed) {
        double d = Double.parseDouble(speed);
        return (new Double(d * 1000)).longValue();
    }

    /**
     * Convert port operational state to Boolean.
     *
     * @param state
     *            port state as string
     * @return port state
     */
    public static Boolean portStateConvert(String state) {
        switch (state) {
            case "up":
                return true;
            case "enabled":
                return true;
            case "disabled":
                return false;
            case "down":
                return false;
            default:
                return false;
        }
    }

    /**
     * Convert port operational state to Boolean.
     *
     * @param state
     *            port state as Boolean
     * @return port state
     */
    public static String portStateConvert(Boolean state) {
        if (state) {
            return "enabled";
        } else {
            return "disabled";
        }
    }

    /**
     * Convert port name to port id.
     *
     * @param name
     *            port name as String
     * @return port id
     */
    public static Long portIdConvert(String name) {
        String result = "1";
        String replaceString = name.replace("-Ethernet", "");
        String[] arrOfStr = replaceString.split("-");
        if (arrOfStr[0].length() == 1) {
            result += "0" + arrOfStr[0];
        } else {
            result += arrOfStr[0];
        }
        if (arrOfStr[1].length() == 1) {
            result += "0" + arrOfStr[1];
        } else {
            result += arrOfStr[1];
        }
        return Long.valueOf(result);
    }
    /**
     * Convert port id to port name.
     *
     * @param id
     *            port id as Long
     * @return port name as String
     */
    public static String portIdConvert(Long id) {
        Integer pre = Integer.parseInt(id.toString().substring(1, 3));
        Integer post = Integer.parseInt(id.toString().substring(3, 5));
        return pre.toString() + "-" + post.toString();
    }

}
