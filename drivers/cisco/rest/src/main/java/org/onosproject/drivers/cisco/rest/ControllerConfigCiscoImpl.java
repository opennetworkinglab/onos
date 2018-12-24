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
 */

package org.onosproject.drivers.cisco.rest;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.onlab.packet.IpAddress;
import org.onosproject.drivers.cisco.rest.NxApiRequest.CommandType;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerConfig;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Gets and Sets the openflow controller.
 */
public class ControllerConfigCiscoImpl extends AbstractHandlerBehaviour implements ControllerConfig {
    private final Logger log = getLogger(getClass());

    private static final String SHOW_OF_CONTROLLER_CMD = "show openflow switch 1 controllers";
    private static final String OPENFLOW_CMD = "openflow";
    private static final String SW1_CMD = "switch 1";
    private static final String DELETE_OF_CONFIG = "no switch 1";
    private static final String PROTO_VER_CMD = "protocol-version 1.3";
    private static final String N9K_PIPELINE_CMD = "pipeline 201";
    private static final String N7K_PIPELINE_CMD = "pipeline 301";
    private static final String OF_CONTROLLER_CONF_CMD = "controller ipv4 %s port %d vrf %s security none";
    private static final String NO_SHUTDOWN_CMD = "no shutdown";
    private static final String SHOW_VERSION = "show version";
    private static final String COPY_RUNNING_CONFIG = "copy running-config startup-config";

    @Override
    public List<ControllerInfo> getControllers() {
        List<ControllerInfo> controllers = new ArrayList<ControllerInfo>();
        DeviceId deviceId = handler().data().deviceId();

        String response;

            // "show openflow switch 1 controller" command only shows outputs with cli_ascii command type
            response = NxApiRequest.post(handler(), SHOW_OF_CONTROLLER_CMD, CommandType.CLI_ASCII);

        if (response == null) {
            log.error("Failed to perform {} command on the device {} Response has Error/null",
                    SHOW_OF_CONTROLLER_CMD, deviceId);
            return controllers;
        }

        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode json = om.readTree(response);
            if (json.has("result")) {
                JsonNode res = json.get("result");
                String msg = res.findValue("msg").asText();
                controllers.addAll(parseControllerInfo(msg));
            } else if (json.has("error")) {
                log.error("{} Response has IllegalStateException Error/null", deviceId);
                return controllers;
            }
        } catch (IOException e) {
            log.error("Exception thrown", e);
        }

        return controllers;
    }

    @Override
    public void setControllers(List<ControllerInfo> controllers) {
        DriverHandler handler = handler();
        DeviceId deviceId = handler.data().deviceId();

        List<String> cmds = new ArrayList<>();
        cmds.add(OPENFLOW_CMD);
        cmds.add(DELETE_OF_CONFIG);
        cmds.add(SW1_CMD);
        cmds.add(PROTO_VER_CMD);
        if (checkSwitchN7K(handler)) {
            cmds.add(N7K_PIPELINE_CMD);
            log.info("This is N7K Switch");
        } else {
            cmds.add(N9K_PIPELINE_CMD);
            log.info("This is N9K Switch");
        }
        // can configure up to eight controllers
        controllers.stream().limit(8).forEach(c -> cmds
                .add(String.format(OF_CONTROLLER_CONF_CMD, c.ip().toString(), c.port(), "management")));
        cmds.add(NO_SHUTDOWN_CMD);
        cmds.add(COPY_RUNNING_CONFIG);
        String response = NxApiRequest.postClis(handler, cmds);
        if (Objects.isNull(response)) {
            throw new NullPointerException("Response is null");
        }

        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode json = om.readTree(response);
            //TODO parse error messages.
            if (json.has("error")) {
                log.error("{} Response has IllegalStateException Error", deviceId);
                return;
            }
        } catch (IOException e) {
            log.error("Exception thrown", e);
        }
    }

    @Override
    public void removeControllers(List<ControllerInfo> controllers) {
        DriverHandler handler = handler();
        DeviceId deviceId = handler.data().deviceId();

        List<String> cmds = new ArrayList<>();
        cmds.add(OPENFLOW_CMD);
        cmds.add(DELETE_OF_CONFIG);
        cmds.add(COPY_RUNNING_CONFIG);

        String response = NxApiRequest.postClis(handler, cmds);
        if (Objects.isNull(response)) {
            log.error(" Device {} Response is null", deviceId);
            return;
        }

        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode json = om.readTree(response);
            if (json.has("errors")) {
                log.error("{} Response has JSON Format Error {}", deviceId, json);
                return;
            }
            //TODO parse error messages.
        } catch (IOException e) {
            log.error("Exception thrown", e);
        }
    }

    private List<ControllerInfo> parseControllerInfo(String data) {
        final String regex = "(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}):(\\d{1,5})";
        Pattern pattern = Pattern.compile(regex);
        Matcher match = pattern.matcher(data);
        List<ControllerInfo> controllers = new ArrayList<ControllerInfo>();
        while (match.find()) {
            String str = match.group();
            String[] ips = str.split(":");
            ControllerInfo info = new ControllerInfo(IpAddress.valueOf(ips[0]),
                    Integer.parseInt(ips[1]), "tcp");
            controllers.add(info);
        }

        return controllers;
    }


    public boolean checkSwitchN7K(DriverHandler handler) {

        String response = NxApiRequest.postCli(handler, SHOW_VERSION);

        String msg = "";

        try {
            ObjectMapper om = new ObjectMapper();
            JsonNode json = om.readTree(response);
            JsonNode res = json.get("result");
            msg = res.findValue("msg").asText();
        } catch (IOException e) {
            log.error("Exception thrown", e);
            return false;
        }

        if (msg.contains("Nexus7700")) {
            return true;
        }
        return false;
    }
}
