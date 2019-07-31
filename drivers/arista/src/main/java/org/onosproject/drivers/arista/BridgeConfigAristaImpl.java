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

package org.onosproject.drivers.arista;


import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.BridgeConfig;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.BridgeName;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;

public class BridgeConfigAristaImpl extends AbstractHandlerBehaviour implements BridgeConfig {
    private final Logger log = getLogger(getClass());

    private static final String CONFIGURE_TERMINAL = "configure";
    private static final String OPENFLOW_CMD = "openflow";
    private static final String BIND_CMD = "bind interface %s";
    private static final String NO_BIND_CMD = "no bind interface %s";
    private static final String INTERFACE_CMD = "interface %s";
    private static final String NO_SPEED = "no speed";
    private static final String SPEED_40G_FULL_CMD = "speed forced 40gfull";


    @Override
    public boolean addBridge(BridgeDescription bridgeDescription) {
        log.warn("addBridge is not supported");
        return false;
    }

    @Override
    public void deleteBridge(BridgeName bridgeName) {
        log.warn("deleteBridge is not supported");
    }

    @Override
    public Collection<BridgeDescription> getBridges() {
        log.warn("deleteBridge is not supported");
        return null;
    }

    @Override
    public void addPort(BridgeName bridgeName, String portName) {
        List<String> cmds = new ArrayList<>();
        cmds.add(CONFIGURE_TERMINAL);
        cmds.add(OPENFLOW_CMD);
        cmds.add(String.format(BIND_CMD, portName));

        AristaUtils.getWithChecking(handler(), cmds);
    }

    @Override
    public void addPorts(BridgeName bridgeName, List<String> portNames) {
        List<String> cmds = new ArrayList<>();
        cmds.add(CONFIGURE_TERMINAL);
        cmds.add(OPENFLOW_CMD);
        for (String portName : portNames) {
            cmds.add(String.format(BIND_CMD, portName));
        }

        AristaUtils.getWithChecking(handler(), cmds);
    }

    @Override
    public void deletePort(BridgeName bridgeName, String portName) {
        List<String> cmds = new ArrayList<>();
        cmds.add(CONFIGURE_TERMINAL);
        cmds.add(OPENFLOW_CMD);
        cmds.add(String.format(NO_BIND_CMD, portName));

        AristaUtils.getWithChecking(handler(), cmds);
    }

    @Override
    public void deletePorts(BridgeName bridgeName, List<String> portNames) {
        List<String> cmds = new ArrayList<>();
        cmds.add(CONFIGURE_TERMINAL);
        cmds.add(OPENFLOW_CMD);
        for (String portName : portNames) {
            cmds.add(String.format(NO_BIND_CMD, portName));
        }

        AristaUtils.getWithChecking(handler(), cmds);
    }

    @Override
    public Collection<PortDescription> getPorts() {
        // TODO need to implement
        log.warn("not implemented yet");
        return null;
    }

    @Override
    public Set<PortNumber> getPortNumbers() {
        // TODO need to implement
        log.warn("not implemented yet");
        return null;
    }

    @Override
    public List<PortNumber> getLocalPorts(Iterable<String> ifaceIds) {
        // TODO need to implement
        log.warn("not implemented yet");
        return null;
    }
}
