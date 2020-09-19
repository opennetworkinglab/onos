/*
 * Copyright 2020-present Open Networking Foundation
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

 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */

package org.onosproject.cli.net;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.behaviour.InternalConnectivity;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

@Service
@Command(scope = "onos", name = "check-internal-connectivity",
        description = "Check if two port of a device can be connected")
public class InternalConnectivityCommand extends AbstractShellCommand {

    private static final Logger log = getLogger(BitErrorCommand.class);

    @Argument(index = 0, name = "input port", description = "{DeviceID}/{PortNumber}",
            required = true, multiValued = false)
    @Completion(ConnectPointCompleter.class)
    private String input = null;

    @Argument(index = 1, name = "output port", description = "{DeviceID}/{PortNumber}",
            required = true, multiValued = false)
    @Completion(ConnectPointCompleter.class)
    private String output = null;

    @Override
    protected void doExecute() throws Exception {
        ConnectPoint inputConnectPoint = ConnectPoint.deviceConnectPoint(input);
        ConnectPoint outputConnectPoint = ConnectPoint.deviceConnectPoint(output);

        DeviceService deviceService = get(DeviceService.class);
        DeviceId inputDeviceId = inputConnectPoint.deviceId();
        DeviceId outputDeviceId = outputConnectPoint.deviceId();
        PortNumber inputPortNumber = inputConnectPoint.port();
        PortNumber outputPortNumber = outputConnectPoint.port();

        InternalConnectivity internalConnectivityBehaviour;

        if (!inputDeviceId.equals(outputDeviceId)) {
            print("[ERROR] specified connect points should belong to the same device.");
            return;
        }

        Device device = deviceService.getDevice(inputDeviceId);

        if (device != null && device.is(InternalConnectivity.class)) {
            internalConnectivityBehaviour = device.as(InternalConnectivity.class);
        } else {
            print("[ERROR] specified device %s does not support Internal Connectivity Behaviour.",
                    device.toString());
            return;
        }

        if (internalConnectivityBehaviour.testConnectivity(inputPortNumber, outputPortNumber)) {
            print("[CONNECTIVITY ALLOWED] device %s from input-port %s to output-port %s",
                    device.id().toString(),
                    inputPortNumber.toString(),
                    outputPortNumber.toString());
        } else {
            print("[CONNECTIVITY NOT ALLOWED] device %s from input-port %s to output-port %s",
                    device.id().toString(),
                    inputPortNumber.toString(),
                    outputPortNumber.toString());
        }
    }
}