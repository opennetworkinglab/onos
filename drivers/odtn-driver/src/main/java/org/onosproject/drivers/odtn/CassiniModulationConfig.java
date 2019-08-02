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
 *
 * This Work is contributed by Sterlite Technologies
 */

package org.onosproject.drivers.odtn;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.drivers.odtn.util.NetconfSessionUtility;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Direction;
import org.onosproject.net.ModulationScheme;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ModulationConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/*
 * Driver Implementation of the ModulationConfig for OpenConfig terminal devices.
 */
public class CassiniModulationConfig<T> extends AbstractHandlerBehaviour implements ModulationConfig<T> {


    private static final String RPC_TAG_NETCONF_BASE =
            "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

    private static final String RPC_CLOSE_TAG = "</rpc>";

    private static final Logger log = LoggerFactory.getLogger(CassiniModulationConfig.class);

    private ComponentType state = ComponentType.DIRECTION;

    private static final double OSNR_THRESHOLD_VALUE = 13.0;


    enum BitRate {
        GBPS_200(200),      // 200 Gbps
        GBPS_100(100),        // 100 Gbps
        GBPS_40(40),          // 40 Gbps
        GBPS_10(10);          // 10 Gbps

        private final long value;

        public long getValue() {
            return value;
        }

        BitRate(long value) {
            this.value = value;
        }
    }

    private NetconfController getController() {
        return handler().get(NetconfController.class);
    }

    /*
     *
     * Get the deviceId for which the methods apply.
     *
     * @return The deviceId as contained in the handler data
     */


    private DeviceId getDeviceId() {
        return handler().data().deviceId();
    }


    /**
     * Get the target Modulation Scheme on the component.
     *
     * @param port      the port
     * @param component the port component
     * @return ModulationScheme as per bitRate value
     **/
    @Override
    public Optional<ModulationScheme> getModulationScheme(PortNumber port, T component) {
        checkType(component);
        return state.getModulationScheme(port, component);
    }

    /**
     * Set the target Modulation Scheme on the component.
     *
     * @param port      the port
     * @param component the port component
     * @param bitRate   bit rate in bps
     **/
    @Override
    public void setModulationScheme(PortNumber port, T component, long bitRate) {
        checkType(component);
        state.setModulationScheme(port, component, bitRate);
    }

    /*
     *
     * Set the ComponentType to invoke proper methods for different template T.
     * @param component the component.
     */
    void checkType(Object component) {
        String clsName = component.getClass().getName();

        if (component instanceof Direction) {
            state = CassiniModulationConfig.ComponentType.DIRECTION;
        } else if (component instanceof OchSignal) {
            state = CassiniModulationConfig.ComponentType.OCHSIGNAL;
        } else {
            log.error("Cannot parse the component type {}.", clsName);
        }


        state.cassini = this;
    }

    /*
     *
     * Component type.
     */


    enum ComponentType {

        /*
         *
         * Direction.
         */


        DIRECTION() {
            @Override
            Optional<ModulationScheme> getModulationScheme(PortNumber port, Object component) {
                return super.getModulationScheme(port, component);
            }

            @Override
            void setModulationScheme(PortNumber port, Object component, long bitRate) {
                super.setModulationScheme(port, component, bitRate);
            }
        },

        /**
         * OchSignal.
         */


        OCHSIGNAL() {
            @Override
            Optional<ModulationScheme> getModulationScheme(PortNumber port, Object component) {
                return super.getModulationScheme(port, component);
            }

            @Override
            void setModulationScheme(PortNumber port, Object component, long bitRate) {
                super.setModulationScheme(port, component, bitRate);
            }
        };


        CassiniModulationConfig cassini;

        /*
         * mirror method in the internal class.
         * @param port port
         * @param component component
         * @return target modulation
         */
        Optional<ModulationScheme> getModulationScheme(PortNumber port, Object component) {
            NetconfSession session = NetconfSessionUtility
                    .getNetconfSession(cassini.getDeviceId(), cassini.getController());
            checkNotNull(session);
            String filter = createModulationFilter(cassini, port);
            StringBuilder rpcReq = new StringBuilder();
            rpcReq.append(RPC_TAG_NETCONF_BASE)
                    .append("<get>")
                    .append("<filter>")
                    .append(filter)
                    .append("</filter>")
                    .append("</get>")
                    .append(RPC_CLOSE_TAG);
            log.debug("RPC Call for Getting Modulation : \n {}", rpcReq.toString());
            XMLConfiguration xconf = NetconfSessionUtility.executeRpc(session, rpcReq.toString());
            try {
                HierarchicalConfiguration config =
                        xconf.configurationAt("components/component/optical-channel/config");

                String modulationScheme = String.valueOf(config.getString("modulation"));

                return Optional.of(ModulationScheme.valueOf(modulationScheme));
            } catch (IllegalArgumentException e) {
                return Optional.empty();
            }
        }

