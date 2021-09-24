/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.drivers.lumentum;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.lang3.StringUtils;

import org.onlab.packet.ChassisId;
import org.onlab.util.Frequency;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.intent.OpticalPathIntent;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;

import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.optical.device.OmsPortHelper.omsPortDescription;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Device description behaviour for Lumentum ROADM-A Whitebox devices using NETCONF.
 *
 * Tested on device Lumentum:ROADM with Twin 1X20 WSS
 * Software versions:
 * - dcian_R3.1.2_057
 * - dcian_R2.1.4_136
 */
public class LumentumNetconfRoadmDiscovery
        extends AbstractHandlerBehaviour implements DeviceDescriptionDiscovery {

    private static final String PHYSICAL_PORT = "data.physical-ports.physical-port";

    private static final String DN = "dn";
    private static final String DN_PORT = "port=";
    private static final String PORT_EXTENSION = "port-extension";
    protected static final String OPTICAL_INPUT = "port-optical-input";
    protected static final String OPTICAL_OUTPUT = "port-optical-output";
    private static final String PORT_PLUGGABLE = "port-pluggable";
    private static final String PORT_ETHERNET = "port-ethernet";

    private static final String MAINTENANCE_STATE = "config.maintenance-state";
    private static final String PORT_SPEED = "config.loteeth:port-speed";
    private static final String IN_SERVICE = "in-service";
    private static final String PORT_NAME = "entity-description";

    public static final ChannelSpacing CHANNEL_SPACING_50 = ChannelSpacing.CHL_50GHZ;
    public static final Frequency START_CENTER_FREQ_50 = Frequency.ofGHz(191_350);
    public static final Frequency END_CENTER_FREQ_50 = Frequency.ofGHz(196_100);

    private static final int MIN_MUX_PORT = 4101;
    private static final int MAX_MUX_PORT = 4120;
    private static final int MIN_DEM_PORT = 5201;
    private static final int MAX_DEM_PORT = 5220;
    private static final int DELTA_MUX_DEM_PORT = MIN_DEM_PORT - MIN_MUX_PORT;

    private static final String MUX_PORT_NAME = "Mux Input";
    private static final String DEMUX_PORT_NAME = "Demux Output";
    private static final String LINE_PORT_NAME = "Optical Line";

    private final Logger log = getLogger(getClass());

    @Override
    public DeviceDescription discoverDeviceDetails() {
        SparseAnnotations annotations = DefaultAnnotations.builder().build();

        log.debug("Lumentum NETCONF - starting discoverDeviceDetails");

        // Some defaults values
        String vendor       = "Lumentum";
        String hwVersion    = "not loaded";
        String swVersion    = "not loaded";
        String serialNumber = "not loaded";
        String chassisData    = "ne=1;chassis=10";

        ChassisId chassisId = null;
        DeviceId deviceId = handler().data().deviceId();

        NetconfSession session = getNetconfSession();
        if (session == null) {
            log.error("Lumentum NETCONF - session not found for {}", deviceId);
            return null;
        }

        //Retrieve system information from ietf-system
        StringBuilder systemRequestBuilder = new StringBuilder();
        systemRequestBuilder.append("<system-state xmlns=\"urn:ietf:params:xml:ns:yang:ietf-system\">");
        systemRequestBuilder.append("</system-state>");

        try {
            String reply = session.get(systemRequestBuilder.toString(), null);
            log.debug("Lumentum NETCONF - session.get reply {}", reply);

            XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(reply);

            vendor    = xconf.getString("data.system-state.platform.machine", vendor);
            swVersion    = xconf.getString("data.system-state.platform.os-version", swVersion);
        } catch (NetconfException e) {
            log.error("Lumentum NETCONF error in session.get with filter <system-state>", e);
        }

        //Retrieve system information
        StringBuilder chassisRequestBuilder = new StringBuilder();
        chassisRequestBuilder.append("<chassis-list xmlns=\"http://www.lumentum.com/lumentum-ote-equipment\">");
        chassisRequestBuilder.append("</chassis-list>");

        try {
            String reply = session.get(chassisRequestBuilder.toString(), null);
            log.debug("Lumentum NETCONF - session.get reply {}", reply);

            XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(reply);

            hwVersion    = xconf.getString("data.chassis-list.chassis.state.loteq:hardware-rev", hwVersion);
            serialNumber = xconf.getString("data.chassis-list.chassis.state.loteq:serial-no", serialNumber);
            chassisData  = xconf.getString("data.chassis-list.chassis.dn", chassisData);

            String[] parts = chassisData.split("chassis=");
            chassisId = new ChassisId(Long.valueOf(parts[1], 10));

        } catch (NetconfException e) {
            log.error("Lumentum NETCONF error in session.get", e);
        }

        //Upon connection of a new devices all pre-configured connections are removed
        //TODO consider a way to keep "external" FlowRules
        rpcRemoveAllConnections("1");
        rpcRemoveAllConnections("2");

        log.info("Lumentum ROADM20 - discovered details:");
        log.info("TYPE      {}", Device.Type.ROADM);
        log.info("VENDOR    {}", vendor);
        log.info("HWVERSION {}", hwVersion);
        log.info("SWVERSION {}", swVersion);
        log.info("SERIAL    {}", serialNumber);
        log.info("CHASSISID {}", chassisId);

        //Return the Device Description
        return new DefaultDeviceDescription(deviceId.uri(), Device.Type.ROADM,
                vendor, hwVersion, swVersion, serialNumber, chassisId, annotations);
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        DeviceId deviceId = handler().data().deviceId();
        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        Device device = deviceService.getDevice(deviceId);

        //Get the configuration from the device
        if (device == null) {
            log.error("Lumentum NETCONF - device object not found for {}", deviceId);
            return ImmutableList.of();
        }

        NetconfSession session = getNetconfSession();
        if (session == null) {
            log.error("Lumentum NETCONF - session not found for {}", deviceId);
            return ImmutableList.of();
        }

        StringBuilder requestBuilder = new StringBuilder();
        requestBuilder.append("<physical-ports xmlns=\"http://www.lumentum.com/lumentum-ote-port\" ");
        requestBuilder.append("xmlns:lotep=\"http://www.lumentum.com/lumentum-ote-port\" ");
        requestBuilder.append("xmlns:lotepopt=\"http://www.lumentum.com/lumentum-ote-port-optical\" ");
        requestBuilder.append("xmlns:loteeth=\"http://www.lumentum.com/lumentum-ote-port-ethernet\">");
        requestBuilder.append("</physical-ports>");

        String reply;
        try {
            reply = session.get(requestBuilder.toString(), null);
        } catch (NetconfException e) {
            log.error("Lumentum NETCONF - " +
                    "discoverPortDetails failed to retrieve port details {}", handler().data().deviceId(), e);
            return ImmutableList.of();
        }

        List<PortDescription> descriptions = parseLumentumRoadmPorts(XmlConfigParser.
                        loadXml(new ByteArrayInputStream(reply.getBytes())));

        return ImmutableList.copyOf(descriptions);
    }

    /**
     * Parses a configuration and returns a set of ports.
     *
     * @param cfg a hierarchical configuration
     * @return a list of port descriptions
     */
    protected List<PortDescription> parseLumentumRoadmPorts(HierarchicalConfiguration cfg) {
        List<PortDescription> portDescriptions = Lists.newArrayList();
        List<HierarchicalConfiguration> ports = cfg.configurationsAt(PHYSICAL_PORT);

        ports.stream().forEach(pcfg -> {

            DefaultAnnotations.Builder annotations = DefaultAnnotations.builder();

            //Load port number
            PortNumber portNum = PortNumber.portNumber(
                    pcfg.getString(DN).substring(pcfg.getString(DN).lastIndexOf(DN_PORT) + 5));

            //Load port state
            String maintenanceState = pcfg.getString(MAINTENANCE_STATE);
            boolean isEnabled = ((maintenanceState != null) && (maintenanceState).equals(IN_SERVICE));

            //Load port type (FIBER/COPPER)
            Port.Type type = null;
            for (Object o : pcfg.getList(PORT_EXTENSION)) {
                String s = (String) o;
                if (s.contains(OPTICAL_INPUT) || s.contains(OPTICAL_OUTPUT)) {
                    type = Port.Type.FIBER;
                    log.debug("Loaded OPTICAL port {}", portNum);

                } else if (s.contains(PORT_ETHERNET) || s.contains(PORT_PLUGGABLE)) {
                    type = Port.Type.COPPER;
                    log.debug("Loaded PACKET port {}", portNum);

                }
            }

            //Load port speed of Ethernet interface, expressed in Mb/s
            Long speed = 0L; //should be the speed of optical port
            if (type != null) {
                if (type.equals(Port.Type.COPPER)) {
                    String speedString = pcfg.getString(PORT_SPEED);
                    log.debug("--- port {} loaded speed {}", portNum, speedString);
                    if (speedString != null) {
                        if (speedString.contains("Mb")) {
                            speed = Long.parseLong(speedString.substring(
                                    speedString.lastIndexOf("speed_") + 6,
                                    speedString.lastIndexOf("Mb")));
                        }
                        if (speedString.contains("Gb")) {
                            speed = 1000 * Long.parseLong(speedString.substring(
                                    speedString.lastIndexOf("speed_") + 6,
                                    speedString.lastIndexOf("Gb")));
                        }
                    } else {
                        log.error("Lumentum NETCONF - Port speed of Ethernet port not correctly loaded");
                    }
                }
            } else {
                log.error("Port Type not correctly loaded");
            }

            /**
             * Setting the reverse port value for the unidirectional ports.
             *
             * In this device each port includes an input fiber and an output fiber.
             * The 20 input  fibers are numbered from MIN_MUX_PORT = 4101 to MAX_MUX_PORT = 4120.
             * The 20 output fibers are numbered from MIN_DEM_PORT = 5201 to MAX_DEM_PORT = 5220.
             *
             * Where port 520x is always the reverse of 410x.
             */
            if ((portNum.toLong() >= MIN_MUX_PORT) && (portNum.toLong() <= MAX_MUX_PORT)) {
                Long reversePortId = portNum.toLong() + DELTA_MUX_DEM_PORT;
                annotations.set(OpticalPathIntent.REVERSE_PORT_ANNOTATION_KEY, reversePortId.toString());
            }
            if ((portNum.toLong() >= MIN_DEM_PORT) && (portNum.toLong() <= MAX_DEM_PORT)) {
                Long reversePortId = portNum.toLong() - DELTA_MUX_DEM_PORT;
                annotations.set(OpticalPathIntent.REVERSE_PORT_ANNOTATION_KEY, reversePortId.toString());
            }

            //Load other information
            pcfg.getKeys().forEachRemaining(k -> {
                if (!k.contains(DN) && !k.contains(PORT_SPEED) && !k.contains(PORT_EXTENSION)
                        && !k.contains(MAINTENANCE_STATE)) {
                    String value = pcfg.getString(k);
                    if (!value.isEmpty()) {
                        k = StringUtils.replaceEach(k, new String[]{"loteeth:", "lotep:",
                                                            "lotepopt:", "config.", "=", ":",
                                                            "state."},
                                                    new String[]{"", "", "", "", "", "", ""});

                        annotations.set(k, value);

                        //To visualize port name in the ROADM app GUI
                        if (k.equals(PORT_NAME)) {
                            annotations.set(AnnotationKeys.PORT_NAME, value);
                        }

                    }
                }
            });

            log.debug("Lumentum NETCONF - retrieved port {},{},{},{},{}",
                    portNum, isEnabled, type, speed, annotations.build());


            if ((type == Port.Type.FIBER) &&
                    ((annotations.build().value(AnnotationKeys.PORT_NAME)).contains(MUX_PORT_NAME) ||
                            (annotations.build().value(AnnotationKeys.PORT_NAME)).contains(DEMUX_PORT_NAME) ||
                            (annotations.build().value(AnnotationKeys.PORT_NAME)).contains(LINE_PORT_NAME))) {

                //These are the ports supporting OchSignals
                portDescriptions.add(omsPortDescription(portNum,
                        isEnabled,
                        START_CENTER_FREQ_50,
                        END_CENTER_FREQ_50,
                        CHANNEL_SPACING_50.frequency(),
                        annotations.build()));
            } else {
                //These are COPPER ports, or FIBER ports not supporting OchSignals
                DefaultPortDescription.Builder portDescriptionBuilder = DefaultPortDescription.builder();
                portDescriptionBuilder.withPortNumber(portNum)
                        .isEnabled(isEnabled)
                        .type(type)
                        .portSpeed(speed)
                        .annotations(annotations.build());

                portDescriptions.add(portDescriptionBuilder.build());
            }
        });

        return portDescriptions;
    }

    //Following Lumentum documentation rpc operation to delete all connections
    private boolean rpcRemoveAllConnections(String module) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">" + "\n");
        stringBuilder.append(
                "<remove-all-connections xmlns=\"http://www.lumentum.com/lumentum-ote-connection\">" + "\n");
        stringBuilder.append("<dn>ne=1;chassis=1;card=1;module=" + module + "</dn>" + "\n");
        stringBuilder.append("</remove-all-connections>" + "\n");
        stringBuilder.append("</rpc>" + "\n");

        return editCrossConnect(stringBuilder.toString());
    }

    private boolean editCrossConnect(String xcString) {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            log.error("Lumentum NETCONF - session not found for {}", handler().data().deviceId());
            return false;
        }

        try {
            return session.editConfig(xcString);
        } catch (NetconfException e) {
            log.error("Failed to edit the CrossConnect edid-cfg for device {}",
                    handler().data().deviceId(), e);
            log.debug("Failed configuration {}", xcString);
            return false;
        }
    }

    /**
     * Helper method to get the Netconf session.
     */
    private NetconfSession getNetconfSession() {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        return controller.getNetconfDevice(did()).getSession();
    }

    /**
     * Helper method to get the device id.
     */
    private DeviceId did() {
        return data().deviceId();
    }
}
