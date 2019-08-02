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

import com.google.common.collect.ImmutableList;
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
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * Driver Implementation of the DeviceDescrption discovery for OpenConfig
 * terminal devices.
 *
 */
public class CassiniTerminalDeviceDiscoveryOld
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

    private static final Logger log = getLogger(CassiniTerminalDeviceDiscoveryOld.class);


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
     * Builds a request to get Device Components, config and operational data.
     *
     * @return A string with the Netconf RPC for a get with subtree rpcing based on
     *    /components/
     */
    private String getTerminalDeviceBuilder() {
        return filteredGetBuilder("<terminal-device xmlns='http://openconfig.net/yang/terminal-device'/>");
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
            if (session == null) {
                log.error("discoverPortDetails called with null session for {}", did());
                return ImmutableList.of();
            }

            CompletableFuture<String> fut = session.rpc(getTerminalDeviceBuilder());
            String rpcReply = fut.get();

            XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(rpcReply);
            xconf.setExpressionEngine(new XPathExpressionEngine());

            HierarchicalConfiguration logicalChannels = xconf.configurationAt("data/terminal-device/logical-channels");
            return parseLogicalChannels(logicalChannels);
        } catch (Exception e) {
            log.error("Exception discoverPortDetails() {}", did(), e);
            return ImmutableList.of();
        }
    }




    /**
     * Parses transceiver information from OpenConfig XML configuration.
     *
     * @param terminalDevice the XML document with components root.
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
    protected List<PortDescription> parseLogicalChannels(HierarchicalConfiguration terminalDevice) {
        return terminalDevice.configurationsAt("channel")
            .stream()
            .filter(channel -> !channel.getString("index", "unknown").equals("unknown"))
            .map(channel -> {
                try {
                    // Pass the root document for cross-reference
                    return parseLogicalChannel(channel);
                } catch (Exception e) {
                    return null;
                }
                })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }


     /**
      * Parses a component XML doc into a PortDescription.
      *
      * @param channel subtree to parse. It must be a component ot type PORT.
      *  case we need to check transceivers or optical channels.
      *
      * @return PortDescription or null if component does not have onos-index
      */
     private PortDescription parseLogicalChannel(
             HierarchicalConfiguration channel) {

         HierarchicalConfiguration config = channel.configurationAt("config");
         String logicalChannelIndex = config.getString("index");
         String description = config.getString("description");
         String rateClass = config.getString("rate-class");
         log.info("Parsing Component {} type {} rate {}", logicalChannelIndex, description, rateClass);

         Map<String, String> annotations = new HashMap<>();
         annotations.put(OdtnDeviceDescriptionDiscovery.OC_LOGICAL_CHANNEL, logicalChannelIndex);
         annotations.put(OdtnDeviceDescriptionDiscovery.OC_NAME, description);

         // Store all properties as port properties

         Pattern clientPattern = Pattern.compile("xe(\\d*)/1"); // e.g. xe1/1
         Pattern linePattern = Pattern.compile("oe(\\d*)/(\\d*)"); // e.g. oe1
         Matcher clientMatch = clientPattern.matcher(description);
         Matcher lineMatch = linePattern.matcher(description);

         Pattern portSpeedPattern = Pattern.compile("TRIB_RATE_([0-9.]*)G");
         Matcher portSpeedMatch = portSpeedPattern.matcher(rateClass);


         Builder builder = DefaultPortDescription.builder();

         if (portSpeedMatch.find()) {
             Long speed = Long.parseLong(portSpeedMatch.group(1));
             builder.portSpeed(speed * 1000);
         }

         if (clientMatch.find()) {
             Long num = Long.parseLong(clientMatch.group(1));
             Long portNum = 100 + num;
             String connectionId = "connection:" + num.toString();

             annotations.putIfAbsent(PORT_TYPE, OdtnPortType.CLIENT.value());
             annotations.putIfAbsent(ONOS_PORT_INDEX, portNum.toString());
             annotations.putIfAbsent(CONNECTION_ID, connectionId);

             builder.withPortNumber(PortNumber.portNumber(portNum));
             builder.type(Type.PACKET);

             builder.annotations(DefaultAnnotations.builder().putAll(annotations).build());
             return builder.build();

         } else if (lineMatch.find()) {
             Long num = (Long.parseLong(lineMatch.group(1)) - 1) * 2 + Long.parseLong(lineMatch.group(2));
             Long portNum = 200 + num;
             String connectionId = "connection:" + num.toString();

             annotations.putIfAbsent(PORT_TYPE, OdtnPortType.LINE.value());
             annotations.putIfAbsent(ONOS_PORT_INDEX, portNum.toString());
             annotations.putIfAbsent(CONNECTION_ID, connectionId);

             OchSignal signalId = OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, 1);
             return OchPortHelper.ochPortDescription(
                     PortNumber.portNumber(portNum),
                     true,
                     OduSignalType.ODU4, // TODO Client signal to be discovered
                     true,
                     signalId,
                     DefaultAnnotations.builder().putAll(annotations).build());

         } else {
             log.warn("Unexpected component description: {}", description);
             return null;
         }

     }
}
