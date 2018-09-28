/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.net.optical.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.util.Frequency;
import org.onosproject.cli.net.DevicePortsListCommand;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.Port.Type;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.optical.OchPort;
import org.onosproject.net.optical.OduCltPort;
import org.onosproject.net.optical.OmsPort;
import org.onosproject.net.optical.OtuPort;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.optical.device.OpticalDeviceServiceView.opticalView;

/**
 * Lists all ports or all ports of a device.
 */
@Service
@Command(scope = "onos", name = "optical-ports",
         description = "Lists all optical ports or all optical ports of a device")
public class OpticalPortsListCommand extends DevicePortsListCommand {

    private static final String FMT = "  port=%s, state=%s, type=%s, speed=%s %s";
    private static final String FMT_OCH = "  port=%s, state=%s, type=%s, signalType=%s, isTunable=%s %s";
    private static final String FMT_ODUCLT_OTU = "  port=%s, state=%s, type=%s, signalType=%s %s";
    private static final String FMT_OMS = "  port=%s, state=%s, type=%s, freqs=%s / %s / %s GHz, totalChannels=%s %s";

    private static final EnumSet<Port.Type> OPTICAL = EnumSet.of(Type.OCH, Type.ODUCLT, Type.OMS, Type.OTU);

    @Override
    protected void doExecute() {
        DeviceService service = opticalView(get(DeviceService.class));
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

    // Determines if a port should be included in output.
    @Override
    protected boolean isIncluded(Port port) {
        return OPTICAL.contains(port.type()) &&
               super.isIncluded(port);
    }

    @Override
    protected void printPorts(DeviceService service, Device device) {
        List<Port> ports = new ArrayList<>(service.getPorts(device.id()));
        ports.sort((p1, p2) ->
             Long.signum(p1.number().toLong() - p2.number().toLong())
        );
        for (Port port : ports) {
            if (!isIncluded(port)) {
                continue;
            }
            String portName = port.number().toString();
            String portIsEnabled = port.isEnabled() ? "enabled" : "disabled";
            String portType = port.type().toString().toLowerCase();
            switch (port.type()) {
                case OCH:
                    if (port instanceof OchPort) {
                        OchPort och = (OchPort) port;
                        print(FMT_OCH, portName, portIsEnabled, portType,
                              och.signalType().toString(),
                              och.isTunable() ? "yes" : "no",
                              annotations(och.unhandledAnnotations()));
                       break;
                    }
                    print("WARN: OchPort but not on OpticalDevice or ill-formed");
                    print(FMT, portName, portIsEnabled, portType, port.portSpeed(), annotations(port.annotations()));
                    break;
                case ODUCLT:
                    if (port instanceof OduCltPort) {
                        OduCltPort oduCltPort = (OduCltPort) port;
                        print(FMT_ODUCLT_OTU, portName, portIsEnabled, portType,
                              oduCltPort.signalType().toString(),
                              annotations(oduCltPort.unhandledAnnotations()));
                        break;
                    }
                    print("WARN: OduCltPort but not on OpticalDevice or ill-formed");
                    print(FMT, portName, portIsEnabled, portType, port.portSpeed(), annotations(port.annotations()));
                    break;
                case OMS:
                    if (port instanceof OmsPort) {
                        OmsPort oms = (OmsPort) port;
                        print(FMT_OMS, portName, portIsEnabled, portType,
                              oms.minFrequency().asHz() / Frequency.ofGHz(1).asHz(),
                              oms.maxFrequency().asHz() / Frequency.ofGHz(1).asHz(),
                              oms.grid().asHz() / Frequency.ofGHz(1).asHz(),
                              oms.totalChannels(),
                              annotations(oms.unhandledAnnotations()));
                        break;
                    }
                    print("WARN: OmsPort but not on OpticalDevice or ill-formed");
                    print(FMT, portName, portIsEnabled, portType, port.portSpeed(), annotations(port.annotations()));
                    break;
                case OTU:
                    if (port instanceof OtuPort) {
                        OtuPort otuPort = (OtuPort) port;
                        print(FMT_ODUCLT_OTU, portName, portIsEnabled, portType,
                              otuPort.signalType().toString(),
                              annotations(otuPort.unhandledAnnotations()));
                        break;
                    }
                    print("WARN: OtuPort but not on OpticalDevice or ill-formed");
                    print(FMT, portName, portIsEnabled, portType, port.portSpeed(), annotations(port.annotations()));
                    break;
                default:
                    // do not print non-optical ports
                    break;
            }
        }
    }
}
