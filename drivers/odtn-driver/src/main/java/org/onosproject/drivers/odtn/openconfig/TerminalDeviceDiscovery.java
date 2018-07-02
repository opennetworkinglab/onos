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

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DefaultPortDescription.Builder;
import org.onosproject.net.device.PortDescription;

import org.onosproject.net.driver.AbstractHandlerBehaviour;

import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.Port.Type;
import org.onosproject.net.PortNumber;

import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;

import com.google.common.collect.ImmutableList;

import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;


/**
 * Driver Implementation of the DeviceDescrption discovery for OpenConfig
 * terminal devices.
 *
 */
public class TerminalDeviceDiscovery
    extends AbstractHandlerBehaviour
    implements OdtnDeviceDescriptionDiscovery, DeviceDescriptionDiscovery {

    private static final String RPC_TAG_NETCONF_BASE =
        "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

    private static final String RPC_CLOSE_TAG = "</rpc>";

    private static final String OC_PLATFORM_TYPES_TRANSCEIVER =
        "oc-platform-types:TRANSCEIVER";

    private static final String OC_PLATFORM_TYPES_PORT =
        "oc-platform-types:PORT";

    private static final String OC_TRANSPORT_TYPES_OPTICAL_CHANNEL =
        "oc-opt-types:OPTICAL_CHANNEL";

    private static final Logger log = getLogger(TerminalDeviceDiscovery.class);


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
        filter.append("   <type xmlns:oc-platform-types='http://openconfig.net/");
        filter.append("yang/platform-types'>oc-platform-types:OPERATING_SYSTEM</type>");
        filter.append("  </state>");
        filter.append(" </component>");
        filter.append("</components>");
        return filteredGetBuilder(filter.toString());
        /* I am not sure the alternative method is more efficient
           try {
           DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
           DocumentBuilder db = dbf.newDocumentBuilder();
           Document doc = db.newDocument();
           Element rpc = doc.createElementNS("urn:ietf:params:xml:ns:netconf:base:1.0", "rpc");
           Element get = doc.createElement("get");
           Element rpc = doc.createElement("rpc");
           Element components = doc.createElementNS("http://openconfig.net/yang/platform", "components");
           Element component = doc.createElement("component");
           Element state = doc.createElement("state");
           Element type  = doc.createElement("type");
           type.setAttributeNS("http://www.w3.org/2000/xmlns/",
           "xmlns:oc-platform-types", "http://openconfig.net/yang/platform-types");
           type.appendChild(doc.createTextNode("oc-platform-types:OPERATING_SYSTEM"));
           state.appendChild(type);
           component.appendChild(state);
           components.appendChild(component);
           rpc.appendChild(components);
           get.appendChild(rpc);
           rpc.appendChild(get);
           doc.appendChild(rpc);
           return NetconfRpcParserUtil.toString(doc);
           } catch (Exception e) {
           throw new IllegalStateException(new NetconfException("Exception in getDeviceDetailsBuilder", e));
           }
         */
    }


    /**
     * Builds a request to get Device Components, config and operational data.
     *
     * @return A string with the Netconf RPC for a get with subtree rpcing based on
     *    /components/
     */
    private String getDeviceComponentsBuilder() {
        return filteredGetBuilder("<components xmlns='http://openconfig.net/yang/platform'/>");
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
        log.info("TerminalDeviceDiscovery::discoverDeviceDetails device {}", did());
        boolean defaultAvailable = true;
        SparseAnnotations annotations = DefaultAnnotations.builder().build();

        // Other option "OTHER", we use ROADM for now
        org.onosproject.net.Device.Type type =
            org.onosproject.net.Device.Type.ROADM;

        // Some defaults
        String vendor       = "NOVENDOR";
        String hwVersion    = "0.1.1";
        String swVersion    = "0.1.1";
        String serialNumber = "0xCAFEBEEF";
        String chassisId    = "128";

        // Get the session,
        NetconfSession session = getNetconfSession(did());
        if (session != null) {
            try {
                String reply = session.get(getDeviceDetailsBuilder());
                // <rpc-reply> as root node
                XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
                vendor       = xconf.getString("data/components/component/state/mfg-name", vendor);
                serialNumber = xconf.getString("data/components/component/state/serial-no", serialNumber);
                // Requires OpenConfig >= 2018
                swVersion    = xconf.getString("data/components/component/state/software-version", swVersion);
                hwVersion    = xconf.getString("data/components/component/state/hardware-version", hwVersion);
            } catch (Exception e) {
                throw new IllegalStateException(new NetconfException("Failed to retrieve version info.", e));
            }
        } else {
            log.info("TerminalDeviceDiscovery::discoverDeviceDetails - No netconf session for {}", did());
        }
        log.info("VENDOR    {}", vendor);
        log.info("HWVERSION {}", hwVersion);
        log.info("SWVERSION {}", swVersion);
        log.info("SERIAL    {}", serialNumber);
        log.info("CHASSISID {}", chassisId);
        ChassisId cid = new ChassisId(Long.valueOf(chassisId, 10));
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
            NetconfSession session = getNetconfSession(did());
            /*
            Note: the method may get called before the netconf session is established
            2018-05-24 14:01:43,607 | INFO
            event NetworkConfigEvent{time=2018-05-24T14:01:43.602Z, type=CONFIG_ADDED, ....
            configClass=class org.onosproject.netconf.config.NetconfDeviceConfig

            2018-05-24 14:01:43,623 | INFO  | vice-installer-2 | TerminalDeviceDiscovery
            TerminalDeviceDiscovery::discoverPortDetails netconf:127.0.0.1:830

            2018-05-24 14:01:43,624 | ERROR | vice-installer-2 | TerminalDeviceDiscovery
            org.onosproject.onos-drivers-metrohaul - 1.14.0.SNAPSHOT | Exception discoverPortDetails()

            2018-05-24 14:01:43,631 | INFO  | vice-installer-1 | NetconfControllerImpl
            Creating NETCONF session to netconf:127.0.0.1:830 with apache-mina
             */
            if (session == null) {
                log.error("discoverPortDetails called with null session for {}", did());
                return ImmutableList.of();
            }

            CompletableFuture<String> fut = session.rpc(getDeviceComponentsBuilder());
            String rpcReply = fut.get();

            XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(rpcReply);
            xconf.setExpressionEngine(new XPathExpressionEngine());

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
        return components.configurationsAt("component")
            .stream()
            .filter(component -> {
                    return !component.getString("name", "unknown")
                            .equals("unknown") &&
                        component.getString("state/type", "unknown")
                            .equals(OC_PLATFORM_TYPES_PORT);
                    })
            .map(component -> {
                try {
                    // Pass the root document for cross-reference
                    return parsePortComponent(component, components);
                } catch (Exception e) {
                    return null;
                }
                })
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
         return hasSubComponentOfType(component, components,
                 OC_TRANSPORT_TYPES_OPTICAL_CHANNEL);
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
         return hasSubComponentOfType(component, components,
                 OC_PLATFORM_TYPES_TRANSCEIVER);
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

         if (!annotations.containsKey(ONOS_PORT_INDEX)) {
             log.warn("DEBUG: PORT {} does not include onos-index, skipping", name);
             return null;
         }

         // The heuristic to know if it is client or line side
         if (!annotations.containsKey(PORT_TYPE)) {
             if (hasTransceiverSubComponent(component, components)) {
                 annotations.put(PORT_TYPE, OdtnPortType.CLIENT.value());
             } else if (hasOpticalChannelSubComponent(component, components)) {
                 annotations.put(PORT_TYPE, OdtnPortType.LINE.value());
             }
         }

         // Build the port
         Builder builder = DefaultPortDescription.builder();
         builder.withPortNumber(PortNumber.portNumber(
                     Long.parseLong(annotations.get(ONOS_PORT_INDEX)), name));
         if (annotations.get(PORT_TYPE)
                 .equals(OdtnPortType.CLIENT.value())) {
             log.info("Adding CLIENT port");
             builder.type(Type.PACKET);
         } else if (annotations.get(PORT_TYPE)
                 .equals(OdtnPortType.LINE.value())) {
             log.info("Adding LINE port");
             builder.type(Type.OCH);
         } else {
             log.info("Unknown port added as CLIENT port");
         }
         builder.annotations(DefaultAnnotations.builder().putAll(annotations).build());
         return builder.build();
     }
}
