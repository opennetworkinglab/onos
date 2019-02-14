/*
 * Copyright 2017-present Open Networking Foundation
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import com.google.common.collect.Lists;
import com.google.common.collect.ImmutableList;
import org.onlab.packet.IpAddress;
import org.onosproject.net.behaviour.ControllerConfig;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.slf4j.Logger;
import com.fasterxml.jackson.databind.JsonNode;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Sets, gets and removes the openflow controller configuration from a Arista Rest device.
 */
public class ControllerConfigAristaImpl extends AbstractHandlerBehaviour implements ControllerConfig {

    private static final String SHOW_CONTROLLER_CMD = "show openflow";
    private static final String CONFIGURE_TERMINAL = "configure";
    private static final String OPENFLOW_CMD = "openflow";
    private static final String SET_CONTROLLER_CMD = "controller tcp:%s:%d";
    private static final String NO_SHUTDOWN_CMD = "no shutdown";
    private static final String SHUTDOWN_CMD = "shutdown";
    private static final String REMOVE_CONTROLLER_CMD = "no controller tcp:%s:%d";
    private static final String COPY_RUNNING_CONFIG = "copy running-config startup-config";
    private static final String CONTROLLER_INFO = "controllersInfo";
    private static final String CONTROLLER_ADDR = "controllerAddr";
    private static final String CONTROLLER_IP = "ip";
    private static final String CONTROLLER_PORT = "port";
    private static final String PROTOCOL_TCP = "tcp";


    private static final int MAX_CONTROLLERS = 8;

    private final Logger log = getLogger(getClass());

    @Override
    public List<ControllerInfo> getControllers() {
        log.debug("Arista get Controllers");

        List<ControllerInfo> controllers = new ArrayList<>();
        Optional<JsonNode> res = AristaUtils.retrieveCommandResult(handler(), SHOW_CONTROLLER_CMD);
        if (res == null) {
            log.warn("There is no connected controller.");
            return controllers;
        }

        JsonNode controllerInfo = res.get().findValue(CONTROLLER_INFO);
        Iterator<JsonNode> controlleriter = controllerInfo.iterator();
        while (controlleriter.hasNext()) {
            JsonNode temp1 = controlleriter.next();
            if (temp1.has(CONTROLLER_ADDR)) {
                JsonNode controllerAddr = temp1.get(CONTROLLER_ADDR);
                if (controllerAddr.has(CONTROLLER_IP) && controllerAddr.has(CONTROLLER_PORT)) {
                    String ip = controllerAddr.get(CONTROLLER_IP).asText();
                    int port = controllerAddr.get(CONTROLLER_PORT).asInt();
                    ControllerInfo info = new ControllerInfo(IpAddress.valueOf(ip), port, PROTOCOL_TCP);
                    controllers.add(info);
                    log.debug("Controller Information {}", info.target());
                }
            }
        }

        return ImmutableList.copyOf(controllers);
    }

    @Override
    public void setControllers(List<ControllerInfo> controllers) {
        log.debug("Arista set Controllers");

        List<String> cmds = new ArrayList<>();
        cmds.add(CONFIGURE_TERMINAL);
        cmds.add(OPENFLOW_CMD);
        //The Arista switch supports up to 8 multi-controllers.
        controllers.stream().limit(MAX_CONTROLLERS).forEach(c -> cmds
                .add(String.format(SET_CONTROLLER_CMD, c.ip().toString(), c.port())));
        if (controllers.size() > MAX_CONTROLLERS) {
            log.warn(" {} Arista Switch maximun 8 controllers, not adding {} excessive ones",
                    handler().data().deviceId(), controllers.size() - MAX_CONTROLLERS);
        }
        cmds.add(NO_SHUTDOWN_CMD);
        cmds.add(COPY_RUNNING_CONFIG);

        AristaUtils.retrieveCommandResult(handler(), cmds);
    }


    @Override
    public void removeControllers(List<ControllerInfo> controllers) {
        log.debug("Arista remove Controllers");

        List<String> cmds = Lists.newArrayList();
        cmds.add(CONFIGURE_TERMINAL);
        cmds.add(OPENFLOW_CMD);
        controllers.stream().limit(MAX_CONTROLLERS).forEach(c -> cmds
                .add(String.format(REMOVE_CONTROLLER_CMD, c.ip().toString(), c.port())));

        AristaUtils.retrieveCommandResult(handler(), cmds);
    }
}