        /*
         * mirror method in the internal class.
         * @param port port
         * @param component component
         * @param power target value
         */
        void setModulationScheme(PortNumber port, Object component, long bitRate) {

            ModulationScheme modulation = null;
            String editConfig = null;
            double osnr = 0.0;
            boolean rpcResponse = false;
            boolean receiver = false;

            // TODO OSNR is valid only for receiver so need to modify
            /*if(component.toString().equals(Direction.INGRESS.toString())){
                receiver=true;
            }*/

            //check if bitrate is less than equal to 100 Gig
            if (bitRate <= BitRate.GBPS_100.value) {
                modulation = ModulationScheme.DP_QPSK;
                editConfig = modulationEditConfig(cassini, port, component, bitRate, modulation.name());
                //setting the modulation by calling rpc
                rpcResponse = setModulationRpc(port, component, editConfig);
                if (rpcResponse) {
                    // TODO OSNR is valid only for receiver so need to modify
                    osnr = fetchDeviceSnr(cassini, port);
                    if (osnr <= OSNR_THRESHOLD_VALUE) {
                        log.error("Channel not possible for this OSNR Value : {}", osnr);
                    }
                }
            } else { // check if bitrate is greater than 100 Gig
                modulation = ModulationScheme.DP_16QAM;
                editConfig = modulationEditConfig(cassini, port, component, bitRate, modulation.name());
                //setting the modulation by calling rpc
                rpcResponse = setModulationRpc(port, component, editConfig);
                if (rpcResponse) {
                    //TODO OSNR is valid only for receiver so need to modify
                    osnr = fetchDeviceSnr(cassini, port);
                    if (osnr <= OSNR_THRESHOLD_VALUE) {
                        modulation = ModulationScheme.DP_8QAM;
                        editConfig = modulationEditConfig(cassini, port, component, bitRate, modulation.name());
                        //setting the modulation by calling rpc
                        rpcResponse = setModulationRpc(port, component, editConfig);
                        if (rpcResponse) {
                            // TODO OSNR is valid only for receiver so need to modify
                            osnr = fetchDeviceSnr(cassini, port);
                            if (osnr <= OSNR_THRESHOLD_VALUE) {
                                log.warn("Channel not possible for this OSNR Value : {}." +
                                        " Please reduce the channel bitrate.", osnr);
                            }
                        }
                    }
                }
            }


        }


        /*
         * Get filtered content under <optical-channel><state>.
         * @param pc power config instance
         * @param port the port number
         * @param underState the filter condition
         * @return RPC reply
         */

        //RPC call for OSNR still not finalised, need to update Rpc once validated
        private static XMLConfiguration getTerminalDeviceSnr(CassiniModulationConfig config, PortNumber port) {
            NetconfSession session = NetconfSessionUtility
                    .getNetconfSession(config.getDeviceId(), config.getController());
            checkNotNull(session);
            String name = ocName(config, port);
            StringBuilder rpcReq = new StringBuilder(RPC_TAG_NETCONF_BASE);
            rpcReq.append("<get><filter>")
                    .append("<osnr xmlns=\"http://openconfig.net/yang/terminal-device\">")
                    .append("<instant/>")
                    .append("</osnr></filter></get>")
                    .append(RPC_CLOSE_TAG);
            log.info("RPC Call for Fetching OSNR :\n\n {}", rpcReq.toString());
            XMLConfiguration xconf = NetconfSessionUtility.executeRpc(session, rpcReq.toString());
            return xconf;
        }

        private static String createModulationFilter(CassiniModulationConfig modulationConfig, PortNumber portNumber) {
            String name = ocName(modulationConfig, portNumber);
            StringBuilder sb = new StringBuilder("<components xmlns=\"http://openconfig.net/yang/platform\">");
            sb.append("<component>").append("<name>").append(name).append("</name>");
            sb.append("<optical-channel xmlns=\"http://openconfig.net/yang/terminal-device\">");
            sb.append("<config><modulation/></config>");
            sb.append("</optical-channel>");
            sb.append("</component>").append("</components>");
            return sb.toString();
        }

