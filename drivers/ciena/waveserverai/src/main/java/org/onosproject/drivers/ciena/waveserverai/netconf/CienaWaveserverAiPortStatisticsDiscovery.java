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
import org.onosproject.drivers.netconf.TemplateManager;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
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
import java.util.ArrayList;
import java.util.Collection;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.ciena.waveserverai.netconf.CienaWaveserverAiDeviceDescription.portIdConvert;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Discovers the device and ports from a Ciena WaveServer Ai Netconf device.
 */

public class CienaWaveserverAiPortStatisticsDiscovery extends AbstractHandlerBehaviour
        implements PortStatisticsDiscovery {
    private static final TemplateManager TEMPLATE_MANAGER = new TemplateManager();

    private final Logger log = getLogger(getClass());

    public CienaWaveserverAiPortStatisticsDiscovery() {
        log.info("Loaded handler behaviour CienaWaveserverAiPortStatisticsDiscovery.");
    }

    static {
        TEMPLATE_MANAGER.load(CienaWaveserverAiPortStatisticsDiscovery.class,
                             "/templates/requests/%s.j2",
                             "discoverPortStatistics");
    }

    @Override
    public Collection<PortStatistics> discoverPortStatistics() {
        log.debug("Calculating port stats for Waveserver Ai device");
        Collection<PortStatistics> portStats = new ArrayList<>();

        DeviceId deviceId = handler().data().deviceId();
        NetconfController controller = checkNotNull(handler().get(NetconfController.class));
        if (controller == null || controller.getDevicesMap() == null
                || controller.getDevicesMap().get(deviceId) == null) {
            log.warn("NETCONF session to device {} not yet established, cannot load links, will be retried", deviceId);
            return portStats;
        }
        NetconfSession session = controller.getDevicesMap().get(deviceId).getSession();

        try {
            XPath xp = XPathFactory.newInstance().newXPath();
            String tx = "current-bin/statistics/interface-counts/tx/";
            String rx = "current-bin/statistics/interface-counts/rx/";

            Node node = TEMPLATE_MANAGER.doRequest(session, "discoverPortStatistics");
            NodeList nodeList = (NodeList) xp.evaluate("waveserver-pm/ethernet-performance-instances",
                                                       node, XPathConstants.NODESET);
            Node nodeListItem;
            int count = nodeList.getLength();
            for (int i = 0; i < count; ++i) {
                nodeListItem = nodeList.item(i);
                portStats.add(DefaultPortStatistics.builder()
                          .setDeviceId(deviceId)
                          .setPort(PortNumber.portNumber(
                                  portIdConvert(xp.evaluate("instance-name/text()", nodeListItem))))
                          .setPacketsReceived(Long.parseLong(xp.evaluate(rx + "packets/value/text()", nodeListItem)))
                          .setPacketsSent(Long.parseLong(xp.evaluate(tx + "packets/value/text()", nodeListItem)))
                          .setBytesReceived(Long.parseLong(xp.evaluate(rx + "bytes/value/text()", nodeListItem)))
                          .setBytesSent(Long.parseLong(xp.evaluate(tx + "bytes/value/text()", nodeListItem)))
//                          .setPacketsRxDropped(packetsRxDropped)
//                          .setPacketsRxErrors(packetsRxErrors)
//                          .setPacketsTxDropped(packetsTxDropped)
//                          .setPacketsTxErrors(packetsTxErrors)
                          .build());
            }
        } catch (NetconfException | XPathExpressionException e) {
            log.error("Unable to retrieve port stats information for device {}, {}", deviceId, e);
        }

        return ImmutableList.copyOf(portStats);
    }

}
