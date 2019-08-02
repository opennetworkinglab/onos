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
 *
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */

package org.onosproject.drivers.odtn;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.onlab.packet.ChassisId;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.optical.device.OchPortHelper;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * Driver Implementation of the DeviceDescrption discovery for OpenConfig
 * terminal devices.
 */
public class CassiniTerminalDeviceDiscovery
        extends AbstractHandlerBehaviour
        implements OdtnDeviceDescriptionDiscovery, DeviceDescriptionDiscovery {

    private static final String RPC_TAG_NETCONF_BASE =
            "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

    private static final String RPC_CLOSE_TAG = "</rpc>";

    private static final String OC_TRANSPORT_TYPES_OPTICAL_CHANNEL =
            "OPTICAL_CHANNEL";

    private static final Logger log = getLogger(CassiniTerminalDeviceDiscovery.class);


    /**
     * Returns the NetconfSession with the device for which the method was called.
     *
     * @param deviceId device indetifier
     * @return The netconf session or null
     */
    private NetconfSession getNetconfSession(DeviceId deviceId) {
        NetconfController controller = handler().get(NetconfController.class);
        NetconfDevice ncdev = controller.getDevicesMap().get(deviceId);
        if (ncdev == null) {
            log.trace("No netconf device, returning null session");
            return null;
        }
        return ncdev.getSession();
    }


    /**
     * Get the deviceId for which the methods apply.
     *
     * @return The deviceId as contained in the handler data
     */
    private DeviceId did() {
        return handler().data().deviceId();
    }


    /**
     * Construct a String with a Netconf filtered get RPC Message.
     *
     * @param filter A valid XML tree with the filter to apply in the get
     * @return a String containing the RPC XML Document
     */
    private String filteredGetBuilder(String filter) {
        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);
        rpc.append("<get>");
        rpc.append("<filter type='subtree'>");
        rpc.append(filter);
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append(RPC_CLOSE_TAG);
        return rpc.toString();
    }


    @Override
    public DeviceDescription discoverDeviceDetails() {
        return new DefaultDeviceDescription(handler().data().deviceId().uri(),
                Device.Type.TERMINAL_DEVICE,
                "EDGECORE",
                "Cassini",
                "OcNOS",
                "",
                new ChassisId("1"));
    }


    /**
     * Returns a list of PortDescriptions for the device.
     *
     * @return a list of descriptions.
     * <p>
     * The RPC reply follows the following pattern:
     * //CHECKSTYLE:OFF
     * <pre>{@code
     * <?xml version="1.0" encoding="UTF-8"?>
     * <rpc-reply xmlns="urn:ietf:params:xml:ns:netconf:base:1.0" message-id="7">
     * <data>
     *   <components xmlns="http://openconfig.net/yang/platform">
     *     <component>....
     *     </component>
     *     <component>....
     *     </component>
     *   </components>
     * </data>
     * </rpc-reply>
     * }</pre>
     * //CHECKSTYLE:ON
     */
    @Override
    public List<PortDescription> discoverPortDetails() {
        try {
            NetconfSession session = getNetconfSession(did());
            if (session == null) {
                log.error("discoverPortDetails called with null session for {}", did());
                return ImmutableList.of();
            }
            CompletableFuture<CharSequence> fut1 = session.asyncGet();
            String rpcReplyTest = fut1.get().toString();

            XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(rpcReplyTest);
            xconf.setExpressionEngine(new XPathExpressionEngine());

            HierarchicalConfiguration logicalChannels = xconf.configurationAt("components");
            return discoverPorts(logicalChannels);
        } catch (Exception e) {
            log.error("Exception discoverPortDetails() {}", did(), e);
            return ImmutableList.of();
        }
    }

    /**
     * Parses port information from OpenConfig XML configuration.
     *
     * @param cfg tree where the root node is {@literal <data>}
     * @return List of ports
     */
    @VisibleForTesting
    private List<PortDescription> discoverPorts(HierarchicalConfiguration cfg) {
        // If we want to use XPath
        cfg.setExpressionEngine(new XPathExpressionEngine());
        // converting components into PortDescription.
        List<HierarchicalConfiguration> components = cfg.configurationsAt("component");
        return components.stream()
                .map(this::toPortDescriptionInternal)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Converts Component subtree to PortDescription.
     *
     * @param component subtree to parse
     * @return PortDescription or null if component is not an ONOS Port
     */
    private PortDescription toPortDescriptionInternal(HierarchicalConfiguration component) {
        Map<String, String> annotations = new HashMap<>();
         /*
        <components xmlns="http://openconfig.net/yang/platform">
             <component>
                <name>oc1/0</name>
                 <config>
                    <name>oc1/0</name>
                 </config>
                 <state>
                    <name>oc1/0</name>
                 <type>OPTICAL_CHANNEL</type>
                <id/>
                <description/>
                <mfg-name/>
                <hardware-version/>
                <firmware-version/>
                <software-version/>
                <serial-no/>
                <part-no/>
                <removable>true</removable>
                <empty>false</empty>
                <parent/>
                <temperature>
                <instant>0.0</instant>
                <avg>0.0</avg>
                <min>0.0</min>
                <max>0.0</max>
                <interval>0</interval>
                <min-time>0</min-time>
                <max-time>0</max-time>
                <alarm-status>true</alarm-status>
                <alarm-threshold>0</alarm-threshold>
                </temperature>
                <memory>
                    <available>0</available>
                    <utilized>0</utilized>
                </memory>
                <allocated-power>0</allocated-power>
                <used-power>0</used-power>
            </state>
            <optical-channel xmlns="http://openconfig.net/yang/terminal-device">
                <config>
                    <line-port>port-10101</line-port>
                </config>
            <state>
                <output-power/>
                <input-power/>
            </state>
            </optical-channel>
        </component>
        */
        String name = component.getString("name");
        String type = component.getString("state/type");
        checkNotNull(name, "name not found");
        checkNotNull(type, "state/type not found");
        annotations.put(OdtnDeviceDescriptionDiscovery.OC_NAME, name);
        annotations.put(OdtnDeviceDescriptionDiscovery.OC_TYPE, type);

        //TODO this currently support only line-side ports through parsing of optical channels.
        if (type.equals(OC_TRANSPORT_TYPES_OPTICAL_CHANNEL)) {
            String portName = component.getString("optical-channel/config/line-port");
            String portIndex = portName.split("-")[1];
            annotations.putIfAbsent(AnnotationKeys.PORT_NAME, portName);
            annotations.putIfAbsent(PORT_TYPE, OdtnPortType.LINE.value());
            annotations.putIfAbsent(ONOS_PORT_INDEX, portIndex);
            annotations.putIfAbsent(CONNECTION_ID, "connection-" + portIndex);

            OchSignal signalId = OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, 1);
            return OchPortHelper.ochPortDescription(
                    PortNumber.portNumber(Long.parseLong(portIndex)),
                    true,
                    OduSignalType.ODU4, // TODO Client signal to be discovered
                    true,
                    signalId,
                    DefaultAnnotations.builder().putAll(annotations).build());

        } else {
            log.debug("Unknown port component type {}", type);
            return null;
        }
    }
}
