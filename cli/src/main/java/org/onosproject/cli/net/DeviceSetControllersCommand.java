/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.cli.net;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.ControllerConfig;
import org.onosproject.net.behaviour.ControllerInfo;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Sets role of the controller node for the given infrastructure device.
 */
@Command(scope = "onos", name = "device-setcontrollers",
        description = "sets the list of controllers for the given infrastructure device")
public class DeviceSetControllersCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    String uri = null;

    @Argument(index = 1, name = "controllersListStrings", description = "list of " +
            "controllers to set for the specified device",
            required = true, multiValued = true)
    String[] controllersListStrings = null;

    private DeviceId deviceId;
    private List<ControllerInfo> newControllers = new ArrayList<>();

    @Override
    protected void execute() {

        Arrays.asList(controllersListStrings).forEach(
                cInfoString -> newControllers.add(parseCInfoString(cInfoString)));
        DriverService service = get(DriverService.class);
        deviceId = DeviceId.deviceId(uri);
        DriverHandler h = service.createHandler(deviceId);
        ControllerConfig config = h.behaviour(ControllerConfig.class);
        print("before:");
        config.getControllers().forEach(c -> print(c.target()));
        try {
            config.setControllers(newControllers);
        } catch (NullPointerException e) {
            print("No Device with requested parameters {} ", uri);
        }
        print("after:");
        config.getControllers().forEach(c -> print(c.target()));
        print("size %d", config.getControllers().size());
    }


    private ControllerInfo parseCInfoString(String cInfoString) {
        Annotations annotation;

        String[] config = cInfoString.split(",");
        if (config.length == 2) {
            String[] pair = config[1].split("=");

            if (pair.length == 2) {
                annotation = DefaultAnnotations.builder()
                        .set(pair[0], pair[1]).build();
            } else {
                print("Wrong format {}", config[1]);
                return null;
            }

            String[] data = config[0].split(":");
            String type = data[0];
            IpAddress ip = IpAddress.valueOf(data[1]);
            int port = Integer.parseInt(data[2]);

            return new ControllerInfo(ip, port, type, annotation);
        } else {
            print(config[0]);
            return new ControllerInfo(config[0]);
        }
    }
}
