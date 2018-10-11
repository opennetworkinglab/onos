/*
 * Copyright 2015-present Open Networking Foundation
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

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
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
@Service
@Command(scope = "onos", name = "device-setcontrollers",
        description = "sets the list of controllers for the given infrastructure device")
public class DeviceSetControllersCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String uri = null;

    @Argument(index = 1, name = "controllersListStrings", description = "list of " +
            "controllers to set for the specified device",
            required = false, multiValued = true)
    String[] controllersListStrings = null;

    @Option(name = "--remove",
            description = "Remove specified controllers configuration")
    private boolean removeCont = false;

    @Option(name = "--remove-all",
            description = "Remove all controllers configuration, " +
                    "does not require any input")
    private boolean removeAll = false;

    private DeviceId deviceId;
    private List<ControllerInfo> controllers = new ArrayList<>();

    @Override
    protected void doExecute() {

        if (controllersListStrings == null && !removeCont && !removeAll) {
            print("No controller are given, skipping.");
            return;
        }
        if (controllersListStrings != null) {
            Arrays.asList(controllersListStrings).forEach(
                    cInfoString -> {
                        ControllerInfo controllerInfo = parseCInfoString(cInfoString);
                        if (controllerInfo != null) {
                            controllers.add(controllerInfo);
                        }
                    });
        }
        DriverService service = get(DriverService.class);
        deviceId = DeviceId.deviceId(uri);
        DriverHandler h = service.createHandler(deviceId);
        ControllerConfig config = h.behaviour(ControllerConfig.class);
        print("before:");
        config.getControllers().forEach(c -> print(c.target()));
        try {
            if (removeAll) {
                if (!controllers.isEmpty()) {
                    print("Controllers list should be empty to remove all controllers");
                } else {
                    List<ControllerInfo> controllersToRemove = config.getControllers();
                    controllersToRemove.forEach(c -> print("Will remove " + c.target()));
                    config.removeControllers(controllersToRemove);
                }
            } else {
                if (controllers.isEmpty()) {
                    print("Controllers list is empty, cannot set/remove empty controllers");
                } else {
                    if (removeCont) {
                        print("Will remove specified controllers");
                        config.removeControllers(controllers);
                    } else {
                        print("Will add specified controllers");
                        config.setControllers(controllers);
                    }
                }
            }
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

            return getControllerInfo(annotation, config[0]);
        } else {
            return getControllerInfo(null, config[0]);
        }
    }

    private ControllerInfo getControllerInfo(Annotations annotation, String s) {
        String[] data = s.split(":");
        if (data.length != 3) {
            print("Wrong format of the controller %s, should be in the format <protocol>:<ip>:<port>", s);
            return null;
        }
        String type = data[0];
        IpAddress ip = IpAddress.valueOf(data[1]);
        int port = Integer.parseInt(data[2]);
        if (annotation != null) {
            return new ControllerInfo(ip, port, type, annotation);
        }
        return new ControllerInfo(ip, port, type);
    }
}
