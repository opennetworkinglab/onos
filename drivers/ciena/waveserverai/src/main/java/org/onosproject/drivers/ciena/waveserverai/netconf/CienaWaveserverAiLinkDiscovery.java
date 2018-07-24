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

import org.onosproject.drivers.netconf.TemplateManager;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LinkDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.link.DefaultLinkDescription;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.ciena.waveserverai.netconf.CienaWaveserverAiDeviceDescription.portIdConvert;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Discovers the device and ports from a Ciena WaveServer Ai Netconf device.
 */

public class CienaWaveserverAiLinkDiscovery extends AbstractHandlerBehaviour
        implements LinkDiscovery {
    private static final TemplateManager TEMPLATE_MANAGER = new TemplateManager();

    private final Logger log = getLogger(getClass());

    public CienaWaveserverAiLinkDiscovery() {
        log.info("Loaded handler behaviour CienaWaveserverAiLinkDiscovery.");
    }

    static {
        TEMPLATE_MANAGER.load(CienaWaveserverAiLinkDiscovery.class,
                             "/templates/requests/%s.j2",
                             "getLinks", "discoverPortDetails");
    }

    @Override
    public Set<LinkDescription> getLinks() {
        log.debug("LINKS CHECKING ...");
        Set<LinkDescription> links = new HashSet<>();

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
            Map<String, Device> lookup = new HashMap<>();
            for (Device d : devices) {
                lookup.put(d.chassisId().toString(), d);
            }
            log.debug("MAP: {}", lookup);

            XPath xp = XPathFactory.newInstance().newXPath();
            Node node = TEMPLATE_MANAGER.doRequest(session, "discoverPortDetails");
            NodeList nodeList = (NodeList) xp.evaluate("waveserver-ports/ports", node, XPathConstants.NODESET);
            int count = nodeList.getLength();
            Node nodeListItem;
            for (int i = 0; i < count; i += 1) {
                Long portAsLong;
                Long destPortAsLong;
                String destChassis = null;
                String destPort = null;
                nodeListItem = nodeList.item(i);
                String port = xp.evaluate("port-id/text()", nodeListItem);
                portAsLong = portIdConvert(port);
                log.debug("CHECKING: {}", port);
                if (xp.evaluate("id/type/text()", nodeListItem).equals("otn")) {
                    String label = xp.evaluate("id/label/text()", nodeListItem);
                    final String r1 = "\\$\\{remote_mac:(.*?)\\}";
                    final Pattern p1 = Pattern.compile(r1);
                    final Matcher m1 = p1.matcher(label);
                    if (m1.find()) {
                        destChassis = m1.group(1).replaceFirst("^0+(?!$)", "");
                    }
                    final String r2 = "\\$\\{remote_port:(.*?)\\}";
                    final Pattern p2 = Pattern.compile(r2);
                    final Matcher m2 = p2.matcher(label);
                    if (m2.find()) {
                        destPort = m2.group(1);
                    }
                    destPortAsLong = portIdConvert(destPort);
                    if (destChassis != null && destPort != null) {
                        log.debug("LOOKING FOR OTN neighbor chassis: {}", destChassis);
                        Device dest = lookup.get(destChassis);
                        if (dest != null) {
                            links.add(new DefaultLinkDescription(
                                    new ConnectPoint(dest.id(),
                                            PortNumber.portNumber(destPortAsLong, destPort)),
                                    new ConnectPoint(deviceId,
                                            PortNumber.portNumber(portAsLong, port)),
                                    Link.Type.TUNNEL, true));
                        } else {
                            log.error("DEST OTN CHASSIS is NULL for {}", xp.evaluate("port-id/text()", nodeListItem));
                        }
                    } else {
                        log.error("NO LINK for {}", xp.evaluate("port-id/text()", nodeListItem));
                    }
                } else if (xp.evaluate("id/type/text()", nodeListItem).equals("ethernet")) {
                    Map<String, Object> templateContext = new HashMap<>();
                    templateContext.put("port-number", port);
                    node = TEMPLATE_MANAGER.doRequest(session, "getLinks", templateContext);
                    String chassisIdSubtype = xp.evaluate(
                            "waveserver-lldp/port/remote/chassis/chassis-id/chassis-id-subtype/text()", node);
                    if (chassisIdSubtype.equals("mac-address")) {
                        destChassis = xp.evaluate(
                                "waveserver-lldp/port/remote/chassis/chassis-id/chassis-id/text()",
                                node).trim().toLowerCase();
                        if (destChassis.startsWith("0x")) {
                            destChassis = destChassis.substring(2);
                        }
                    } else {
                        log.error("Unknown Chassis-id-subtype {}", xp.evaluate(
                                "waveserver-lldp/port/remote/chassis/chassis-id/chassis-id-subtype/text()", node));
                    }
                    destPort = xp.evaluate("waveserver-lldp/port/remote/port/id/id/text()", node);
                    destPortAsLong = Long.valueOf(destPort);

                    if (destChassis != null && !destPort.equals("")) {
                        log.debug("LOOKING FOR ethernet neighbor chassisId: {}", destChassis);
                        Device dest = lookup.get(destChassis);
                        if (dest != null) {
                            links.add(new DefaultLinkDescription(
                                    new ConnectPoint(deviceId,
                                                     PortNumber.portNumber(portAsLong, port)),
                                    new ConnectPoint(dest.id(),
                                                     PortNumber.portNumber(destPortAsLong, destPort)),
                                    Link.Type.TUNNEL, true));
                        } else {
                            log.debug("DEST CHASSIS is NULL for port {}", xp.evaluate("port-id/text()", nodeListItem));
                        }
                    } else {
                        log.debug("NO LINK for {}", xp.evaluate("port-id/text()", nodeListItem));
                    }
                }
            }
        } catch (NetconfException | XPathExpressionException e) {
            log.error("Unable to retrieve links for device {}, {}", deviceId, e);
        }
        return links;
    }

}
