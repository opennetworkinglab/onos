/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.drivers.ciena.c5162.netconf;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.onlab.packet.ChassisId;
import org.onosproject.drivers.netconf.TemplateManager;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LinkDiscovery;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Discovers the ports from a Ciena WaveServer Rest device.
 */
public class Ciena5162DeviceDescription extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery, PortStatisticsDiscovery, LinkDiscovery {
    private static final Logger log = getLogger(Ciena5162DeviceDescription.class);
    static final TemplateManager TEMPLATE_MANAGER = new TemplateManager();

    static {
        TEMPLATE_MANAGER.load(Ciena5162DeviceDescription.class, "/templates/requests/%s.j2", "systemInfo",
                "softwareVersion", "logicalPorts", "port-stats", "link-info");
    }

    @Override
    public DeviceDescription discoverDeviceDetails() {

        DeviceId deviceId = handler().data().deviceId();
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        NetconfSession session = controller.getDevicesMap().get(handler().data().deviceId()).getSession();
        try {
            Node systemInfo = TEMPLATE_MANAGER.doRequest(session, "systemInfo");
            Node softwareVersion = TEMPLATE_MANAGER.doRequest(session, "softwareVersion");
            XPath xp = XPathFactory.newInstance().newXPath();
            String mac = xp.evaluate("components/component/properties/property/state/value/text()", systemInfo)
                    .toUpperCase();
            return new DefaultDeviceDescription(deviceId.uri(), Device.Type.SWITCH,
                    xp.evaluate("components/component/state/mfg-name/text()", systemInfo),
                    xp.evaluate("components/component/state/name/text()", systemInfo),
                    xp.evaluate("software-state/running-package/package-version/text()", softwareVersion),
                    xp.evaluate("components/component/state/serial-no/text()", systemInfo),
                    new ChassisId(Long.valueOf(mac, 16)));

        } catch (XPathExpressionException | NetconfException ne) {
            log.error("failed to query system info from device {}", handler().data().deviceId(), ne);
        }

        return new DefaultDeviceDescription(deviceId.uri(), Device.Type.SWITCH, "Ciena", "5162", "Unknown", "Unknown",
                new ChassisId());
    }

    /**
     * Convert the specification of port speed in the of of #unit, i.e. {@10G} to MB
     * as represented by a Long.
     *
     * @param ps
     *            specification of port speed
     * @return port speed as MBs
     */
    private Long portSpeedToLong(String ps) {
        String value = ps.trim();
        StringBuilder digits = new StringBuilder();
        String unit = "";
        for (int i = 0; i < value.length(); i += 1) {
            final char c = value.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            } else {
                unit = value.substring(i).toUpperCase().trim();
                break;
            }
        }

        switch (unit) {
        case "G":
        case "GB":
            return Long.valueOf(digits.toString()) * 1000;
        case "M":
        case "MB":
        default:
            return Long.valueOf(digits.toString());
        }
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        List<PortDescription> ports = new ArrayList<PortDescription>();
        DeviceId deviceId = handler().data().deviceId();
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        if (controller == null || controller.getDevicesMap() == null
                || controller.getDevicesMap().get(deviceId) == null) {
            log.warn("NETCONF session to device {} not yet established, will be retried", deviceId);
            return ports;
        }
        NetconfSession session = controller.getDevicesMap().get(deviceId).getSession();

