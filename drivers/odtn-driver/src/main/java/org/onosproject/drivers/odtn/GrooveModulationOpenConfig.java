/*
 * Copyright 2020-present Open Networking Foundation
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
import org.onosproject.net.ModulationScheme;
import org.onosproject.net.PortNumber;
import org.onosproject.netconf.DatastoreId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/*
 * Driver Implementation of the ModulationConfig for OcNos standard open config based terminal devices.
 */
public class GrooveModulationOpenConfig<T> extends TerminalDeviceModulationConfig<T> {

    public static Logger log = LoggerFactory.getLogger(GrooveModulationOpenConfig.class);

    /**
     * Construct a rpc target power message.
     *
     * @return RPC payload
     */
    @Override
    public DatastoreId getDataStoreId() {
        return DatastoreId.RUNNING;
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

    @Override
    public ModulationScheme modulationSchemeType(String operationalMode) {
        /*Used for Internal Testing */
        //String modulationScheme="DP16QAM";
        ModulationScheme modulation;
        if (operationalMode.equalsIgnoreCase("62") ||
                operationalMode.equalsIgnoreCase("68")) {
            modulation = ModulationScheme.DP_16QAM;
        } else {
            modulation = ModulationScheme.DP_QPSK;
        }
        return modulation;
    }

    @Override
    public String modulationEditConfigRequestRpc(TerminalDeviceModulationConfig modulationConfig, PortNumber portNumber,
                                                 Object component, long bitRate, String modulation) {
        if (component != null) {
            // This is an edit-config operation.
            String portMode = modulation.equals("DP_QPSK") ? "QPSK_100G" : "16QAM_200G";
            String portName = state.ocName(modulationConfig, portNumber);
            Pattern portPattern = Pattern.compile(".*-[1]-[1-9][0-4]?-L[1-2]$"); // e.g. TRANSCEIVER-1-1-L1
            Matcher lineMatch = portPattern.matcher(portName);
            lineMatch.find();
            final String[] split = lineMatch.group(0).split("-");

            return "<ne xmlns=\"http://coriant.com/yang/os/ne\">" +
                    "        <shelf>" +
                    "          <shelf-id>" + split[1] + "</shelf-id>" +
                    "          <slot>" +
                    "            <slot-id>" + split[2] + "</slot-id>" +
                    "            <card>" +
                    "              <port>" +
                    "                <port-id>" + split[3].replace("L", "") + "</port-id>" +
                    "                <port-mode>" + portMode + "</port-mode>" +
                    "              </port>" +
                    "            </card>" +
                    "          </slot>" +
                    "        </shelf>" +
                    "      </ne>";
        } else {
            log.error("Cannot process the component {}.", component.getClass());
            return null;
        }
    }

    @Override
    public void setModulationSchemeProcessor(PortNumber port, Object component, long bitRate) {
        ModulationScheme modulation = null;
        if (bitRate <= BitRate.GBPS_100.value) {
            modulation = ModulationScheme.DP_QPSK;
        } else {
            modulation = ModulationScheme.DP_16QAM;

        }
        // TODO: Groove doesn't support to change the modulation format via OpenConfig directly
        //  without recommissioning the Optical Channel
        // Workaround: use Groove native model via port-mode change
        // String editConfig = modulationEditConfig(groove, port, component, bitRate, modulation.name());
        String editConfig = state.modulationEditConfig(state.terminalDevice, port, component,
                                                       bitRate, modulation.name());

        //setting the modulation by calling rpc
        state.setModulationRpc(port, component, editConfig);
    }

    /**
     * Get the Modulation Scheme on the component.
     *
     * @param conf HierarchicalConfiguration for path ../optical-channel/config
     * @return Optional<ModulationScheme>
     **/
    public Optional<ModulationScheme> getModulation(XMLConfiguration conf) {
        HierarchicalConfiguration config =
                conf.configurationAt("data/components/component/optical-channel/config");

        String operationalMode = String.valueOf(config.getString("operational-mode"));
        /*Used for Internal Testing */
        //String modulationScheme="DP16QAM";
        ModulationScheme modulation;
        if (operationalMode.equalsIgnoreCase("62") ||
                operationalMode.equalsIgnoreCase("68")) {
            modulation = ModulationScheme.DP_16QAM;
        } else {
            modulation = ModulationScheme.DP_QPSK;
        }
        return Optional.of(modulation);
    }
}

