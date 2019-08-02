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

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.commons.configuration.tree.xpath.XPathExpressionEngine;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.drivers.utilities.XmlConfigParser;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ModulationScheme;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.ModulationConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static com.google.common.base.Preconditions.checkNotNull;

/*
 * Driver Implementation of the ModulationConfig for OcNos based terminal devices.
 */
public class CassiniModulationOcNos<T> extends AbstractHandlerBehaviour implements ModulationConfig<T> {


    private static final String RPC_TAG_NETCONF_BASE =
            "<rpc xmlns=\"urn:ietf:params:xml:ns:netconf:base:1.0\">";

    private static final String RPC_CLOSE_TAG = "</rpc>";

    private static final Logger log = LoggerFactory.getLogger(CassiniModulationOcNos.class);

    private ComponentType state = ComponentType.DIRECTION;


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

    /**
     * Returns the NetconfSession with the device for which the method was called.
     *
     * @param deviceId device indetifier
     * @return The netconf session or null
     */


    private NetconfSession getNetconfSession(DeviceId deviceId) {
        log.info("Inside getNetconfSession () method for device : {}", deviceId);
        NetconfController controller = handler().get(NetconfController.class);
        NetconfDevice ncdev = controller.getDevicesMap().get(deviceId);
        if (ncdev == null) {
            log.trace("No netconf device, returning null session");
            return null;
        }
        return ncdev.getSession();
    }

    /*
     *
     * Get the deviceId for which the methods apply.
     *
     * @return The deviceId as contained in the handler data
     */


    private DeviceId did() {
        return handler().data().deviceId();
    }

    /**
     * Execute RPC request.
     *
     * @param session Netconf session
     * @param message Netconf message in XML format
     * @return XMLConfiguration object
     */

    private XMLConfiguration executeRpc(NetconfSession session, String message) {
        try {
            CompletableFuture<String> fut = session.rpc(message);
            String rpcReply = fut.get();
            XMLConfiguration xconf = (XMLConfiguration) XmlConfigParser.loadXmlString(rpcReply);
            xconf.setExpressionEngine(new XPathExpressionEngine());
            return xconf;
        } catch (NetconfException ne) {
            log.error("Exception on Netconf protocol: {}.", ne);
        } catch (InterruptedException ie) {
            log.error("Interrupted Exception: {}.", ie);
        } catch (ExecutionException ee) {
            log.error("Concurrent Exception while executing Netconf operation: {}.", ee);
        }
        return null;
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
        checkState(component);
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
        checkState(component);
        state.setModulationScheme(port, component, bitRate);
    }

