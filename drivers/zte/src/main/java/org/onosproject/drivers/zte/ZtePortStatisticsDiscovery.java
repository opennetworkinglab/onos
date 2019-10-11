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
 */
package org.onosproject.drivers.zte;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.commons.configuration.XMLConfiguration;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;

import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_NAME;
import static org.slf4j.LoggerFactory.getLogger;

public class ZtePortStatisticsDiscovery extends AbstractHandlerBehaviour
        implements PortStatisticsDiscovery {

    private static final Logger LOG = getLogger(ZtePortStatisticsDiscovery.class);

    @Override
    public Collection<PortStatistics> discoverPortStatistics() {
        DeviceId deviceId = handler().data().deviceId();
        LOG.debug("Discovering ZTE PortStatistics for device {}", deviceId);

        NetconfController controller = handler().get(NetconfController.class);

        if (null == controller) {
            LOG.error("Cannot find NetconfController");
            return null;
        }

        NetconfSession session = controller.getDevicesMap().get(deviceId).getSession();

        if (null == session) {
            LOG.error("No session available for device {}", deviceId);
            return null;
        }

        DeviceService deviceService = this.handler().get(DeviceService.class);
        List<Port> ports = deviceService.getPorts(deviceId);

        Collection<PortStatistics> portStatistics = Lists.newArrayList();

        ports.stream()
                .filter(Port::isEnabled)
                .filter(this::isClientPort)
                .forEach(port -> portStatistics.add(discoverSpecifiedPortStatistics(session, deviceId, port)));

        return portStatistics;
    }

    private boolean isClientPort(Port port) {
        String portName = port.annotations().value(OC_NAME);
        if (Strings.isNullOrEmpty(portName)) {
            return false;
        }
        String[] portInfos = portName.split("-");
        return portInfos.length == 4 && portInfos[3].startsWith("C");
    }

    private String getDataOfRpcReply(String rpcReply) {
        String data = null;
        int begin = rpcReply.indexOf("<data>");
        int end = rpcReply.lastIndexOf("</data>");
        if (begin != -1 && end != -1) {
            data = (String) rpcReply.subSequence(begin, end + "</data>".length());
        } else {
            data = rpcReply;
        }
        return data;
    }

    private PortStatistics discoverSpecifiedPortStatistics(NetconfSession session, DeviceId deviceId, Port port) {

        String portName = port.annotations().value(OC_NAME);
        String rpc = buildPortStatisticsRequest("INTERFACE" + portName.substring("PORT".length()));

        try {
            String reply = session.requestSync(rpc);
            XMLConfiguration cfg = (XMLConfiguration) XmlConfigParser.loadXmlString(getDataOfRpcReply(reply));
            DefaultPortStatistics.Builder builder = DefaultPortStatistics.builder();

            builder.setPort(port.number())
                    .setPacketsReceived(getInteger(cfg, "in-pks"))
                    .setPacketsSent(getInteger(cfg, "out-pkts"))
                    .setBytesReceived(getInteger(cfg, "in-octets"))
                    .setBytesSent(getInteger(cfg, "out-octets"))
                    .setPacketsRxDropped(getInteger(cfg, "in-fcs-errors"))
                    .setPacketsTxDropped(getInteger(cfg, "carrier-transitions"))
                    .setPacketsRxErrors(getInteger(cfg, "in-errors"))
                    .setPacketsTxErrors(getInteger(cfg, "out-errors"))
                    .setDeviceId(deviceId);

            return builder.build();
        } catch (NetconfException e) {
            LOG.error("ZTE device portStatistic request error.", e);
            return null;
        }
    }

    private String buildPortStatisticsRequest(String portName) {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
        rpc.append("<get>");
        rpc.append("<filter xmlns:ns0=\"urn:ietf:params:xml:ns:netconf:base:1.0\" ns0:type=\"subtree\">");
        rpc.append("<interfaces xmlns=\"http://openconfig.net/yang/interfaces\">");
        rpc.append("<interface>");
        rpc.append("<name>");
        rpc.append(portName);
        rpc.append("</name>");
        rpc.append("<state>");
        rpc.append("<counters>");
        rpc.append("</counters>");
        rpc.append("</state>");
        rpc.append("</interface>");
        rpc.append("</interfaces>");
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append("</rpc>");

        return rpc.toString();
    }

    private int getInteger(XMLConfiguration cfg, String item) {
        String numString = cfg.getString("interfaces.interface.state.counters." + item);
        if (Strings.isNullOrEmpty(numString)) {
            LOG.debug("Cannot get port statistic data for {}, set 0 as default.", item);
            return 0;
        }

        try {
            return Integer.parseInt(numString);
        } catch (NumberFormatException e) {
            LOG.warn("Cannot convert data for {}", item);
            return 0;
        }
    }
}
