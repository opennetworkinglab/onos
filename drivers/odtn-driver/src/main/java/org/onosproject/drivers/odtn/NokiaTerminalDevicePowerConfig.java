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

import com.google.common.collect.Range;
import org.apache.commons.configuration.XMLConfiguration;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.PowerConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Driver Implementation of the PowerConfig for OpenConfig terminal devices.
 * Currently works only with PSI-2T.
 * If you want to make it work with ROADM, you need to implement this interface again.
 *
 */
public class NokiaTerminalDevicePowerConfig<T>
        extends AbstractHandlerBehaviour implements PowerConfig<T> {

    private static final String RPC_TAG_NETCONF_BASE =
            "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

    private static final String RPC_CLOSE_TAG = "</rpc>";

    private static final String OPTICAL_CHANNEL = "OCH";

    private static final Logger log = getLogger(NokiaTerminalDevicePowerConfig.class);

    private ComponentType state = ComponentType.DIRECTION;

    //The username and password are different from the username and password in the netconf-cfg file
    private static final String USER_NAME = "admin";
    private static final String PASSWORD = "admin";


    /**
     * Login to the device by providing the correct user and password in order to configure the device
     * Returns the NetconfSession with the device for which the method was called.
     *
     * @param deviceId device identifier
     * @param userName username to access the device
     * @param passwd password to access the device
     * @return The netconf session or null
     */
    private NetconfSession getNetconfSessionAndLogin(DeviceId deviceId, String userName, String passwd) {
        NetconfController nc = handler().get(NetconfController.class);
        NetconfDevice ndev = nc.getDevicesMap().get(deviceId);
        if (ndev == null) {
            log.debug("NetConf device " + deviceId + " is not found, returning null session");
            return null;
        }
        NetconfSession ns = ndev.getSession();
        if (ns == null) {
            log.error("discoverPorts called with null session for \n {}", deviceId);
            return null;
        }

        try {
            String reply = ns.requestSync(buildLoginRpc(userName, passwd));
            if (reply.contains("<ok/>")) {
                return ns;
            } else {
                log.debug("Reply contains this: \n {}", reply);
                return null;
            }
        } catch (NetconfException e) {
            log.error("Can NOT login to the device", e);
        }
        return ns;
    }

    /**
     * Construct a rpc login message.
     *
     * @param userName username to access the device
     * @param passwd password to access the device
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
     * Get the deviceId for which the methods apply.
     *
     * @return The deviceId as contained in the handler data
     */
    private DeviceId did() {
        return handler().data().deviceId();
    }

    /**
     * Execute RPC request.
     * @param session Netconf session
     * @param message Netconf message in XML format
     * @return XMLConfiguration object
     */

    //TODO: rewrite with requestSync operation..
    private XMLConfiguration executeRpcReq(NetconfSession session, String message) {
        try {
            String reply = session.requestSync(message);
            String data = getDataOfRpcReply(reply);
            log.debug("\n\n [executeRpcReq] RPC request returned this: \n {} \n\n", data);
            XMLConfiguration cfg = (XMLConfiguration) XmlConfigParser.loadXmlString(getDataOfRpcReply(reply));
            return cfg;
        } catch (NetconfException ne) {
            log.error("Exception on NetConf protocol: {}.", ne);
        } catch (Exception e) {
            log.debug("Error loading data to internal XML Configuration format: \n {}", e);
        }
        return null;
    }

    /**
     * Get the target-output-power value on specific optical-channel.
     * @param port the port
     * @param component the port component. It should be 'oc-name' in the Annotations of Port.
     *                  'oc-name' could be mapped to '/component/name' in openconfig yang.
     * @return target power value
     */
    @Override
    public Optional<Double> getTargetPower(PortNumber port, T component) {
        checkState(component);
        return state.getTargetPower(port, component);
    }

    @Override
    public void setTargetPower(PortNumber port, T component, double power) {
        checkState(component);
        state.setTargetPower(port, component, power);
    }

    @Override
    public Optional<Double> currentPower(PortNumber port, T component) {
        checkState(component);
        return state.currentPower(port, component);
    }

    @Override
    public Optional<Double> currentInputPower(PortNumber port, T component) {
        checkState(component);
        return state.currentInputPower(port, component);
    }

    @Override
    public Optional<Range<Double>> getTargetPowerRange(PortNumber port, T component) {
        checkState(component);
        return state.getTargetPowerRange(port, component);
    }

    @Override
    public Optional<Range<Double>> getInputPowerRange(PortNumber port, T component) {
        checkState(component);
        return state.getInputPowerRange(port, component);
    }

    @Override
    public List<PortNumber> getPorts(T component) {
        checkState(component);
        return state.getPorts(component);
    }


    /**
     * Set the ComponentType to invoke proper methods for different template T.
     * @param component the component.
     */
    void checkState(Object component) {
        String clsName = component.getClass().getName();
        switch (clsName) {
            case "org.onosproject.net.Direction":
                state = ComponentType.DIRECTION;
                break;
            case "org.onosproject.net.OchSignal":
                state = ComponentType.OCHSIGNAL;
                break;
            default:
                log.error("Cannot parse the component type {}.", clsName);
                log.info("The component content is {}.", component.toString());
        }
        state.nokia = this;
    }

    /**
     * Component type.
     */
    enum ComponentType {

        /**
         * Direction.
         */
        DIRECTION() {
            @Override
            public Optional<Double> getTargetPower(PortNumber port, Object component) {
                return super.getTargetPower(port, component);
            }
            @Override
            public void setTargetPower(PortNumber port, Object component, double power) {
                super.setTargetPower(port, component, power);
            }
        },

        /**
         * OchSignal.
         */
        OCHSIGNAL() {
            @Override
            public Optional<Double> getTargetPower(PortNumber port, Object component) {
                return super.getTargetPower(port, component);
            }

            @Override
            public void setTargetPower(PortNumber port, Object component, double power) {
                super.setTargetPower(port, component, power);
            }
        };



        NokiaTerminalDevicePowerConfig nokia;

        /**
         * mirror method in the internal class.
         * @param port port
         * @param component component
         * @return target power
         */
        //TODO: Overlap with getTargetPowerRange function..
        Optional<Double> getTargetPower(PortNumber port, Object component) {
            NetconfSession session = nokia.getNetconfSessionAndLogin(nokia.did(), USER_NAME, PASSWORD);
            checkNotNull(session);
            String filter = parsePort(nokia, port, null, null);
            if (filter != null) {
                StringBuilder rpcReq = new StringBuilder();
                rpcReq.append(RPC_TAG_NETCONF_BASE)
                        .append("<get>")
                        .append("<filter type='subtree'>")
                        .append(filter)
                        .append("</filter>")
                        .append("</get>")
                        .append(RPC_CLOSE_TAG);
                XMLConfiguration xconf = nokia.executeRpcReq(session, rpcReq.toString());
                log.debug("\n\n [getTargetPower] Obtained information " +
                                  "from getTargetPower function is.. \n {} \n\n", xconf);
                try {
                    String tpower = xconf.getString("components.component." +
                                                            "oc-opt-term:optical-channel." +
                                                            "oc-opt-term:config." +
                                                            "oc-opt-term:target-output-power");
                    double power = Float.valueOf(tpower).doubleValue();
                    log.debug("\n\n [getTargetPower] Target OUTPUT power is.. {} \n\n", power);
                    return Optional.of(power);
                } catch (IllegalArgumentException e) {
                    log.debug("\n\n [getTargetPower] Something went wrong " +
                                      "during the parsing of configuration in getTargetPower function.. \n\n");
                    return Optional.empty();
                }
            } else {
                log.debug("Port you're trying to get ({}) is not optical", port.toString());
                return Optional.empty();
            }
        }

        /**
         * mirror method in the internal class.
         * @param port port
         * @param component component
         * @param power target value
         */
        void setTargetPower(PortNumber port, Object component, double power) {
            NetconfSession session = nokia.getNetconfSessionAndLogin(nokia.did(), USER_NAME, PASSWORD);
            checkNotNull(session);
            String editConfig = parsePort(nokia, port, null, power);
            if (editConfig != null) {
                StringBuilder rpcReq = new StringBuilder();
                rpcReq.append(RPC_TAG_NETCONF_BASE)
                        .append("<edit-config>")
                        .append("<target><running/></target>")
                        .append("<config xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">")
                        .append(editConfig)
                        .append("</config>")
                        .append("</edit-config>")
                        .append(RPC_CLOSE_TAG);
                XMLConfiguration xconf = nokia.executeRpcReq(session, rpcReq.toString());
                log.debug("\n\n [setTargetPower] Obtained information is.. \n {} \n\n", xconf);
                // The successful reply should be "<rpc-reply ...><ok /></rpc-reply>"
                if (!xconf.getRoot().getChild(0).getName().equals("ok")) {
                    log.error("[setTargetPower] The <edit-config> operation " +
                                      "to set target-output-power of Port({}:{}) is failed.",
                              port.toString(), component.toString());
                } else {
                    log.debug("[setTargetPower] Answer on <edit-config> request " +
                                      "is following: \n {}\n", xconf.toString());
                }
            } else {
                log.debug("[setTargetPower] Port you're trying " +
                                  "to set ({}) is not optical", port.toString());
            }
        }

        /**
         * mirror method in the internal class.
         * @param port port
         * @param component the component.
         * @return current output power.
         */
        Optional<Double> currentPower(PortNumber port, Object component) {
            XMLConfiguration xconf = getOpticalChannelState(
                    nokia, port, "<oc-opt-term:output-power>" +
                            "<oc-opt-term:instant/>" +
                            "</oc-opt-term:output-power>");
            try {
                String oipower = xconf.getString("components.component." +
                                                         "oc-opt-term:optical-channel." +
                                                         "oc-opt-term:state." +
                                                         "oc-opt-term:output-power." +
                                                         "oc-opt-term:instant");
                log.debug("\n\n [currentPower] That's what we read: \n {} \n\n", oipower);
                double currentPower = Float.valueOf(oipower).doubleValue();
                return Optional.of(currentPower);
            } catch (Exception e) {
                log.debug("\n\n [currentPower] Something went wrong " +
                                  "during the parsing of obtained answer.. \n\n");
                return Optional.empty();
            }
        }

        /**
         * mirror method in the internal class.
         * @param port port
         * @param component the component
         * @return current input power
         */
        Optional<Double> currentInputPower(PortNumber port, Object component) {
            XMLConfiguration xconf = getOpticalChannelState(
                    nokia, port, "<oc-opt-term:input-power>" +
                            "<oc-opt-term:instant/>" +
                            "</oc-opt-term:input-power>");
            try {
                String iipower = xconf.getString("components.component." +
                                                         "oc-opt-term:optical-channel." +
                                                         "oc-opt-term:state." +
                                                         "oc-opt-term:input-power." +
                                                         "oc-opt-term:instant");
                log.debug("\n\n [currentInputPower] That's what we read: \n {} \n\n", iipower);
                double currentPower = Float.valueOf(iipower).doubleValue();

                return Optional.of(currentPower);
            } catch (Exception e) {
                log.debug("\n\n [currentInputPower] Something went wrong " +
                                  "during the parsing of obtained answer.. \n\n");
                return Optional.empty();
            }
        }

        /**
         * Getting target value of output power.
         * @param port port
         * @param component the component
         * @return target output power range
         */

        Optional<Range<Double>> getTargetPowerRange(PortNumber port, Object component) {
            double targetMin = -20;
            double targetMax = 5;
            return Optional.of(Range.open(targetMin, targetMax));
        }

        Optional<Range<Double>> getInputPowerRange(PortNumber port, Object component) {
            double targetMin = -20;
            double targetMax = 5;
            return Optional.of(Range.open(targetMin, targetMax));
        }

        List<PortNumber> getPorts(Object component) {
            // FIXME
            log.warn("[getPorts] Not Implemented Yet!");
            return new ArrayList<PortNumber>();
        }

        /**
         * Get filtered content under <optical-channel><state>.
         * @param pc power config instance
         * @param port the port number
         * @param underState the filter condition
         * @return RPC reply
         */
        private static XMLConfiguration getOpticalChannelState(NokiaTerminalDevicePowerConfig pc,
                                                               PortNumber port, String underState) {
            NetconfSession session = pc.getNetconfSessionAndLogin(pc.did(), USER_NAME, PASSWORD);
            checkNotNull(session);
            String name = ocName(pc, port);
            StringBuilder rpcReq = new StringBuilder(RPC_TAG_NETCONF_BASE);
            rpcReq.append("<get><filter><components xmlns=\"http://openconfig.net/yang/platform\"><component>")
                    .append("<name>").append(name).append("</name>")
                    .append("<oc-opt-term:optical-channel " +
                                    "xmlns:oc-opt-term=\"http://openconfig.net/yang/terminal-device\">")
                    .append("<oc-opt-term:state>")
                    .append(underState)
                    .append("</oc-opt-term:state>")
                    .append("</oc-opt-term:optical-channel>")
                    .append("</component></components></filter></get>")
                    .append(RPC_CLOSE_TAG);
            XMLConfiguration xconf = pc.executeRpcReq(session, rpcReq.toString());
            return xconf;
        }

        /**
         * Extract component name from portNumber's annotations.
         * @param pc power config instance
         * @param portNumber the port number
         * @return the component name
         */
        private static String ocName(NokiaTerminalDevicePowerConfig pc, PortNumber portNumber) {
            DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
            DeviceId deviceId = pc.handler().data().deviceId();
            String port = deviceService.getPort(deviceId, portNumber).annotations().value("oc-name");

            // Applying some magic to return the correct port with the correct name
            String portType = deviceService.getPort(deviceId, portNumber).type().toString();
            log.debug("\n\n [ocName] Type of the port taken from ONOS storage " +
                              "has following properties.. \n {} \n", portType);
            if (portType.equals(OPTICAL_CHANNEL)) {
                String[] textStr = port.split("-");
                String och = "OCH-" + textStr[1] + "-" + textStr[2] + "-" + textStr[3];
                log.debug("\n\n [ocName] Optical channel returned is.. {} \n\n", och);
                return och;
            } else {
                log.debug("[ocName] This port is not an optical one");
                return null;
            }
        }

        /**
         * Parse filtering string from port and component.
         * @param portNumber Port Number
         * @param component port component (optical-channel)
         * @param power power value set.
         * @return filtering string in xml format
         */
        private static String parsePort(NokiaTerminalDevicePowerConfig pc, PortNumber portNumber,
                                        Object component, Double power) {
            if (component == null) {
                String name = ocName(pc, portNumber);
                if (name != null) {
                    StringBuilder sb = new StringBuilder("<components " +
                                                                 "xmlns=\"http://openconfig.net/yang/platform\">");
                    sb.append("<component>").append("<name>").append(name).append("</name>");
                    if (power != null) {
                        // This is an edit-config operation.
                        sb.append("<oc-opt-term:optical-channel " +
                                          "xmlns:oc-opt-term=\"http://openconfig.net/yang/terminal-device\">")
                                .append("<oc-opt-term:config>")
                                .append("<oc-opt-term:target-output-power>")
                                .append(power)
                                .append("</oc-opt-term:target-output-power>")
                                .append("</oc-opt-term:config>")
                                .append("</oc-opt-term:optical-channel>");
                    }
                    sb.append("</component>").append("</components>");
                    return sb.toString();
                }
            } else {
                log.error("[parsePort] Cannot process the component {}.", component.getClass());
                return null;
            }
            return null;
        }
    }
}
