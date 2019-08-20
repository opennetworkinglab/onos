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
 */

package org.onosproject.drivers.dellrest;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.protocol.rest.RestSBController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

public class DellRestBridgeConfig extends AbstractHandlerBehaviour
        implements BridgeConfig {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String CLI_REQUEST = "/running/dell/_operations/cli";
    private static final String CONFIG_COMMANDS = "<input>" +
            "<config-commands>" +
            "openflow of-instance 1\\r\\n" +
            "shutdown\\r\\n" +
            "exit\\r\\n" +
            "%s" + // interface commands
            "openflow of-instance 1\\r\\n" +
            "no shutdown" +
            "</config-commands>" +
            "</input>";
    private static final String PORT_ADD_COMMANDS = "interface %s\\r\\n" + // interface name
            "of-instance 1\\r\\n" +
            "no shutdown\\r\\n" +
            "exit\\r\\n";
    private static final String PORT_REMOVE_COMMANDS = "interface %s\\r\\n" + // interface name
            "no of-instance\\r\\n" +
            "exit\\r\\n";
    private static final String TENGINTERFACE = "TenGigabitEthernet ";
    private static final String FORTYGINTERFACE = "FortyGigE ";
    private static final String SLASH = "/";
    private static final String DASH = "-";

    @Override
    public boolean addBridge(BridgeDescription bridgeDescription) {
        return false;
    }

    @Override
    public void deleteBridge(BridgeName bridgeName) {

    }

    @Override
    public Collection<BridgeDescription> getBridges() {
        return null;
    }

    @Override
    public void addPort(BridgeName bridgeName, String portName) {
        // bridgeName is not used
        checkNotNull(portName);
        portName = portNameForCli(portName);
        String portAddCommands = String.format(PORT_ADD_COMMANDS, portName);

        DriverHandler handler = handler();
        RestSBController controller = checkNotNull(handler.get(RestSBController.class));
        DeviceId deviceId = handler.data().deviceId();

        InputStream payload = new StringBufferInputStream(String.format(CONFIG_COMMANDS, portAddCommands));
        String resp = controller.post(deviceId, CLI_REQUEST, payload, MediaType.valueOf("*/*"), String.class);
        //TODO Parse resp and if error, return false
        log.info("{}", resp);
        return;
    }

    @Override
    public void deletePort(BridgeName bridgeName, String portName) {
        // bridgeName is not used
        checkNotNull(portName);
        portName = portNameForCli(portName);
        String portRemoveCommands = String.format(PORT_REMOVE_COMMANDS, portName);

        DriverHandler handler = handler();
        RestSBController controller = checkNotNull(handler.get(RestSBController.class));
        DeviceId deviceId = handler.data().deviceId();

        InputStream payload = new StringBufferInputStream(String.format(CONFIG_COMMANDS, portRemoveCommands));
        String resp = controller.post(deviceId, CLI_REQUEST, payload, MediaType.valueOf("*/*"), String.class);
        log.info("{}", resp);
    }

    @Override
    public Collection<PortDescription> getPorts() {
        return null;
    }

    @Override
    public Set<PortNumber> getPortNumbers() {
        return null;
    }

    @Override
    public List<PortNumber> getLocalPorts(Iterable<String> ifaceIds) {
        return null;
    }

    private String portNameForCli(String portName) {
        if (portName.toLowerCase().startsWith("tengig-")) {
            // change eg. "tengig-1-1" to "TenGigabitEthernet 1/1"
            portName = portName.replaceFirst("tengig-", TENGINTERFACE);
            return  portName.replaceFirst(DASH, SLASH);
        } else if (portName.toLowerCase().startsWith("fortygig-")) {
            // change eg. "fortygig-1-49" to "FortyGigE 1/49"
            portName = portName.replaceFirst("fortygig-", FORTYGINTERFACE);
            return  portName.replaceFirst(DASH, SLASH);
        } else {
            return portName;
        }
    }
}