        /**
         * Extract component name from portNumber's annotations.
         *
         * @param pc         modulation config instance
         * @param portNumber the port number
         * @return the component name
         */


        private static String ocName(CassiniModulationConfig pc, PortNumber portNumber) {
            DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
            DeviceId deviceId = pc.handler().data().deviceId();
            return deviceService.getPort(deviceId, portNumber).annotations().value("oc-name");
        }

        private static String channelSpacing(CassiniModulationConfig modulationConfig, PortNumber portNumber) {
            DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
            DeviceId deviceId = modulationConfig.handler().data().deviceId();
            String lambda = deviceService.getPort(deviceId, portNumber).annotations().value("lambda");

            ObjectMapper mapper = new ObjectMapper();
            String channelSpacing = "";
            try {
                JsonNode actualObj = mapper.readTree(lambda);
                JsonNode csNode = actualObj.get("channelSpacing");
                channelSpacing = csNode.asText();
                log.info("Channel_Spacing : " + channelSpacing);

            } catch (IOException e) {
                log.error("Error while parsing Json");
            }
            return channelSpacing;

        }

        private double fetchDeviceSnr(CassiniModulationConfig modulationConfig, PortNumber portNumber) {
            double osnr = 0.0;
            XMLConfiguration xconf = getTerminalDeviceSnr(cassini, portNumber);
            if (xconf == null) {
                return osnr;
            }
            try {
                HierarchicalConfiguration config =
                        xconf.configurationAt("data/components/component/optical-channel/state/osnr");
                osnr = Float.valueOf(config.getString("snr")).doubleValue();
                return osnr;
            } catch (IllegalArgumentException e) {
                return osnr;
            }
        }
        /*
         *
         * Parse filtering string from port and component.
         * @param portNumber Port Number
         * @param component port component (optical-channel)
         * @param bitRate bitRate in bps
         * @return filtering string in xml format

         */

        private String modulationEditConfig(CassiniModulationConfig modulationConfig, PortNumber portNumber,
                                            Object component, long bitRate, String modulation) {
            if (component != null) {
                String portName = ocName(modulationConfig, portNumber);
                //String channelSpacing = channelSpacing(modulationConfig,portNumber);
                //ChannelSpacing cs= ChannelSpacing.valueOf(channelSpacing);
                //double csValue= cs.frequency().asGHz();

                StringBuilder sb = new StringBuilder("<components xmlns=\"http://openconfig.net/yang/platform\">");
                sb.append("<component>").append("<name>").append(portName).append("</name>");
                if (modulation != null) {
                    // This is an edit-config operation.
                    sb.append("<optical-channel xmlns=\"http://openconfig.net/yang/terminal-device\">")
                            .append("<config>")
                            .append("<modulation>")
                            .append(modulation)
                            .append("</modulation>")
                            .append("</config>")
                            .append("</optical-channel>");
                }
                sb.append("</component>").append("</components>");
                return sb.toString();
            } else {
                log.error("Cannot process the component {}.", component.getClass());
                return null;
            }
        }

        private boolean setModulationRpc(PortNumber port, Object component, String editConfig) {
            NetconfSession session = NetconfSessionUtility
                    .getNetconfSession(cassini.getDeviceId(), cassini.getController());
            checkNotNull(session);
            boolean response = true;
            StringBuilder rpcReq = new StringBuilder();
            rpcReq.append(RPC_TAG_NETCONF_BASE)
                    .append("<edit-config>")
                    .append("<target><running/></target>")
                    .append("<config>")
                    .append(editConfig)
                    .append("</config>")
                    .append("</edit-config>")
                    .append(RPC_CLOSE_TAG);
            log.info("RPC call for Setting Modulation : {}", rpcReq.toString());
            XMLConfiguration xconf = NetconfSessionUtility.executeRpc(session, rpcReq.toString());
            if (xconf == null) {
                log.error("The <edit-config> operation to set target-modulation of Port({}:{}) is failed.",
                        port.toString(), component.toString());
            } else if (!xconf.getRoot().getChild(0).getName().equals("ok")) {
                    // The successful reply should be "<rpc-reply ...><ok /></rpc-reply>"
                response = false;
                log.error("The <edit-config> operation to set target-modulation of Port({}:{}) is failed.",
                        port.toString(), component.toString());
            }
            return response;
        }
    }


}
