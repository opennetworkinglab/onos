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

package org.onosproject.drivers.odtn.openconfig;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.OchSignal;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.PortNumber;
import org.onosproject.net.CltSignalType;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.optical.device.OduCltPortHelper;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

import org.onlab.packet.ChassisId;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;

import org.onosproject.drivers.utilities.XmlConfigParser;

import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.PortDescription;

import org.onosproject.net.driver.AbstractHandlerBehaviour;

import org.onosproject.net.optical.device.OchPortHelper;

import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;

import com.google.common.collect.ImmutableList;

import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;


/**
 * Driver Implementation of the DeviceDescription discovery for OpenConfig terminal devices.
 *
 * As defined in OpenConfig each PORT component includes a subcomponent:
 * --- client ports have a subcomponent of type oc-platform-types:TRANSCEIVER
 * --- line   ports have a subcomponent of type oc-opt-types:OPTICAL_CHANNEL
 *
 * Tested with a model in which each port includes the following two properties:
 * --- odtn-port-type: can assume values "client" and "line"
 * --- onos-index: integer value
 *
 * Other assumptions:
 * --- The port name is in the format "port-xxx"
 * --- The subcomponent of type TRANSCEIVER has a name in the format "transceiver-xxx"
 * --- The subcomponent of type OPTICAL_CHANNEL has a name in the format "channel-xxx"
 * --- In the section <terminal-device><logical-channels> the channel with index xxx is associated to transceiver-xxx
 *
 * Where xxx is the value of the onos-index property (e.g., port-11801, transceiver-11801, channel-11801)
 *
 * See simplified example of a port component:
 *
 * //CHECKSTYLE:OFF
 * <component>
 *     <name>port-11801</name>
 *     <state>
 *         <name>port-11801</name>
 *         <type>oc-platform-types:PORT</type>
 *     </state>
 *     <properties>
 *         <property>
 *             <name>odtn-port-type</name>
 *             <state>
 *                 <name>odtn-port-type</name>
 *                 <value>client</value>
 *             </state>
 *         </property>
 *         <property>
 *             <name>onos-index</name>
 *             <state>
 *                 <name>onos-index</name>
 *                 <value>11801</value>
 *             </state>
 *             </property>
 *     </properties>
 *     <subcomponents>
 *         <subcomponent>
 *             <name>transceiver-11801</name>
 *             <state>
 *                 <name>transceiver-11801</name>
 *             </state>
 *         </subcomponent>
 *     </subcomponents>
 * </component>
 * <terminal-device>
 *     <logical-channels>
 *         <channel>
 *             <index>11801</index>
 *             <state>
 *                 <index>11801</index>
 *                 <description>Logical channel 11801</description>
 *                 <admin-state>DISABLED</admin-state>
 *                 <rate-class>oc-opt-types:TRIB_RATE_10G</rate-class>
 *                 <trib-protocol>oc-opt-types:PROT_10GE_LAN</trib-protocol>
 *                 <logical-channel-type>oc-opt-types:PROT_ETHERNET</logical-channel-type>
 *                 <loopback-mode>NONE</loopback-mode>
 *                 <test-signal>false</test-signal>
 *                 <link-state>UP</link-state>
 *             </state>
 *             <ingress>
 *                 <state>
 *                     <transceiver>transceiver-11801</transceiver>
 *                 </state>
 *             </ingress>
 *         </channel>
 *     <logical-channels>
 * <terminal-device>
 * //CHECKSTYLE:ON
 */
