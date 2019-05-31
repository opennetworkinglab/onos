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

 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */
package org.onosproject.drivers.odtn.openroadm;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.onlab.packet.ChassisId;
import org.onlab.util.Frequency;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.intent.OpticalPathIntent;
import org.onosproject.net.optical.device.OchPortHelper;
import org.onosproject.net.optical.device.OmsPortHelper;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;

import java.util.concurrent.ExecutionException;
/**
 * Driver Implementation of the DeviceDescrption discovery for OpenROADM.
 */
public class OpenRoadmDeviceDescription extends OpenRoadmNetconfHandlerBehaviour
  implements DeviceDescriptionDiscovery {

    // These annotations are added to the device and ports
    public final class AnnotationKeys {
        public static final String OPENROADM_NODEID = "openroadm-node-id";
        public static final String OPENROADM_CIRCUIT_PACK_NAME =
          "openroadm-circuit-pack-name";
        public static final String OPENROADM_PORT_NAME = "openroadm-port-name";
        public static final String OPENROADM_PARTNER_CIRCUIT_PACK_NAME =
          "openroadm-partner-circuit-pack-name";
        public static final String OPENROADM_PARTNER_PORT_NAME =
          "openroadm-partner-port-name";
        public static final String OPENROADM_LOGICAL_CONNECTION_POINT =
          "openroadm-logical-connection-point";
        private AnnotationKeys() {
            // utility class
        }
    }

    public static final ChannelSpacing CHANNEL_SPACING =
      ChannelSpacing.CHL_50GHZ;


    /*
     * The following 2 values are not specified by the OpenROADM standard,
     * but they are a reasonable default for a tunable C-band, defined from
     * Channel C1 at 191.35 to C96 at 196.10 GHz (for a spacing at 50GHz)
     */
    public static final Frequency START_CENTER_FREQ = Frequency.ofGHz(191_350);
    public static final Frequency STOP_CENTER_FREQ = Frequency.ofGHz(196_100);


    public static final String OPENROADM_DEVICE_OPEN = //
      "<org-openroadm-device xmlns=\"http://org/openroadm/device\">";
    public static final String OPENROADM_DEVICE_CLOSE = //
      "</org-openroadm-device>";


    /**
     * Builds a request to get OpenRoadm Device main node (within root).
     *
     *  @param nodeTag the tag with the name to get e.g. <info/>
     *
     * @return A string with the Netconf RPC for a get with subtree info
     */
    private String getDeviceXmlNodeBuilder(final String nodeTag) {
        StringBuilder filter = new StringBuilder();
        filter.append(OPENROADM_DEVICE_OPEN);
        filter.append(nodeTag);
        filter.append(OPENROADM_DEVICE_CLOSE);
        return filteredGetBuilder(filter.toString());
    }

    /**
     * Builds a request to get Device details (<info>).
     *
     * @return A string with the Netconf RPC for a get with subtree info
     */
    private String getDeviceDetailsBuilder() {
        return getDeviceXmlNodeBuilder("<info/>");
    }

    /**
     * Builds a request to get Ports data (<circuit-packs>).
     *
     * @return A string with the Netconf RPC
     */
    private String getDeviceCircuitPacksBuilder() {
        return getDeviceXmlNodeBuilder("<circuit-packs/>");
    }

    /**
     * Builds a request to get External Links data (<external-link>).
     *
     * @return A string with the Netconf RPC
     */
    private String getDeviceExternalLinksBuilder() {
        return getDeviceXmlNodeBuilder("<external-link/>");
    }

    /**
     * Builds a request to get Device Degrees, config and operational data.
     *
     * @return A string with the Netconf RPC for a get with subtree rpcing based
     * on /components/component/state/type being oc-platform-types:PORT
     */
    private String getDeviceDegreesBuilder() {
        return getDeviceXmlNodeBuilder("<degree/>");
    }

    /**
     * Builds a request to get Device SharedRiskGroups, config and operational
     * data.
     *
     * @return A string with the Netconf RPC for a get with subtree
     */
    private String getDeviceSharedRiskGroupsBuilder() {
        return getDeviceXmlNodeBuilder("<shared-risk-group/>");
    }

    /**
     * Builds a request to get Ports data.
     * Changed to XPath and added one based on classic filters since some agents
     * do not support xpath filtering.
     *
     * @return A string with the Netconf RPC
     */
    private String getDeviceExternalPortsBuilderXPath() {
        StringBuilder filter = new StringBuilder();
        filter.append(
          "/org-openroadm-device/circuit-packs/ports[port-qual='roadm-external']");
        return xpathFilteredGetBuilder(filter.toString());
    }

    /**
     * Builds a request to get Ports data.
     *
     * @return A string with the Netconf RPC
     */
    private String getDeviceExternalPortsBuilder() {
        StringBuilder filter = new StringBuilder();
        filter.append(OPENROADM_DEVICE_OPEN);
        filter.append("<circuit-packs>");
        filter.append(" <ports>");
        filter.append("  <port-qual>roadm-external</port-qual>");
        filter.append(" </ports>");
        filter.append("</circuit-packs>");
        filter.append(OPENROADM_DEVICE_CLOSE);
        return filteredGetBuilder(filter.toString());
    }

    /**
     * Builds a request to get External Links data.
     *
     * @return A string with the Netconf RPC
     */
    private String getDeviceExternalLinksBuilderXpath() {
        StringBuilder filter = new StringBuilder();
        filter.append("/org-openroadm-device/external-link");
        return xpathFilteredGetBuilder(filter.toString());
    }

    /**
     * Builds a request to get External Links data.
     *
     * @param nodeId OpenROADM node identifier.
     * @param circuitPackName name of the circuit part of the port.
     * @param portName name of the port.
     * @return A string with the Netconf RPC
     */
    private String getDeviceExternalLinkForPortBuilderXPath(
      String nodeId, String circuitPackName, String portName) {
        StringBuilder filter = new StringBuilder();
        filter.append("/org-openroadm-device/external-link[");
        filter.append("./source/node-id='");
        filter.append(nodeId);
        filter.append("' and ");
        filter.append("./source/circuit-pack-name='");
        filter.append(circuitPackName);
        filter.append("' and ");
        filter.append("./source/port-name='");
        filter.append(portName);
        filter.append("']");
        return xpathFilteredGetBuilder(filter.toString());
    }

    private String getDeviceExternalLinkForPortBuilder(String nodeId,
                                                       String circuitPackName,
                                                       String portName) {
        StringBuilder filter = new StringBuilder();
        filter.append(OPENROADM_DEVICE_OPEN);
        filter.append("<external-link>");
        filter.append(" <source>");
        filter.append("  <node-id>");
        filter.append(nodeId);
        filter.append("</node-id>");
        filter.append("  <circuit-pack-name>");
        filter.append(circuitPackName);
        filter.append("</circuit-pack-name>");
        filter.append("  <port-name>");
        filter.append(portName);
        filter.append("</port-name>");
        filter.append(" </source>");
        filter.append("</external-link>");
        filter.append(OPENROADM_DEVICE_CLOSE);
        return xpathFilteredGetBuilder(filter.toString());
    }


    /**
     * Returns a DeviceDescription with Device info.
     *
     * @return DeviceDescription or null
     */
    @Override
    public DeviceDescription discoverDeviceDetails() {
        boolean defaultAvailable = true;
        NetconfDevice ncDevice = getNetconfDevice();
        if (ncDevice == null) {
            log.error("ONOS Error: Device reachable, deviceID {} is not in Map", did());
            return null;
        }
        DefaultAnnotations.Builder annotationsBuilder =
          DefaultAnnotations.builder();

        // Some defaults
        String vendor = "UNKNOWN";
        String hwVersion = "2.2.0";
        String swVersion = "2.2.0";
        String serialNumber = "0x0000";
        String chassisId = "0";
        String nodeType = "rdm";

        // Get the session, if null, at least we can use the defaults.
        NetconfSession session = getNetconfSession(did());
        if (session != null) {
            try {
                String reply = session.rpc(getDeviceDetailsBuilder()).get();
                XMLConfiguration xconf =
                  (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
                String nodeId =
                  xconf.getString("data.org-openroadm-device.info.node-id", "");
                if (nodeId.equals("")) {
                    log.error("[OPENROADM] {} org-openroadm-device node-id undefined, returning", did());
                    return null;
                }
                annotationsBuilder.set(AnnotationKeys.OPENROADM_NODEID, nodeId);
                nodeType = xconf.getString("data.org-openroadm-device.info.node-type", "");
                if (nodeType.equals("")) {
                    log.error("[OPENROADM] {} empty node-type", did());
                    return null;
                }
                vendor = xconf.getString(
                  "data.org-openroadm-device.info.vendor", vendor);
                hwVersion = xconf.getString(
                  "data.org-openroadm-device.info.model", hwVersion);
                swVersion = xconf.getString(
                  "data.org-openroadm-device.info.softwareVersion", swVersion);
                serialNumber = xconf.getString(
                  "data.org-openroadm-device.info.serial-id", serialNumber);
                chassisId = xconf.getString(
                  "data.org-openroadm-device.info.node-number", chassisId);

                // GEOLOCATION
                String longitudeStr = xconf.getString(
                  "data.org-openroadm-device.info.geoLocation.longitude");
                String latitudeStr = xconf.getString(
                  "data.org-openroadm-device.info.geoLocation.latitude");
                if (longitudeStr != null && latitudeStr != null) {
                    annotationsBuilder
                      .set(org.onosproject.net.AnnotationKeys.LONGITUDE,
                           longitudeStr)
                      .set(org.onosproject.net.AnnotationKeys.LATITUDE,
                           latitudeStr);
                }
            } catch (NetconfException | InterruptedException | ExecutionException e) {
                log.error("[OPENROADM] {} exception", did());
                return null;
            }
        } else {
            log.debug("[OPENROADM] - No  session {}", did());
        }

        log.debug("[OPENROADM] {} - VENDOR {} HWVERSION {} SWVERSION {} SERIAL {} CHASSIS {}",
                did(), vendor, hwVersion, swVersion, serialNumber, chassisId);
        ChassisId cid = new ChassisId(Long.valueOf(chassisId, 10));
        /*
         * OpenROADM defines multiple devices (node types). This driver has been tested with
         * ROADMS, (node type, "rdm"). Other devices can also be discovered, and this code is here
         * for future developments - untested - it is likely that the XML documents
         * are model specific.
         */
        org.onosproject.net.Device.Type type;
        if (nodeType.equals("rdm")) {
            type = org.onosproject.net.Device.Type.ROADM;
        } else if (nodeType.equals("ila")) {
            type = org.onosproject.net.Device.Type.OPTICAL_AMPLIFIER;
        } else if (nodeType.equals("xpdr")) {
            type = org.onosproject.net.Device.Type.TERMINAL_DEVICE;
        } else if (nodeType.equals("extplug")) {
            type = org.onosproject.net.Device.Type.OTHER;
        } else {
            log.error("[OPENROADM] {} unsupported node-type", did());
            return null;
        }
        DeviceDescription desc = new DefaultDeviceDescription(
                did().uri(), type, vendor, hwVersion, swVersion, serialNumber, cid,
                defaultAvailable, annotationsBuilder.build());
        return desc;
    }




    /**
     * Get the external links as a list of XML hieriarchical configs.
     *  @param session the NETConf session to the OpenROADM device.
     *  @return a list of hierarchical conf. each one external link.
     */
    List<HierarchicalConfiguration> getExternalLinks(NetconfSession session) {
        try {
            String reply = session.rpc(getDeviceExternalLinksBuilder()).get();
            XMLConfiguration extLinksConf = //
                (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
            extLinksConf.setExpressionEngine(new XPathExpressionEngine());
            return extLinksConf.configurationsAt(
                    "/data/org-openroadm-device/external-link");
        } catch (NetconfException | InterruptedException | ExecutionException e) {
            log.error("[OPENROADM] {} exception getting external links", did());
            return ImmutableList.of();
        }
    }


    /**
     * Get the circuit packs from the device as a list of XML hierarchical configs.
     *  @param session the NETConf session to the OpenROADM device.
     *  @return a list of hierarchical conf. each one circuit pack.
     */
    List<HierarchicalConfiguration> getCircuitPacks(NetconfSession session) {
        try {
            String reply = session.rpc(getDeviceCircuitPacksBuilder()).get();
            XMLConfiguration cpConf = //
                (XMLConfiguration) XmlConfigParser.loadXmlString(reply);
            cpConf.setExpressionEngine(new XPathExpressionEngine());
            return cpConf.configurationsAt(
                    "/data/org-openroadm-device/circuit-packs");
        } catch (NetconfException | InterruptedException | ExecutionException e) {
            log.error("[OPENROADM] {} exception getting circuit packs", did());
            return ImmutableList.of();
        }
    }

    /**
     * Returns a list of PortDescriptions for the device.
     *
     * @return a list of descriptions.
     */
    @Override
        public List<PortDescription> discoverPortDetails() {
            NetconfSession session = getNetconfSession(did());
            if (session == null) {
                log.error("discoverPortDetails null session for {}", did());
                return ImmutableList.of();
            }
            if (!getDevice().annotations().keys().contains("openroadm-node-id")) {
                log.error("PortDiscovery before DeviceDiscovery, using netconf");
            return ImmutableList.of();
        }
        String nodeId = getDevice().annotations().value("openroadm-node-id");
        List<PortDescription> list = new ArrayList<PortDescription>();
        List<HierarchicalConfiguration> circuitPacks = getCircuitPacks(session);
        /*
         * Iterate all the ports. We need to pass the whole circuitPacks list
         * because some port data refers to ports in other circuit packs
         * (reverse), in addition to pass the current circuit pack name, list of
         * external ports etc
         */
        for (HierarchicalConfiguration c : circuitPacks) {
            parsePorts(list, nodeId, // c contains the whole circuit pack
                       c.getString("circuit-pack-name"), //
                       c.configurationsAt(
                         "ports[port-qual='roadm-external']"), // ext ports
                       circuitPacks, getExternalLinks(session));
        }
        return list;
    }

    /**
     * Parses port information.
     *
     *  @param list  List of port descriptions to append to.
     *  @param nodeId OpenROADM node identifier.
     *  @param circuitPackName Name of the circuit pack the ports belong to
     *  @param ports hierarchical conf containing all the ports for the circuit
     * pack
     *  @param circuitPacks all the circuit packs (to correlate data).
     *  @param extLinks Hierarchical configuration containing all the ext.
     * links.
     */
    protected void parsePorts(List<PortDescription> list, String nodeId,
                              String circuitPackName,
                              List<HierarchicalConfiguration> ports,
                              List<HierarchicalConfiguration> circuitPacks,
                              List<HierarchicalConfiguration> extLinks) {
        checkNotNull(nodeId);
        checkNotNull(circuitPackName);
        for (HierarchicalConfiguration port : ports) {
            try {
                String portName = checkNotNull(port.getString("port-name"));
                long portNum = Long.parseLong(port.getString("label"));
                PortNumber pNum = PortNumber.portNumber(portNum);
                PortNumber reversepNum = findReversePort(port, circuitPacks);
                // To see if we have an external port
                HierarchicalConfiguration eLink = null;
                for (HierarchicalConfiguration extLink : extLinks) {
                    String eln =
                        checkNotNull(extLink.getString("external-link-name"));
                    String esnid =
                        checkNotNull(extLink.getString("source/node-id"));
                    String escpn =
                        checkNotNull(extLink.getString("source/circuit-pack-name"));
                    String espn =
                        checkNotNull(extLink.getString("source/port-name"));
                    if (nodeId.equals(esnid) && circuitPackName.equals(escpn) &&
                            portName.equals(espn)) {
                        eLink = extLink;
                    }
                }
                PortDescription pd = parsePortComponent(
                        nodeId, circuitPackName, pNum, reversepNum, port, eLink);
                if (pd != null) {
                    list.add(pd);
                }
            } catch (NetconfException e) {
                log.error("[OPENROADM] {} NetConf exception", did());
                return;
            }
        }
    }

    /**
     * Given a device port (external), return its patner/reverse port.
     *
     * @param thisPort the port for which we are looking for the reverse port.
     * @param circuitPacks all the circuit packs (to correlate data).
     * @return the port number for the reverse port.
     * @throws NetconfException .
     */
    protected PortNumber
    findReversePort(HierarchicalConfiguration thisPort,
                    List<HierarchicalConfiguration> circuitPacks)
      throws NetconfException {
        String partnerCircuitPackName =
          checkNotNull(thisPort.getString("partner-port/circuit-pack-name"));
        String partnerPortName =
          checkNotNull(thisPort.getString("partner-port/port-name"));
        for (HierarchicalConfiguration c : circuitPacks) {
            if (!partnerCircuitPackName.equals(
                  c.getString("circuit-pack-name"))) {
                continue;
            }
            for (HierarchicalConfiguration thatPort :
                 c.configurationsAt("ports[port-qual='roadm-external']")) {
                String thatPortName = thatPort.getString("port-name");
                if (partnerPortName.equals(thatPortName)) {
                    long thatPortNum =
                      Long.parseLong(thatPort.getString("label"));
                    return PortNumber.portNumber(thatPortNum);
                }
            }
        }
        // We should not reach here
        throw new NetconfException("missing partner/reverse port info");
    }

    /**
     * Parses a component XML doc into a PortDescription.
     * An OMS port description is constructed from XML parsed data.
     *
     * @param port the port to parse
     * @return PortDescription or null
     */
    private PortDescription
    parsePortComponent(String nodeId, String circuitPackName, PortNumber pNum,
                       PortNumber reversepNum, HierarchicalConfiguration port,
                       HierarchicalConfiguration extLink) {
        Map<String, String> annotations = new HashMap<>();
        annotations.put(AnnotationKeys.OPENROADM_NODEID, nodeId);
        annotations.put(AnnotationKeys.OPENROADM_CIRCUIT_PACK_NAME,
                        circuitPackName);
        annotations.put(AnnotationKeys.OPENROADM_PORT_NAME,
                        port.getString("port-name"));
        annotations.put(AnnotationKeys.OPENROADM_PARTNER_CIRCUIT_PACK_NAME,
                        port.getString("partner-port/circuit-pack-name", ""));
        annotations.put(AnnotationKeys.OPENROADM_PARTNER_PORT_NAME,
                        port.getString("partner-port/port-name", ""));
        annotations.put(AnnotationKeys.OPENROADM_LOGICAL_CONNECTION_POINT,
                        port.getString("logical-connection-point", ""));
        // Annotate the reverse port, this is needed for bidir intents.
        annotations.put(OpticalPathIntent.REVERSE_PORT_ANNOTATION_KEY,
                        Long.toString(reversepNum.toLong()));

        // for backwards compatibility
        annotations.put("logical-connection-point",
                        port.getString("logical-connection-point", ""));
        // Annotate external link if we found one for this port
        if (extLink != null) {
            String ednid = extLink.getString("destination/node-id");
            String edcpn = extLink.getString("destination/circuit-pack-name");
            String edpn = extLink.getString("destination/port-name");
            annotations.put("openroadm-external-node-id", ednid);
            annotations.put("openroadm-external-circuit-pack-name", edcpn);
            annotations.put("openroadm-external-port-name", edpn);
        }

        /*
         * Declare the actual optical port:
         * Assumptions: client ports are OCh, assumed to carry ODU4 (should be
         * configurable)
         */
        if (port.getString("port-wavelength-type", "wavelength")
              .equals("wavelength")) {
            // OchSignal is needed for OchPortDescription constructor, but it's
            // tunable
            OchSignal signalId =
              OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, 3);
            return OchPortHelper.ochPortDescription(
              pNum, true /* enabled */, OduSignalType.ODU4, true /* tunable */,
              signalId,
              DefaultAnnotations.builder().putAll(annotations).build());
        } else {
            return OmsPortHelper.omsPortDescription(
              pNum, true /* enabled */,
              // Relationship : START and STOP Freq  not being used (See
              // LambdaQuery)
              START_CENTER_FREQ, STOP_CENTER_FREQ, CHANNEL_SPACING.frequency(),
              DefaultAnnotations.builder().putAll(annotations).build());
        }
    }
}
