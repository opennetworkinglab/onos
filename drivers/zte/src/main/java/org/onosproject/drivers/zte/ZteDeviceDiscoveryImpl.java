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

import com.google.common.collect.ImmutableList;
import com.google.common.io.CharSource;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.onlab.packet.ChassisId;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.Port.Type;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DefaultPortDescription.Builder;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.optical.device.OchPortHelper;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;
import org.onosproject.net.AnnotationKeys;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class ZteDeviceDiscoveryImpl
        extends AbstractHandlerBehaviour
        implements OdtnDeviceDescriptionDiscovery, DeviceDescriptionDiscovery {
    private final Logger log = getLogger(getClass());

    @Override
    public DeviceDescription discoverDeviceDetails() {
        DeviceId deviceId = handler().data().deviceId();
        log.info("Discovering ZTE device {}", deviceId);

        NetconfController controller = handler().get(NetconfController.class);
        NetconfSession session = controller.getDevicesMap().get(deviceId).getSession();

        String hwVersion = "ZTE hw";
        String swVersion = "ZTE sw";
        String serialNumber = "000000000000";

        try {
            String reply = session.requestSync(buildDeviceInfoRequest());
            XMLConfiguration cfg = (XMLConfiguration) XmlConfigParser.loadXmlString(getDataOfRpcReply(reply));
            hwVersion = cfg.getString("components.component.state.hardware-version");
            swVersion = cfg.getString("components.component.state.software-version");
            serialNumber = cfg.getString("components.component.state.serial-no");
        } catch (NetconfException e) {
            log.error("ZTE device discovery error.", e);
        }

        return new DefaultDeviceDescription(deviceId.uri(),
                Device.Type.OTN,
                "ZTE",
                hwVersion,
                swVersion,
                serialNumber,
                new ChassisId(1));
    }

    private String buildDeviceInfoRequest() {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
        rpc.append("<get>");
        rpc.append("<filter xmlns:ns0=\"urn:ietf:params:xml:ns:netconf:base:1.0\" ns0:type=\"subtree\">");
        rpc.append("<components xmlns=\"http://openconfig.net/yang/platform\">");
        rpc.append("<component>");
        rpc.append("<name>CHASSIS-1-1</name>");
        rpc.append("<state></state>");
        rpc.append("</component>");
        rpc.append("</components>");
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append("</rpc>");

        return rpc.toString();
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

    @Override
    public List<PortDescription> discoverPortDetails() {
        DeviceId deviceId = handler().data().deviceId();
        log.info("Discovering ZTE device ports {}", deviceId);

        NetconfController controller = handler().get(NetconfController.class);
        NetconfSession session = controller.getDevicesMap().get(deviceId).getSession();
        XMLConfiguration cfg = new XMLConfiguration();
        try {
            String reply = session.requestSync(buildPortDetailRequest(), 30);
            String data = getDataOfRpcReply(reply);
            if (data == null) {
                log.error("No valid response found from {}:\n{}", deviceId, reply);
                return ImmutableList.of();
            }
            cfg.load(CharSource.wrap(data).openStream());
            return discoverPorts(cfg);
        } catch (Exception e) {
            log.error("ZTE device port discovery error.", e);
        }

        return ImmutableList.of();
    }

    private String buildPortDetailRequest() {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">");
        rpc.append("<get>");
        rpc.append("<filter xmlns:ns0=\"urn:ietf:params:xml:ns:netconf:base:1.0\" ns0:type=\"subtree\">");
        rpc.append("<components xmlns=\"http://openconfig.net/yang/platform\">");
        rpc.append("</components>");
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append("</rpc>");

        return rpc.toString();
    }

    private List<PortDescription> discoverPorts(XMLConfiguration cfg) {
        cfg.setExpressionEngine(new XPathExpressionEngine());
        List<HierarchicalConfiguration> components = cfg.configurationsAt("components/component");
        return components.stream()
                .filter(this::isPortComponent)
                .map(this::toPortDescriptionInternal)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private boolean isPortComponent(HierarchicalConfiguration component) {
        String name = component.getString("name");
        String type = component.getString("state/type");

        return name != null && name.startsWith("PORT") && type != null
                && type.equals("openconfig-platform-types:PORT");
    }

    private PortDescription toPortDescriptionInternal(HierarchicalConfiguration component) {
        Map<String, String> annotations = new HashMap<>();
        String name = component.getString("name");
        String type = component.getString("state/type");

        annotations.put(OdtnDeviceDescriptionDiscovery.OC_NAME, name);
        annotations.put(OdtnDeviceDescriptionDiscovery.OC_TYPE, type);
        annotations.putIfAbsent(AnnotationKeys.PORT_NAME, name);

        // PORT-1-4-C1
        String[] textStr = name.split("-");

        // use different value of portNumber on the same equipment
        String portComponentIndex = textStr[textStr.length - 1];
        int slotIndex = Integer.parseInt(textStr[2]);
        int slotPortIndex = Integer.parseInt(portComponentIndex.substring(1));
        int portNumber = slotIndex * 10 + slotPortIndex;

        annotations.putIfAbsent(ONOS_PORT_INDEX, portComponentIndex);
        annotations.putIfAbsent(CONNECTION_ID, "connection:" + Integer.parseInt(portComponentIndex.substring(1)));

        if (portComponentIndex.charAt(0) == 'L') {
            // line
            annotations.putIfAbsent(PORT_TYPE, OdtnPortType.LINE.value());
            OchSignal signalId = OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, 1);
            return OchPortHelper.ochPortDescription(
                    PortNumber.portNumber(portNumber + 100L),
                    true,
                    OduSignalType.ODUC2,
                    true,
                    signalId,
                    DefaultAnnotations.builder().putAll(annotations).build());
        } else if (portComponentIndex.charAt(0) == 'C') {
            // client
            annotations.putIfAbsent(PORT_TYPE, OdtnPortType.CLIENT.value());
            Builder builder = DefaultPortDescription.builder();
            builder.withPortNumber(PortNumber.portNumber(portNumber))
                    .isEnabled(true)
                    .portSpeed(100000L)
                    .type(Type.PACKET)
                    .annotations(DefaultAnnotations.builder().putAll(annotations).build());

            return builder.build();
        }

        return null;
    }
}