public class ClientLineTerminalDeviceDiscovery
    extends AbstractHandlerBehaviour
    implements OdtnDeviceDescriptionDiscovery, DeviceDescriptionDiscovery {

    private static final String RPC_TAG_NETCONF_BASE =
        "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

    private static final String OC_PLATFORM_TYPES_OPERATING_SYSTEM =
            "oc-platform-types:OPERATING_SYSTEM";

    private static final String RPC_CLOSE_TAG = "</rpc>";

    private static final String OC_PLATFORM_TYPES_TRANSCEIVER =
        "oc-platform-types:TRANSCEIVER";

    private static final String OC_PLATFORM_TYPES_PORT =
        "oc-platform-types:PORT";

    private static final String OC_TRANSPORT_TYPES_OPTICAL_CHANNEL =
        "oc-opt-types:OPTICAL_CHANNEL";

    private static final Logger log = getLogger(ClientLineTerminalDeviceDiscovery.class);


    /**
     * Returns the NetconfSession with the device for which the method was called.
     *
     * @param deviceId device indetifier
     *
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
     * Get the device instance for which the methods apply.
     *
     * @return The device instance
     */
    private Device getDevice() {
        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        Device device = deviceService.getDevice(did());
        return device;
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


    /**
     * Construct a String with a Netconf filtered get RPC Message.
     *
     * @param filter A valid XPath Expression with the filter to apply in the get
     * @return a String containing the RPC XML Document
     *
     * Note: server must support xpath capability.

     * <select=" /components/component[name='PORT-A-In-1']/properties/...
     * ...property[name='onos-index']/config/value" type="xpath"/>
     */
    private String xpathFilteredGetBuilder(String filter) {
        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);
        rpc.append("<get>");
        rpc.append("<filter type='xpath' select=\"");
        rpc.append(filter);
        rpc.append("\"/>");
        rpc.append("</get>");
        rpc.append(RPC_CLOSE_TAG);
        return rpc.toString();
    }


    /**
     * Builds a request to get Device details, operational data.
     *
     * @return A string with the Netconf RPC for a get with subtree rpcing based on
     *    /components/component/state/type being oc-platform-types:OPERATING_SYSTEM
     */
    private String getDeviceDetailsBuilder() {
        StringBuilder filter = new StringBuilder();
        filter.append("<components xmlns='http://openconfig.net/yang/platform'>");
        filter.append(" <component>");
        filter.append("  <state>");
        filter.append("   <type xmlns:oc-platform-types='http://openconfig.net/yang/platform-types'>");
        filter.append(OC_PLATFORM_TYPES_OPERATING_SYSTEM);
        filter.append("   </type>");
        filter.append("  </state>");
        filter.append(" </component>");
        filter.append("</components>");
        return filteredGetBuilder(filter.toString());
    }


    /**
     * Builds a request to get Device Components, config and operational data.
     *
     * @return A string with the Netconf RPC for a get with subtree rpcing based on
     *    /components/
     */
    private String getDeviceComponentsBuilder() {
        return filteredGetBuilder(
            "<components xmlns='http://openconfig.net/yang/platform'/>");
    }


    /**
     * Builds a request to get Device Ports, config and operational data.
     *
     * @return A string with the Netconf RPC for a get with subtree rpcing based on
     *    /components/component/state/type being oc-platform-types:PORT
     */
    private String getDevicePortsBuilder() {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<components xmlns='http://openconfig.net/yang/platform'>");
        rpc.append(" <component><state>");
        rpc.append("   <type xmlns:oc-platform-types='http://openconfig.net/");
        rpc.append("yang/platform-types'>oc-platform-types:PORT</type>");
        rpc.append(" </state></component>");
        rpc.append("</components>");
        return filteredGetBuilder(rpc.toString());
    }


    /**
     * Returns a DeviceDescription with Device info.
     *
     * @return DeviceDescription or null
     *
     * //CHECKSTYLE:OFF
     * <pre>{@code
     * <data>
     * <components xmlns="http://openconfig.net/yang/platform">
     *  <component>
     *   <state>
     *     <name>FIRMWARE</name>
     *     <type>oc-platform-types:OPERATING_SYSTEM</type>
     *     <description>CTTC METRO-HAUL Emulated OpenConfig TerminalDevice</description>
     *     <version>0.0.1</version>
     *   </state>
     *  </component>
     * </components>
     * </data>
     *}</pre>
     * //CHECKSTYLE:ON
     */
    @Override
    public DeviceDescription discoverDeviceDetails() {
        boolean defaultAvailable = true;
        SparseAnnotations annotations = DefaultAnnotations.builder().build();

        log.debug("ClientLineTerminalDeviceDiscovery::discoverDeviceDetails device {}", did());

        // Other option "OTN" or "OTHER", we use TERMINAL_DEVICE
        org.onosproject.net.Device.Type type =
            Device.Type.TERMINAL_DEVICE;

        // Some defaults
        String vendor       = "NOVENDOR";
        String serialNumber = "0xCAFEBEEF";
        String hwVersion    = "0.2.1";
        String swVersion    = "0.2.1";
        String chassisId    = "128";

        // Get the session,
        NetconfSession session = getNetconfSession(did());
        try {
            String reply = session.get(getDeviceDetailsBuilder());
            log.debug("REPLY to DeviceDescription {}", reply);

            // <rpc-reply> as root node, software hardare version requires openconfig >= 2018
            XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
            vendor       = xconf.getString("data.components.component.state.mfg-name", vendor);
            serialNumber = xconf.getString("data.components.component.state.serial-no", serialNumber);
            swVersion    = xconf.getString("data.components.component.state.software-version", swVersion);
            hwVersion    = xconf.getString("data.components.component.state.hardware-version", hwVersion);
        } catch (Exception e) {
                log.error("ClientLineTerminalDeviceDiscovery::discoverDeviceDetails - Failed to retrieve session {}",
                        did());
                throw new IllegalStateException(new NetconfException("Failed to retrieve version info.", e));
        }

        ChassisId cid = new ChassisId(Long.valueOf(chassisId, 10));

        log.info("Device retrieved details");
        log.info("VENDOR    {}", vendor);
        log.info("HWVERSION {}", hwVersion);
        log.info("SWVERSION {}", swVersion);
        log.info("SERIAL    {}", serialNumber);
        log.info("CHASSISID {}", chassisId);

        return new DefaultDeviceDescription(did().uri(),
                    type, vendor, hwVersion, swVersion, serialNumber,
                    cid, defaultAvailable, annotations);
    }



    /**
     * Returns a list of PortDescriptions for the device.
     *
     * @return a list of descriptions.
     *
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
            XPathExpressionEngine xpe = new XPathExpressionEngine();
            NetconfSession session = getNetconfSession(did());
            if (session == null) {
                log.error("discoverPortDetails called with null session for {}", did());
                return ImmutableList.of();
            }

            CompletableFuture<String> fut = session.rpc(getDeviceComponentsBuilder());
            String rpcReply = fut.get();

            XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(rpcReply);
            xconf.setExpressionEngine(xpe);

            log.debug("REPLY {}", rpcReply);
            HierarchicalConfiguration components = xconf.configurationAt("data/components");
            return parsePorts(components);
        } catch (Exception e) {
            log.error("Exception discoverPortDetails() {}", did(), e);
            return ImmutableList.of();
        }
    }

    /**
     * Parses port information from OpenConfig XML configuration.
     *
     * @param components the XML document with components root.
     * @return List of ports
     *
     * //CHECKSTYLE:OFF
     * <pre>{@code
     *   <components xmlns="http://openconfig.net/yang/platform">
     *     <component>....
     *     </component>
     *     <component>....
     *     </component>
     *   </components>
     * }</pre>
     * //CHECKSTYLE:ON
     */
    protected List<PortDescription> parsePorts(HierarchicalConfiguration components) {
        return components.configurationsAt("component").stream()
            .filter(component -> {
                    return !component.getString("name", "unknown").equals("unknown") &&
                    component.getString("state/type", "unknown").equals(OC_PLATFORM_TYPES_PORT);
                    })
            .map(component -> {
                try {
                    // Pass the root document for cross-reference
                    return parsePortComponent(component, components);
                } catch (Exception e) {
                    return null;
                }
            }
            )
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }


    /**
     * Checks if a given component has a subcomponent of a given type.
     *
     * @param component subtree to parse looking for subcomponents.
     * @param components the full components tree, to cross-ref in
     *  case we need to check (sub)components' types.
     *
     * @return true or false
     */
    private boolean hasSubComponentOfType(
            HierarchicalConfiguration component,
            HierarchicalConfiguration components,
            String type) {
        long count = component.configurationsAt("subcomponents/subcomponent")
            .stream()
            .filter(subcomponent -> {
                        String scName = subcomponent.getString("name");
                        StringBuilder sb = new StringBuilder("component[name='");
                        sb.append(scName);
                        sb.append("']/state/type");
                        String scType = components.getString(sb.toString(), "unknown");
                        return scType.equals(type);
                    })
            .count();
        return (count > 0);
    }


    /**
     * Checks if a given component has a subcomponent of type OPTICAL_CHANNEL.
     *
     * @param component subtree to parse
     * @param components the full components tree, to cross-ref in
     *  case we need to check transceivers or optical channels.
     *
     * @return true or false
     */
    private boolean hasOpticalChannelSubComponent(
            HierarchicalConfiguration component,
            HierarchicalConfiguration components) {
        return hasSubComponentOfType(component, components, OC_TRANSPORT_TYPES_OPTICAL_CHANNEL);
    }


    /**
     *  Checks if a given component has a subcomponent of type TRANSCEIVER.
     *
     * @param component subtree to parse
     * @param components the full components tree, to cross-ref in
     *  case we need to check transceivers or optical channels.
     *
     * @return true or false
     */
    private boolean hasTransceiverSubComponent(
            HierarchicalConfiguration component,
            HierarchicalConfiguration components) {
        return hasSubComponentOfType(component, components, OC_PLATFORM_TYPES_TRANSCEIVER);
    }


    /**
     * Parses a component XML doc into a PortDescription.
     *
     * @param component subtree to parse. It must be a component ot type PORT.
     * @param components the full components tree, to cross-ref in
     *  case we need to check transceivers or optical channels.
     *
     * @return PortDescription or null if component does not have onos-index
     */
    private PortDescription parsePortComponent(
            HierarchicalConfiguration component,
            HierarchicalConfiguration components) {
        Map<String, String> annotations = new HashMap<>();
        String name = component.getString("name");
        String type = component.getString("state/type");

        log.info("Parsing Component {} type {}", name, type);

        annotations.put(OdtnDeviceDescriptionDiscovery.OC_NAME, name);
        annotations.put(OdtnDeviceDescriptionDiscovery.OC_TYPE, type);

        // Store all properties as port properties
        component.configurationsAt("properties/property")
            .forEach(property -> {
                    String pn = property.getString("name");
                    String pv = property.getString("state/value");
                    annotations.put(pn, pv);
                    });

        // Assing an ONOS port number
        PortNumber portNum;
        if (annotations.containsKey(ONOS_PORT_INDEX)) {
            portNum = PortNumber.portNumber(Long.parseLong(annotations.get(ONOS_PORT_INDEX)));
        } else {
            log.warn("PORT {} does not include onos-index, hashing...", name);
            portNum = PortNumber.portNumber(name.hashCode());
        }
        log.debug("PORT {} number {}", name, portNum);

        // The heuristic to know if it is client or line side
        if (!annotations.containsKey(PORT_TYPE)) {
            if (hasTransceiverSubComponent(component, components)) {
                annotations.put(PORT_TYPE, OdtnPortType.CLIENT.value());
            } else if (hasOpticalChannelSubComponent(component, components)) {
                annotations.put(PORT_TYPE, OdtnPortType.LINE.value());
            }
        }

        // Build the port
        // NOTE: using portNumber(id, name) breaks things. Intent parsing, port resorce management, etc. There seems
        // to be an issue with resource mapping
        if (annotations.get(PORT_TYPE).equals(OdtnPortType.CLIENT.value())) {
            log.debug("PORT {} number {} added as CLIENT port", name, portNum);

            return OduCltPortHelper.oduCltPortDescription(portNum,
                    true,
                    CltSignalType.CLT_10GBE,
                    DefaultAnnotations.builder().putAll(annotations).build());
        }
        if (annotations.get(PORT_TYPE).equals(OdtnPortType.LINE.value())) {
            log.debug("PORT {} number {} added as LINE port", name, portNum);

            // TODO: To be configured
            OchSignal signalId = OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, 1);

            return OchPortHelper.ochPortDescription(
                    portNum, true,
                    OduSignalType.ODU4, // TODO Client signal to be discovered
                    true,
                    signalId,
                    DefaultAnnotations.builder().putAll(annotations).build());
        }
        log.error("PORT {} number {} is of UNKNOWN type", name, portNum);
        return null;
    }
}
