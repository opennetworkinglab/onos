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
import org.onosproject.net.behaviour.ControllerConfig;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.protocol.rest.RestSBController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class DellRestOpenFlowControllerConfig extends AbstractHandlerBehaviour
        implements ControllerConfig {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String TCP = "tcp";
    private static final String CLI_REQUEST = "/running/dell/_operations/cli";
    private static final String SET_CONTROLLERS_XML = "<input>" +
            "<config-commands>" +
            "openflow of-instance 1\\r\\n" + // for now supports only one instance
            "shutdown\\r\\n" +
            "%s" +
            "of-version 1.3\\r\\n" +
            "no shutdown" + // TODO: check previous state
            "</config-commands>" +
            "</input>";
    private static final String SET_CONTROLLER = "controller %d %s port %d tcp\\r\\n";
    private static final String NO_CONTROLLER = "no controller %d\\r\\n";
    private static final int MAX_CONTROLLERS = 2;

    @Override
    public List<ControllerInfo> getControllers() {
        return new ArrayList<>();
    }

    // Example test with ONOS CLI:
    // device-setcontrollers rest:10.251.217.143:8008 tcp:1.2.3.4:2222

    // TODO: assumes that of-instance 1 was in "no shutdown" state,
    // check previous state
    @Override
    public void setControllers(List<ControllerInfo> controllers) {

        // Dell supports max 2 controllers per of-instance
        if (controllers.size() > MAX_CONTROLLERS) {
            log.warn("Cannot set more than 2 controllers.");
        }

        DriverHandler handler = handler();
        RestSBController controller = checkNotNull(handler.get(RestSBController.class));
        DeviceId deviceId = handler.data().deviceId();

        String controllerCommands = getControllerCommands(controllers).stream()
                .reduce(String::concat)
                .orElse("");

        InputStream payload = new StringBufferInputStream(String.format(SET_CONTROLLERS_XML, controllerCommands));
        String resp = controller.post(deviceId, CLI_REQUEST, payload, MediaType.valueOf("*/*"), String.class);
        log.info("{}", resp);
    }

    // Get controller commands for maximum first 2 controllers
    private List<String> getControllerCommands(List<ControllerInfo> controllers) {
        List<String> controllerCommands = new ArrayList<>();

        for (int controllerNum = 0; controllerNum < MAX_CONTROLLERS; ++controllerNum) {
            if (controllers.size() > controllerNum) {
                ControllerInfo controllerInfo = controllers.get(controllerNum);
                // Only "tcp" type is supported
                if (controllerInfo.type().equals(TCP)) {
                    String ip = controllerInfo.ip().toString();
                    int port = controllerInfo.port();
                    controllerCommands.add(String.format(SET_CONTROLLER, controllerNum + 1, ip, port));
                } else {
                    log.warn("Controller type can only be 'tcp'");
                    controllerCommands.add(String.format(NO_CONTROLLER, controllerNum + 1));
                }
            } else {
                // remove existing controller
                controllerCommands.add(String.format(NO_CONTROLLER, controllerNum + 1));
            }
        }

        return controllerCommands;
    }
}
