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
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */
package org.onosproject.drivers.odtn;


import org.onosproject.net.ModulationScheme;
import org.onosproject.net.PortNumber;
import org.onosproject.netconf.DatastoreId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/*
 * Driver Implementation of the ModulationConfig for OcNos standard open config based terminal devices.
 */
public class CassiniModulationOpenConfig<T> extends TerminalDeviceModulationConfig<T> {

    public static Logger log = LoggerFactory.getLogger(CassiniModulationOpenConfig.class);

    /**
     * Construct a rpc target power message.
     *
     * @return RPC payload
     */
    @Override
    public DatastoreId getDataStoreId() {
        return DatastoreId.CANDIDATE;
    }

    /**
     * Construct a rpc target power message.
     *
     * @param name for optical channel name
     * @return RPC payload
     */
    @Override
    public StringBuilder createModulationFilterRequestRpc(String name) {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<name>").append(name).append("</name>");
        return rpc;
    }

    /*
     *
     * Parse filtering string from port and component.
     * @param portNumber Port Number
     * @param component port component (optical-channel)
     * @param bitRate bitRate in bps
     * @return filtering string in xml format

     */
    @Override
    public String modulationEditConfigRequestRpc(TerminalDeviceModulationConfig modulationConfig, PortNumber portNumber,
                                                 Object component, long bitRate, String modulation) {
        if (component != null) {
            // This is an edit-config operation.
            String portName = state.ocName(modulationConfig, portNumber); //oc1/0
            StringBuilder sb = new StringBuilder("<components xmlns=\"http://openconfig.net/yang/platform\">");
            sb.append("<component>");
            sb.append("<name>").append(portName).append("</name>");
            sb.append("<config>");
            sb.append("<name>").append(portName).append("</name>");
            sb.append("</config>");
            sb.append("<optical-channel xmlns=\"http://openconfig.net/yang/terminal-device\">")
                    .append("<config>")
                    .append("<modulation-format>")
                    .append(modulation)
                    .append("</modulation-format>")
                    .append("</config>")
                    .append("</optical-channel>");
            sb.append("</component>");
            sb.append("</components>");
            return sb.toString();
        } else {
            log.error("Cannot process the component {}.", component.getClass());
            return null;
        }
    }

    @Override
    public void setModulationSchemeProcessor(PortNumber port, Object component, long bitRate) {
        String modulation = null;
        String editConfig = null;
        if (bitRate <= BitRate.GBPS_100.value) {
            modulation = "dp_qpsk";
            editConfig = state.modulationEditConfig(state.terminalDevice, port, component, bitRate, modulation);
            //setting the modulation by calling rpc
            state.setModulationRpc(port, component, editConfig);
        } else { // check if bitrate is greater than 100 Gig
            modulation = "dp_16qam";
            editConfig = state.modulationEditConfig(state.terminalDevice, port, component, bitRate, modulation);
            //setting the modulation by calling rpc
            state.setModulationRpc(port, component, editConfig);
        }

    }

    @Override
    public ModulationScheme modulationSchemeType(String modulationScheme) {
        /*Used for Internal Testing */
        //String modulationScheme="DP16QAM";
        ModulationScheme modulation;
        if (modulationScheme.equalsIgnoreCase("dp-16-qam")) {
            modulation = ModulationScheme.DP_16QAM;
        } else if (modulationScheme.equalsIgnoreCase("dp-8-qam")) {
            modulation = ModulationScheme.DP_8QAM;
        } else {
            modulation = ModulationScheme.DP_QPSK;
        }
        return modulation;
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
                state = ComponentType.DIRECTION;
                break;
            case "org.onosproject.net.OchSignal":
                state = ComponentType.OCHSIGNAL;
                break;
            default:
                log.error("Cannot parse the component type {}.", clsName);
                log.info("The component content is {}.", component.toString());
        }

        state.terminalDevice = this;
    }
}