        try {
            Node logicalPorts = TEMPLATE_MANAGER.doRequest(session, "logicalPorts");
            XPath xp = XPathFactory.newInstance().newXPath();
            NodeList nl = (NodeList) xp.evaluate("interfaces/interface/config", logicalPorts, XPathConstants.NODESET);
            int count = nl.getLength();
            Node node;
            for (int i = 0; i < count; i += 1) {
                node = nl.item(i);
                if (xp.evaluate("type/text()", node).equals("ettp")) {
                    ports.add(DefaultPortDescription.builder()
                            .withPortNumber(PortNumber.portNumber(xp.evaluate("name/text()", node)))
                            .isEnabled(Boolean.valueOf(xp.evaluate("admin-status/text()", node)))
                            .portSpeed(portSpeedToLong(xp.evaluate("port-speed/text()", node))).type(Port.Type.PACKET)
                            .build());
                }
            }
        } catch (NetconfException | XPathExpressionException e) {
            log.error("Unable to retrieve port information for device {}, {}", deviceId, e);
        }
        return ports;
    }

    @Override
    public Collection<PortStatistics> discoverPortStatistics() {
        List<PortStatistics> stats = new ArrayList<PortStatistics>();

        DeviceId deviceId = handler().data().deviceId();
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        if (controller == null || controller.getDevicesMap() == null
                || controller.getDevicesMap().get(deviceId) == null) {
            log.warn("NETCONF session to device {} not yet established, will be retried", deviceId);
            return stats;
        }
        NetconfSession session = controller.getDevicesMap().get(deviceId).getSession();

        try {
            Node data = TEMPLATE_MANAGER.doRequest(session, "port-stats");
            XPath xp = XPathFactory.newInstance().newXPath();
            NodeList interfaces = (NodeList) xp.evaluate("interfaces/interface", data, XPathConstants.NODESET);
            int count = interfaces.getLength();
            for (int i = 0; i < count; i += 1) {
                Node iface = interfaces.item(i);
                if (xp.evaluate("config/type/text()", iface).equals("ettp")) {
                    stats.add(DefaultPortStatistics.builder().setDeviceId(deviceId)
                            .setPort(PortNumber.portNumber(xp.evaluate("name/text()", iface)))
                            .setBytesReceived(Long.valueOf(xp.evaluate("state/counters/in-octets/text()", iface)))
                            .setBytesSent(Long.valueOf(xp.evaluate("state/counters/out-octets/text()", iface)))
                            .setPacketsReceived(Long.valueOf(xp.evaluate("state/counters/in-pkts/text()", iface)))
                            .setPacketsSent(Long.valueOf(xp.evaluate("state/counters/out-pkts/text()", iface)))
                            .setPacketsTxErrors(Long.valueOf(xp.evaluate("state/counters/out-errors/text()", iface)))
                            .setPacketsRxErrors(Long.valueOf(xp.evaluate("state/counters/in-errors/text()", iface)))
                            .build());
                }
            }
        } catch (NetconfException | XPathExpressionException e) {
            log.error("Unable to retrieve port statistics for device {}, {}", deviceId, e);
        }

        return stats;
    }

    @Override
    public Set<LinkDescription> getLinks() {
        log.debug("LINKS CHECKING ...");
        Set<LinkDescription> links = new HashSet<LinkDescription>();
        DeviceId deviceId = handler().data().deviceId();
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        if (controller == null || controller.getDevicesMap() == null
                || controller.getDevicesMap().get(deviceId) == null) {
            log.warn("NETCONF session to device {} not yet established, cannot load links, will be retried", deviceId);
            return links;
        }
        NetconfSession session = controller.getDevicesMap().get(deviceId).getSession();
        try {

            DeviceService deviceService = this.handler().get(DeviceService.class);

            Iterable<Device> devices = deviceService.getAvailableDevices();
            Map<String, Device> lookup = new HashMap<String, Device>();
            for (Device d : devices) {
                lookup.put(d.chassisId().toString().toUpperCase(), d);
            }

            Node logicalPorts = TEMPLATE_MANAGER.doRequest(session, "link-info");
            XPath xp = XPathFactory.newInstance().newXPath();
            NodeList ifaces = (NodeList) xp.evaluate("interfaces/interface", logicalPorts, XPathConstants.NODESET);
            int count = ifaces.getLength();
            Node iface;
            Node destChassis;
            for (int i = 0; i < count; i += 1) {
                iface = ifaces.item(i);
                if (xp.evaluate("config/type/text()", iface).equals("ettp")) {
                    destChassis = (Node) xp.evaluate("state/lldp-remote-port-operational/chassis-id", iface,
                            XPathConstants.NODE);

                    if (destChassis != null) {
                        Device dest = lookup.get(destChassis.getTextContent().toUpperCase());

                        if (dest != null) {

                            links.add(new DefaultLinkDescription(
                                    new ConnectPoint(dest.id(),
                                            PortNumber.portNumber(xp.evaluate(
                                                    "state/lldp-remote-port-operational/port-id/text()", iface))),
                                    new ConnectPoint(deviceId,
                                            PortNumber.portNumber(xp.evaluate("name/text()", iface))),
                                    Link.Type.DIRECT, true));
                        } else {
                            log.warn("DEST chassisID not found: chassis {} port {}",
                                     destChassis.getTextContent().toUpperCase(), xp.evaluate("name/text()", iface));
                        }
                    } else {
                        log.debug("NO LINK for {}", xp.evaluate("name/text()", iface));
                    }
                }
            }
        } catch (NetconfException | XPathExpressionException e) {
            log.error("Unable to retrieve links for device {}, {}", deviceId, e);
        }

        return links;
    }

}
