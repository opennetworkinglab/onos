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

import org.onosproject.net.PortNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * Driver Implementation of the ModulationConfig for OcNos based terminal devices.
 */
public class CassiniModulationOcNos<T> extends TerminalDeviceModulationConfig<T> {


    private static Logger log = LoggerFactory.getLogger(CassiniModulationOcNos.class);



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

    /**
     * Construct a rpc target power message.
     *
     * @param filter to build rpc
     * @return RPC payload
     */
    @Override
    public StringBuilder getModulationSchemeRequestRpc(String filter) {
        StringBuilder rpc = new StringBuilder();
        rpc.append("<get>")
                .append("<filter>")
                .append(filter)
                .append("</filter>")
                .append("</get>");
        return rpc;
    }

    @Override
    public void setModulationSchemeProcessor(PortNumber port, Object component, long bitRate) {
        String modulation = null;
        String editConfig = null;
        if (bitRate <= TerminalDeviceModulationConfig.BitRate.GBPS_100.value) {
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
    public String modulationEditConfigRequestRpc(TerminalDeviceModulationConfig modulationConfig, PortNumber portNumber,
                                                 Object component, long bitRate, String modulation) {
        if (component != null) {
            String portName = state.ocName(modulationConfig, portNumber); //oc1/1
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
                        .append(modulation)
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
        log.info("Setting the state with clsName :{} ", clsName);
    }


}