    /*
     *
     * Set the ComponentType to invoke proper methods for different template T.
     * @param component the component.
     */
    void checkState(Object component) {
        String clsName = component.getClass().getName();
        switch (clsName) {
            case "org.onosproject.net.Direction":
                state = CassiniModulationOcNos.ComponentType.DIRECTION;
                break;
            case "org.onosproject.net.OchSignal":
                state = CassiniModulationOcNos.ComponentType.OCHSIGNAL;
                break;
            default:
                log.error("Cannot parse the component type {}.", clsName);
                log.info("The component content is {}.", component.toString());
        }

        state.cassini = this;
        log.info("Setting the state with clsName :{} ", clsName);
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
                log.info("Inside the enum setModulationScheme()");
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


        CassiniModulationOcNos cassini;

        /*
         * mirror method in the internal class.
         * @param port port
         * @param component component
         * @return target modulation
         */
        Optional<ModulationScheme> getModulationScheme(PortNumber port, Object component) {
            NetconfSession session = cassini.getNetconfSession(cassini.did());
            checkNotNull(session);
            String filter = createModulationFilter(cassini, port);
            StringBuilder rpcReq = new StringBuilder();
            rpcReq.append(RPC_TAG_NETCONF_BASE)
                    .append("<get>")
                    .append("<filter type='subtree'>")
                    .append(filter)
                    .append("</filter>")
                    .append("</get>")
                    .append(RPC_CLOSE_TAG);
            log.info("RPC Call for Getting Modulation : \n {}", rpcReq.toString());
            XMLConfiguration xconf = cassini.executeRpc(session, rpcReq.toString());
            try {
                HierarchicalConfiguration config =
                        xconf.configurationAt("data/components/component/optical-channel/config");

                String modulationScheme = String.valueOf(config.getString("modulation"));
                /*Used for Internal Testing */
                //String modulationScheme="DP16QAM";
                ModulationScheme modulation = ModulationScheme.valueOf(modulationScheme);
                return Optional.of(modulation);
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

            //check if bitrate is less than equal to 100 Gig
            if (bitRate <= BitRate.GBPS_100.value) {
                modulation = ModulationScheme.DP_QPSK;
                editConfig = modulationEditConfig(cassini, port, component, bitRate, modulation.name());
                //setting the modulation by calling rpc
                setModulationRpc(port, component, editConfig);
            } else { // check if bitrate is greater than 100 Gig
                modulation = ModulationScheme.DP_16QAM;
                editConfig = modulationEditConfig(cassini, port, component, bitRate, modulation.name());
                //setting the modulation by calling rpc
                setModulationRpc(port, component, editConfig);
            }


        }

        private static String createModulationFilter(CassiniModulationOcNos modulationConfig, PortNumber portNumber) {
            String name = ocName(modulationConfig, portNumber);
            StringBuilder sb = new StringBuilder("<components xmlns=\"http://openconfig.net/yang/platform\">");
            sb.append("<component>").append("<name>").append(name).append("</name>");
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


        private static String ocName(CassiniModulationOcNos pc, PortNumber portNumber) {
            DeviceService deviceService = DefaultServiceDirectory.getService(DeviceService.class);
            DeviceId deviceId = pc.handler().data().deviceId();
            return deviceService.getPort(deviceId, portNumber).annotations().value("oc-name");
        }

        /*
         *
         * Parse filtering string from port and component.
         * @param portNumber Port Number
         * @param component port component (optical-channel)
         * @param bitRate bitRate in bps
         * @return filtering string in xml format

         */
        private String modulationEditConfig(CassiniModulationOcNos modulationConfig, PortNumber portNumber,
                                            Object component, long bitRate, String modulation) {
            /*
            <cmmOpticalControllerTable xmlns="http://www.ipinfusion.com/CMLSchema/zebos">
                <slotIndex>1</slotIndex>   ==============>> optical-module-slot
                <cmmOpticalChannelInterfaceTable>
                    <netIndex>0</netIndex>    =============>> optical-channel-number (Reference to line-port)
                    <modulationFormat>dp-qpsk</modulationFormat>
                 </cmmOpticalNetworkInterfaceTable>
            </cmmOpticalControllerTable>
             */
            String modulationOcNos = convertToOcNosModulation(modulation);
            if (component != null) {
                String portName = ocName(modulationConfig, portNumber); //oc1/1
                String slotName = portName.split("/")[0]; //get oc1
                String slotIndex = slotName.substring(slotName.length() - 1); //then just 1
                String netIndex = portName.split("/")[1]; //get 1 after the /
                StringBuilder sb = new StringBuilder("<cmmOpticalControllerTable " +
                        "xmlns=\"http://www.ipinfusion.com/CMLSchema/zebos\">");
                sb.append("<slotIndex>").append(slotIndex).append("</slotIndex>");
                if (modulation != null) {
                    // This is an edit-config operation.
                    sb.append("<cmmOpticalChannelInterfaceTable>")
                            .append("<netIndex>").append(netIndex).append("<netIndex>")
                            .append("<modulationFormat>")
                            .append(modulationOcNos)
                            .append("</modulationFormat>")
                            .append("</cmmOpticalNetworkInterfaceTable>");
                }
                sb.append("</cmmOpticalControllerTable>");
                return sb.toString();
            } else {
                log.error("Cannot process the component {}.", component.getClass());
                return null;
            }
        }

        private String convertToOcNosModulation(String modulation) {
            if (modulation.equals(ModulationScheme.DP_QPSK.name())) {
                return "dp_qpsk";
            } else {
                return "dp_16qam";
            }
        }

        private boolean setModulationRpc(PortNumber port, Object component, String editConfig) {
            NetconfSession session = cassini.getNetconfSession(cassini.did());
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
            XMLConfiguration xconf = cassini.executeRpc(session, rpcReq.toString());

            // The successful reply should be "<rpc-reply ...><ok /></rpc-reply>"
            if (!xconf.getRoot().getChild(0).getName().equals("ok")) {
                response = false;
                log.error("The <edit-config> operation to set target-modulation of Port({}:{}) is failed.",
                        port.toString(), component.toString());
            }
            return response;
        }
    }


}
