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
 */

package org.onosproject.drivers.odtn;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.onlab.packet.ChassisId;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.Port.Type;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * OpenConfig based device and port discovery.
 */
public class FujitsuOpenConfigDeviceDiscovery extends AbstractHandlerBehaviour
        implements OdtnDeviceDescriptionDiscovery, DeviceDescriptionDiscovery {

    // Enable logging
    private static final Logger log = getLogger(FujitsuOpenConfigDeviceDiscovery.class);
    private static final CharSequence OPTICAL_CHANNEL = "OPTICAL_CHANNEL";
    private static final CharSequence TRANSCEIVER = "TRANSCEIVER";
    private static final String COMPONENT_STATE = "state";
    private static final String COMPONENT_NAME = "name";
    private static final String COMPONENT_TYPE = "type";
    private static final String PORT_COMPONENT_PATH = "data.components.component";
    private static final String MFG_NAME_TREE_PATH = PORT_COMPONENT_PATH + ".state.mfg-name";
    private static final String SW_VERSION_TREE_PATH = PORT_COMPONENT_PATH + ".state.software-version";
    private static final String HW_VERSION_TREE_PATH = PORT_COMPONENT_PATH + ".state.hardware-version";
    private static final String SERIAL_NUMBER_TREE_PATH = PORT_COMPONENT_PATH + ".state.serial-no";
    private static final String CHASSIS_ID_TREE_PATH = PORT_COMPONENT_PATH + ".state.id";

    private PortDescription parsePortComponent(String portComponent,
                                                      Port.Type portDescriptionType,
                                                      String portType,
                                                      HierarchicalConfiguration portConfig) {
        Map<String, String> props = new HashMap<>();
        int portIdentifierNumber =
                portComponent.equalsIgnoreCase(TRANSCEIVER.toString()) ? 100 : 0;
        DefaultPortDescription.Builder builder = DefaultPortDescription.builder();
        String portComponentName =
                checkNotNull(portConfig.configurationAt(COMPONENT_STATE).getString(COMPONENT_NAME),
                        "state/name does not exist");
        String portComponentType =
                checkNotNull(portConfig.configurationAt(COMPONENT_STATE).getString(COMPONENT_TYPE),
                        "state/type does not exist");
        /*
         * Both components have names that end with component1-1/1/0/C1 component2-1/1/0/C1,
         * So we need a way to distinguish component1 from component2
         * Hence, We add a 100 to the port number to distinguish client port from the network port
         */
        int portNumberMatchIndex = portComponentName.split("/[a-zA-Z]").length - 1;
        Long portComponentId = Long.parseLong(
                portComponentName.split("/[a-zA-Z]")[portNumberMatchIndex]) + portIdentifierNumber;
        props.put(OdtnDeviceDescriptionDiscovery.OC_NAME, portComponentName);
        props.put(OdtnDeviceDescriptionDiscovery.OC_TYPE, portComponentType);
        builder.withPortNumber(PortNumber.portNumber((portComponentId),
                portComponentName));
        builder.type(portDescriptionType);
        props.putIfAbsent(PORT_TYPE, portType);
        props.putIfAbsent(CONNECTION_ID, String.valueOf(portComponentId));
        return builder.annotations(DefaultAnnotations.builder().putAll(props).build()).build();
    }

    /**
     * Parses 1FINITY ports based on optical channel or transceiver
     * and returns their details.
     *
     * @param xmlCfg xml object of the component details from 1FINITY-T600
     * @return List<PortDescription>
     */
    @VisibleForTesting
    public List<PortDescription> parse1FinityPorts(HierarchicalConfiguration xmlCfg) {
        List<PortDescription> portDescriptions = Lists.newArrayList();
        List<HierarchicalConfiguration> subTrees = checkNotNull(xmlCfg
                .configurationsAt(PORT_COMPONENT_PATH));
        for (HierarchicalConfiguration portConfig : subTrees) {
            String portTypeSubComponent = checkNotNull(portConfig
                    .configurationAt(COMPONENT_STATE).getString(COMPONENT_TYPE));
            if (portTypeSubComponent.contains(OPTICAL_CHANNEL)) {
                portDescriptions.add(parsePortComponent(OPTICAL_CHANNEL.toString(),
                        Type.OCH,
                        OdtnPortType.LINE.value(),
                        portConfig));
            } else if (portTypeSubComponent.contains(TRANSCEIVER)) {
                portDescriptions.add(parsePortComponent(TRANSCEIVER.toString(),
                        Type.PACKET,
                        OdtnPortType.CLIENT.value(),
                        portConfig));
            }
        }
        return portDescriptions;
    }

    /**
     * Returns a device description appropriately annotated to support
     * downstream model extension via projections of the resulting device,
     * as in the following example.
     * <pre>
     * MicrowaveDevice device = deviceService.get(id).as(MicrowaveDevice.class);
     * </pre>
     *
     * @return annotated device description
     */
    @Override
    public DeviceDescription discoverDeviceDetails() {
        NetconfSession netconfSession = checkNotNull(getNetconfSession());
        String requestedComponent = null;
        try {
            requestedComponent = netconfSession.doWrappedRpc(buildGetComponentRequest("shelf-1"));
        } catch (NetconfException netconfException) {
            log.error("Unable to send the request via netconf: {}\n" +
                    "Getting default Device Description Details", netconfException);
        }
        XMLConfiguration xmlCfg = (XMLConfiguration) XmlConfigParser.loadXmlString(requestedComponent);

        return parseDeviceInformation(handler().data().deviceId().uri(), xmlCfg);
    }

    /**
     * Returns a list of port descriptions appropriately annotated to support
     * downstream model extension via projections of their parent device,
     * as in the following example.
     * <pre>
     * MicrowaveDevice device = deviceService.get(id).as(MicrowaveDevice.class);
     * List&lt;MicrowavePort&gt; ports = device.microwavePorts(deviceService.getPorts(id));
     * </pre>
     *
     * @return annotated device description
     */
    @Override
    public List<PortDescription> discoverPortDetails() {
        NetconfSession netconfSession = checkNotNull(getNetconfSession());
        String allComponents = null;
        try {
            allComponents = netconfSession.doWrappedRpc(buildGetComponentsRequest());
            log.info("Netconf reply is as follows:\n{}", allComponents);
        } catch (NetconfException netconfException) {
            log.error("Unable to send the request via netconf: {}", netconfException);
            return null;
        }
        List<PortDescription> portDescriptions = parse1FinityPorts(XmlConfigParser
                .loadXml(new ByteArrayInputStream(allComponents.getBytes())));
        return ImmutableList.copyOf(portDescriptions);

    }

    /**
     * Parses 1FINITY-T600 device information through the response from an rpc request.
     *
     * @param uri    URI object that comprises of the deviceId
     * @param xmlCfg xml object comprising device related details
     * @return DeviceDescription
     */
    @VisibleForTesting
    public DeviceDescription parseDeviceInformation(URI uri, XMLConfiguration xmlCfg) {
        String mfgName = "FUJITSU";
        String hwVersion = null;
        String swVersion = null;
        String serialNumber = null;
        String chassisId = "1";

        mfgName = xmlCfg.getString(MFG_NAME_TREE_PATH, mfgName);
        swVersion = xmlCfg.getString(SW_VERSION_TREE_PATH, swVersion);
        hwVersion = xmlCfg.getString(HW_VERSION_TREE_PATH, hwVersion);
        serialNumber = xmlCfg.getString(SERIAL_NUMBER_TREE_PATH, serialNumber);
        chassisId = xmlCfg.getString(CHASSIS_ID_TREE_PATH, chassisId);
        return new DefaultDeviceDescription(uri,
                Device.Type.OTN,
                mfgName,
                hwVersion,
                swVersion,
                serialNumber,
                new ChassisId(chassisId));
    }

    /**
     * Utility method to return a Netconf session.
     *
     * @return NetconfSession
     */
    private NetconfSession getNetconfSession() {
        DeviceId did = checkNotNull(handler().data().deviceId());
        NetconfController netconfController = checkNotNull(handler().get(NetconfController.class));
        NetconfDevice netconfDevice = netconfController.getDevicesMap().get(did);
        return checkNotNull(netconfDevice.getSession());
    }

    /**
     * Construct a String with a Netconf filtered get RPC Message.
     *
     * @param filter A valid XML tree with the filter to apply in the get
     * @return a String containing the RPC XML Document
     */
    private String filteredGetBuilder(String filter) {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<get>");
        rpc.append("<filter type='subtree'>");
        rpc.append(filter);
        rpc.append("</filter>");
        rpc.append("</get>");
        return rpc.toString();
    }

    private String buildGetComponentRequest(String componentName) {
        StringBuilder filter = new StringBuilder();
        filter.append("<components xmlns=\"http://openconfig.net/yang/platform\">");
        filter.append("<component>");
        filter.append("<name>");
        filter.append(componentName);
        filter.append("</name>");
        filter.append("</component>");
        filter.append("</components>");
        return filteredGetBuilder(filter.toString());
    }

    private String buildGetComponentsRequest() {
        StringBuilder filter = new StringBuilder();
        filter.append("<components xmlns=\"http://openconfig.net/yang/platform\">");
        filter.append("</components>");
        return filteredGetBuilder(filter.toString());
    }

}
