/*
 * Copyright 2014-present Open Networking Foundation
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onosproject.utils.Comparators;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.onosproject.net.DeviceId.deviceId;

/**
 * Lists all ports or all ports of a device.
 */
@Service
@Command(scope = "onos", name = "ports",
         description = "Lists all ports or all ports of a device")
public class DevicePortsListCommand extends DevicesListCommand {

    private static final String FMT = "  port=%s, state=%s, type=%s, speed=%s %s";

    @Option(name = "-e", aliases = "--enabled", description = "Show only enabled ports",
            required = false, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    private boolean enabled = false;

    @Option(name = "-d", aliases = "--disabled", description = "Show only disabled ports",
            required = false, multiValued = false)
    private boolean disabled = false;

    @Argument(index = 0, name = "uri", description = "Device ID",
              required = false, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    protected String uri = null;

    @Override
    protected void doExecute() {
        DeviceService service = get(DeviceService.class);
        if (uri == null) {
            if (outputJson()) {
                print("%s", jsonPorts(service, getSortedDevices(service)));
            } else {
                for (Device device : getSortedDevices(service)) {
                    printDevice(service, device);
                    printPorts(service, device);
                }
            }

        } else {
            Device device = service.getDevice(deviceId(uri));
            if (device == null) {
                error("No such device %s", uri);
            } else if (outputJson()) {
                print("%s", jsonPorts(service, new ObjectMapper(), device));
            } else {
                printDevice(service, device);
                printPorts(service, device);
            }
        }
    }

    /**
     * Produces JSON array containing ports of the specified devices.
     *
     * @param service device service
     * @param devices collection of devices
     * @return JSON array
     */
    public JsonNode jsonPorts(DeviceService service, Iterable<Device> devices) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Device device : devices) {
            result.add(jsonPorts(service, mapper, device));
        }
        return result;
    }

    /**
     * Produces JSON array containing ports of the specified device.
     *
     * @param service device service
     * @param mapper  object mapper
     * @param device  infrastructure devices
     * @return JSON array
     */
    public JsonNode jsonPorts(DeviceService service, ObjectMapper mapper, Device device) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode ports = mapper.createArrayNode();
        for (Port port : service.getPorts(device.id())) {
            if (isIncluded(port)) {
                ports.add(mapper.createObjectNode()
                                  .put("element", device.id().toString())
                                  .put("port", port.number().toString())
                                  .put("isEnabled", port.isEnabled())
                                  .put("type", port.type().toString().toLowerCase())
                                  .put("portSpeed", port.portSpeed())
                                  .set("annotations", annotations(mapper, port.annotations())));
            }
        }
        result.set("device", jsonForEntity(device, Device.class));
        result.set("ports", ports);
        return result;
    }

    // Determines if a port should be included in output.
    protected boolean isIncluded(Port port) {
        return enabled && port.isEnabled() || disabled && !port.isEnabled() ||
                !enabled && !disabled;
    }

    protected void printPorts(DeviceService service, Device device) {
        List<Port> ports = new ArrayList<>(service.getPorts(device.id()));
        Collections.sort(ports, Comparators.PORT_COMPARATOR);
        for (Port port : ports) {
            if (!isIncluded(port)) {
                continue;
            }
            String portName = port.number().toString();
            Object portIsEnabled = port.isEnabled() ? "enabled" : "disabled";
            String portType = port.type().toString().toLowerCase();
            String annotations = annotations(port.annotations());
            print(FMT, portName, portIsEnabled, portType, port.portSpeed(), annotations);
        }
    }
}
