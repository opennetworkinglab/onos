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
 */

package org.onosproject.segmentrouting.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.PlaceholderCompleter;
import org.onosproject.cli.net.DeviceIdCompleter;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.segmentrouting.phasedrecovery.api.PhasedRecoveryService;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Command(scope = "onos", name = "sr-ports", description = "Enable/Disable group of ports on a specific device")

public class PortsCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "deviceId",
            description = "Device ID",
            required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    private String deviceIdStr;

    @Argument(index = 1, name = "ports",
            description = "Ports to be enabled/disabled: ALL, PAIR, INFRA, EDGE",
            required = true, multiValued = false)
    @Completion(PlaceholderCompleter.class)
    private String portsStr;

    @Argument(index = 2, name = "action",
            description = "Action: ENABLE, DISABLE",
            required = true, multiValued = false)
    @Completion(PlaceholderCompleter.class)
    private String actionStr;

    @Override
    protected void doExecute() {
        PhasedRecoveryService prService = get(PhasedRecoveryService.class);

        DeviceId deviceId = DeviceId.deviceId(deviceIdStr);

        boolean enabled;
        switch (actionStr.toUpperCase()) {
            case "ENABLE":
                enabled = true;
                break;
            case "DISABLE":
                enabled = false;
                break;
            default:
                print("Action should be either ENABLE or DISABLE");
                return;
        }

        Set<PortNumber> portsChanged;
        switch (portsStr.toUpperCase()) {
            case "ALL":
                portsChanged = prService.changeAllPorts(deviceId, enabled);
                break;
            case "PAIR":
                portsChanged = prService.changePairPort(deviceId, enabled);
                break;
            case "INFRA":
                portsChanged = prService.changeInfraPorts(deviceId, enabled);
                break;
            case "EDGE":
                portsChanged = prService.changeEdgePorts(deviceId, enabled);
                break;
            default:
                print("Ports should be ALL, PAIR, INFRA, EDGE");
                return;
        }
        print("Ports set to %s: %s",
                enabled ? "enabled" : "disabled",
                portsChanged.stream().map(PortNumber::toLong).collect(Collectors.toSet()));
    }
}
