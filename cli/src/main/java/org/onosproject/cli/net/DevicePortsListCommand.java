/*
 * Copyright 2014-present Open Networking Laboratory
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
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.util.Frequency;
import org.onosproject.utils.Comparators;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.optical.OduCltPort;
import org.onosproject.net.optical.OmsPort;
import org.onosproject.net.optical.OpticalDevice;
import org.onosproject.net.optical.OtuPort;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.optical.device.OpticalDeviceServiceView.opticalView;

/**
 * Lists all ports or all ports of a device.
 */
@Command(scope = "onos", name = "ports",
         description = "Lists all ports or all ports of a device")
public class DevicePortsListCommand extends DevicesListCommand {

    private static final String FMT = "  port=%s, state=%s, type=%s, speed=%s %s";
    private static final String FMT_OCH = "  port=%s, state=%s, type=%s, signalType=%s, isTunable=%s %s";
    private static final String FMT_ODUCLT_OTU = "  port=%s, state=%s, type=%s, signalType=%s %s";
    private static final String FMT_OMS = "  port=%s, state=%s, type=%s, Freqs= %s / %s / %s GHz, totalChannels=%s %s";

    @Option(name = "-e", aliases = "--enabled", description = "Show only enabled ports",
            required = false, multiValued = false)
    private boolean enabled = false;

    @Option(name = "-d", aliases = "--disabled", description = "Show only disabled ports",
            required = false, multiValued = false)
    private boolean disabled = false;

    @Argument(index = 0, name = "uri", description = "Device ID",
              required = false, multiValued = false)
    String uri = null;

    @Override
    protected void execute() {
        DeviceService service = opticalView(get(DeviceService.class));
        if (uri == null) {
            if (outputJson()) {
                print("%s", jsonPorts(service, getSortedDevices(service)));
            } else {
                for (Device device : getSortedDevices(service)) {
                    printDevice(service, device);
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
                                  .put("port", portName(port.number()))
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

    private String portName(PortNumber port) {
        return port.equals(PortNumber.LOCAL) ? "local" : port.toString();
    }

    // Determines if a port should be included in output.
    private boolean isIncluded(Port port) {
        return enabled && port.isEnabled() || disabled && !port.isEnabled() ||
                !enabled && !disabled;
    }

    @Override
    protected void printDevice(DeviceService service, Device device) {
        super.printDevice(service, device);
        List<Port> ports = new ArrayList<>(service.getPorts(device.id()));
        Collections.sort(ports, Comparators.PORT_COMPARATOR);
        for (Port port : ports) {
            if (!isIncluded(port)) {
                continue;
            }
            String portName = portName(port.number());
            Object portIsEnabled = port.isEnabled() ? "enabled" : "disabled";
            String portType = port.type().toString().toLowerCase();
            String annotations = annotations(port.annotations());
            switch (port.type()) {
                case OCH:
                    if (port instanceof org.onosproject.net.OchPort) {
                        // old OchPort model
                        org.onosproject.net.OchPort oPort = (org.onosproject.net.OchPort) port;
                        print("WARN: OchPort in old model");
                        print(FMT_OCH, portName, portIsEnabled, portType,
                              oPort.signalType().toString(),
                              oPort.isTunable() ? "yes" : "no", annotations);
                        break;
                    }
                    if (port instanceof OchPort) {
                        OchPort och = (OchPort) port;
                        print(FMT_OCH, portName, portIsEnabled, portType,
                              och.signalType().toString(),
                              och.isTunable() ? "yes" : "no", annotations);
                       break;
                    } else if (port.element().is(OpticalDevice.class)) {
                        // Note: should never reach here, but
                        // leaving it here as an example to
                        // manually translate to specific port.
                        OpticalDevice optDevice = port.element().as(OpticalDevice.class);
                        if (optDevice.portIs(port, OchPort.class)) {
                            OchPort och = optDevice.portAs(port, OchPort.class).get();
                            print(FMT_OCH, portName, portIsEnabled, portType,
                                  och.signalType().toString(),
                                  och.isTunable() ? "yes" : "no", annotations);
                            break;
                        }
                    }
                    print("WARN: OchPort but not on OpticalDevice or ill-formed");
                    print(FMT, portName, portIsEnabled, portType, port.portSpeed(), annotations);
                    break;
                case ODUCLT:
                    if (port instanceof org.onosproject.net.OduCltPort) {
                        // old OduCltPort model
                        org.onosproject.net.OduCltPort oPort = (org.onosproject.net.OduCltPort) port;
                        print("WARN: OduCltPort in old model");
                        print(FMT_ODUCLT_OTU, portName, portIsEnabled, portType,
                              oPort.signalType().toString(), annotations);
                        break;
                    }
                    if (port instanceof OduCltPort) {
                        print(FMT_ODUCLT_OTU, portName, portIsEnabled, portType,
                              ((OduCltPort) port).signalType().toString(), annotations);
                        break;
                    }
                    print("WARN: OduCltPort but not on OpticalDevice or ill-formed");
                    print(FMT, portName, portIsEnabled, portType, port.portSpeed(), annotations);
                    break;
                case OMS:
                    if (port instanceof org.onosproject.net.OmsPort) {
                        org.onosproject.net.OmsPort oms = (org.onosproject.net.OmsPort) port;
                        print("WARN: OmsPort in old model");
                        print(FMT_OMS, portName, portIsEnabled, portType,
                              oms.minFrequency().asHz() / Frequency.ofGHz(1).asHz(),
                              oms.maxFrequency().asHz() / Frequency.ofGHz(1).asHz(),
                              oms.grid().asHz() / Frequency.ofGHz(1).asHz(),
                              oms.totalChannels(), annotations);
                        break;
                    }
                    if (port instanceof OmsPort) {
                        OmsPort oms = (OmsPort) port;
                        print(FMT_OMS, portName, portIsEnabled, portType,
                              oms.minFrequency().asHz() / Frequency.ofGHz(1).asHz(),
                              oms.maxFrequency().asHz() / Frequency.ofGHz(1).asHz(),
                              oms.grid().asHz() / Frequency.ofGHz(1).asHz(),
                              oms.totalChannels(), annotations);
                        break;
                    }
                    print("WARN: OmsPort but not on OpticalDevice or ill-formed");
                    print(FMT, portName, portIsEnabled, portType, port.portSpeed(), annotations);
                    break;
                case OTU:
                    if (port instanceof org.onosproject.net.OtuPort) {
                        org.onosproject.net.OtuPort otu = (org.onosproject.net.OtuPort) port;
                        print("WARN: OtuPort in old model");
                        print(FMT_ODUCLT_OTU, portName, portIsEnabled, portType,
                              otu.signalType().toString(), annotations);
                        break;
                    }
                    if (port instanceof OtuPort) {
                        print(FMT_ODUCLT_OTU, portName, portIsEnabled, portType,
                              ((OtuPort) port).signalType().toString(), annotations);
                        break;
                    }
                    print("WARN: OtuPort but not on OpticalDevice or ill-formed");
                    print(FMT, portName, portIsEnabled, portType, port.portSpeed(), annotations);
                    break;
                default:
                     print(FMT, portName, portIsEnabled, portType, port.portSpeed(), annotations);
                    break;
            }
        }
    }
}
