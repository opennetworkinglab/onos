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
import java.util.Set;
//import java.util.HashSet;
//import java.util.Comparator;
//import java.util.Collections;
import java.util.stream.Collectors;
import java.util.List;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Lists possible output ports to which a given input port can be internally connected on a single device.
 */
@Service
@Command(scope = "onos", name = "get-output-ports",
         description = "Lists possible output ports to which a given input port can be internally connected " +
         "on a single device")
public class GetInternalConnectivityOutputPortsCommand extends AbstractShellCommand {

    private static final Logger log = getLogger(BitErrorCommand.class);

    @Argument(index = 0, name = "input port", description = "{DeviceID}/{PortNumber}",
            required = true, multiValued = false)
    @Completion(ConnectPointCompleter.class)
    private String input = null;

    @Override
    protected void doExecute() throws Exception {
        ConnectPoint inputConnectPoint = ConnectPoint.deviceConnectPoint(input);

        DeviceService deviceService = get(DeviceService.class);
        DeviceId inputDeviceId = inputConnectPoint.deviceId();
        PortNumber inputPortNumber = inputConnectPoint.port();

        InternalConnectivity internalConnectivityBehaviour;

        Device device = deviceService.getDevice(inputDeviceId);

        if (device != null && device.is(InternalConnectivity.class)) {
            internalConnectivityBehaviour = device.as(InternalConnectivity.class);
        } else {
            print("[ERROR] specified device %s does not support Internal Connectivity Behaviour.",
                    device.toString());
            return;
        }

        Set<PortNumber> outputPorts = internalConnectivityBehaviour.getOutputPorts(inputPortNumber);
        List<PortNumber> outputPortsList = outputPorts.stream().sorted((a, b)
                                           -> (int) (a.toLong() - b.toLong())).collect(Collectors.toList());

        if (outputPorts.isEmpty()) {
            print("[POSSIBLE OUTPUT PORTS] None!");
        } else {
            print("[POSSIBLE OUTPUT PORTS] " + outputPortsList.toString());
        }
    }
}
