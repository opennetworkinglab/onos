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
import com.google.common.io.CharSource;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.onlab.packet.ChassisId;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port.Type;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DefaultPortDescription.Builder;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;
import org.onosproject.net.OchSignal;
import org.onosproject.net.optical.device.OchPortHelper;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.ChannelSpacing;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Nokia OpenConfig based device and port discovery.
 */
public class NokiaOpenConfigDeviceDiscovery
        extends AbstractHandlerBehaviour
        implements OdtnDeviceDescriptionDiscovery, DeviceDescriptionDiscovery {

    private static final Logger log = getLogger(NokiaOpenConfigDeviceDiscovery.class);
    private static final String RPC_TAG_NETCONF_BASE = "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";
    private static final String RPC_CLOSE_TAG = "</rpc>";
    private static final String OPTICAL_CHANNEL = "OCH";
    private static final String TRANSCEIVER = "TRANSCEIVER";

    //TODO should be loaded from a file config or something else
    //The user and password are different from the user and password in the netconf-cfg file
    private static final String USER_NAME = "admin";
    private static final String PASSWORD = "admin";

    @Override
    public DeviceDescription discoverDeviceDetails() {
        DeviceId did = data().deviceId();
        NetconfSession ns = getNetconfSessionAndLogin(did, USER_NAME, PASSWORD);
        if (ns == null) {
            log.error("DiscoverDeviceDetails called with null session for {}", did);
            return null;
        }
        log.info("Discovering device details {}", handler().data().deviceId());
        String hwVersion = "1830", swVersion = "OpenAgent";
        try {
            String reply = ns.requestSync(buildGetSystemSoftwareRpc());
            XMLConfiguration cfg = (XMLConfiguration) XmlConfigParser.loadXmlString(getDataOfRpcReply(reply));
            hwVersion = cfg.getString("components.component.state.description");
            swVersion = cfg.getString("components.component.state.version");
        } catch (NetconfException e) {
            log.error("Error discovering device details on {}", data().deviceId(), e);
        }
        return new DefaultDeviceDescription(handler().data().deviceId().uri(),
                Device.Type.ROADM_OTN,
                "NOKIA",
                hwVersion,
                swVersion,
                "",
                new ChassisId("1"));
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        DeviceId did = data().deviceId();
        XMLConfiguration cfg = new XMLConfiguration();
        NetconfSession ns = getNetconfSessionAndLogin(did, USER_NAME, PASSWORD);
        if (ns == null) {
            log.error("discoverPorts called with null session for {}", did);
            return ImmutableList.of();
        }
        log.info("Discovering ports details {}", handler().data().deviceId());
        try {
            String reply = ns.requestSync(buildGetPlatformComponentsRpc());
            String data = getDataOfRpcReply(reply);
            if (data == null) {
                log.error("No valid response found from {}:\n{}", did, reply);
                return ImmutableList.of();
            }
            cfg.load(CharSource.wrap(data).openStream());
            try {
                ns.startSubscription();
                log.info("Started subscription");
            } catch (NetconfException e) {
                log.error("NETCONF exception caught on {} when the subscription started \n {}",
                          data().deviceId(), e);
            }
            return discoverPorts(cfg);
        } catch (Exception e) {
            log.error("Error discovering port details on {}", data().deviceId(), e);
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
    private List<PortDescription> discoverPorts(XMLConfiguration cfg) {
        // If we want to use XPath
        cfg.setExpressionEngine(new XPathExpressionEngine());
        // converting components into PortDescription.
        List<HierarchicalConfiguration> components = cfg.configurationsAt("components/component");
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
        String name = component.getString("name");
        String type = component.getString("state/type");
        checkNotNull(name, "name not found");
        checkNotNull(type, "state/type not found");
        annotations.put(OdtnDeviceDescriptionDiscovery.OC_NAME, name);
        annotations.put(OdtnDeviceDescriptionDiscovery.OC_TYPE, type);

        component.configurationsAt("properties/property")
                .forEach(property -> {
                    String pn = property.getString("name");
                    String pv = property.getString("state/value");
                    annotations.put(pn, pv);
                });

        if (type.equals("oc-platform-types:PORT")) {

            String subComponentName = component.getString("subcomponents/subcomponent/name");
            String[] textStr = subComponentName.split("-");
            String portComponentType = textStr[0];
            String portComponentIndex = textStr[textStr.length - 1];
            String portNumber = component.getString("name");

             if (portComponentType.equals(OPTICAL_CHANNEL)) {

                 annotations.putIfAbsent(PORT_TYPE, OdtnPortType.LINE.value());
                 annotations.putIfAbsent(ONOS_PORT_INDEX, portComponentIndex.toString());
                 annotations.putIfAbsent(CONNECTION_ID, "connection" + portComponentIndex.toString());
                 annotations.putIfAbsent(AnnotationKeys.PORT_NAME, portNumber);

                 OchSignal signalId = OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, 1);
                 return OchPortHelper.ochPortDescription(
                        PortNumber.portNumber(Long.parseLong(portComponentIndex)),
                        true,
                        OduSignalType.ODU4, // TODO Client signal to be discovered
                        true,
                        signalId,
                        DefaultAnnotations.builder().putAll(annotations).build());

             } else if (portComponentType.equals(TRANSCEIVER)) {

                 Builder builder = DefaultPortDescription.builder();
                 annotations.putIfAbsent(PORT_TYPE, OdtnPortType.CLIENT.value());
                 annotations.putIfAbsent(ONOS_PORT_INDEX, portComponentIndex.toString());
                 annotations.putIfAbsent(CONNECTION_ID, "connection" + portComponentIndex.toString());
                 annotations.putIfAbsent(AnnotationKeys.PORT_NAME, portNumber);

                 builder.withPortNumber(PortNumber.portNumber(Long.parseLong(portComponentIndex), subComponentName));
                 builder.type(Type.PACKET);

                 builder.annotations(DefaultAnnotations.builder().putAll(annotations).build());
                 return builder.build();

            } else {
                log.debug("Unknown port component type {}", type);
                return null;
            }
        } else {
            log.debug("Another component type {}", type);
            return null;
        }
    }

    /**
     * Login to the device by providing the correct user and password in order to configure the device
     * Returns the NetconfSession with the device for which the method was called.
     *
     * @param deviceId device indetifier
     * @param userName
     * @param passwd
     * @return The netconf session or null
     */
    private NetconfSession getNetconfSessionAndLogin(DeviceId deviceId, String userName, String passwd) {
        NetconfController nc = handler().get(NetconfController.class);
        NetconfDevice ndev = nc.getDevicesMap().get(deviceId);
        if (ndev == null) {
            log.debug("netconf device " + deviceId + " is not found, returning null session");
            return null;
        }
        NetconfSession ns = ndev.getSession();
        if (ns == null) {
            log.error("discoverPorts called with null session for {}", deviceId);
            return null;
        }
        try {
            String reply = ns.requestSync(buildLoginRpc(userName, passwd));
            if (reply.contains("<ok/>")) {
                return ns;
            } else {
                log.debug(reply);
                return null;
            }
        } catch (NetconfException e) {
            log.error("can not login to device", e);
        }
        return ns;
    }

    //crude way of removing rpc-reply envelope (copy from netconf session)
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

    /**
     * Construct a rpc request message to get system software component.
     *
     * @return RPC message
     */
    private String buildGetSystemSoftwareRpc() {

        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);
        rpc.append("<get>");
        rpc.append("<filter type='subtree'>");
        rpc.append("<components xmlns=\"http://openconfig.net/yang/platform\">");
        rpc.append("<component>");
        rpc.append("<name>SYSTEM-SOFTWARE</name>");
        rpc.append("</component>");
        rpc.append("</components>");
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append(RPC_CLOSE_TAG);
        return rpc.toString();
    }

    /**
     * Construct a rpc request message to get openconfig platform components.
     *
     * @return RPC message
     */
    private String buildGetPlatformComponentsRpc() {
        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);
        rpc.append("<get>");
        rpc.append("<filter type='subtree'>");
        rpc.append("<components xmlns=\"http://openconfig.net/yang/platform\"></components>");
        rpc.append("</filter>");
        rpc.append("</get>");
        rpc.append(RPC_CLOSE_TAG);
        return rpc.toString();
    }

    /**
     * Construct a rpc login message.
     *
     * @param userName
     * @param passwd
     * @return RPC message
     */
    private String buildLoginRpc(String userName, String passwd) {
        StringBuilder rpc = new StringBuilder(RPC_TAG_NETCONF_BASE);
        rpc.append("<login xmlns=\"http://nokia.com/yang/nokia-security\">");
        rpc.append("<username>");
        rpc.append(userName);
        rpc.append("</username>");
        rpc.append("<password>");
        rpc.append(passwd);
        rpc.append("</password>");
        rpc.append("</login>");
        rpc.append(RPC_CLOSE_TAG);
        return rpc.toString();
    }

}
