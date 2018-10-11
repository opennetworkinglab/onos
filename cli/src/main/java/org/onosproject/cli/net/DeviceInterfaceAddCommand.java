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
package org.onosproject.cli.net;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.InterfaceConfig;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.net.driver.DriverService;

import java.util.ArrayList;
import java.util.List;

/**
 * Configures a device interface.
 */
@Service
@Command(scope = "onos", name = "device-add-interface",
         description = "Configures a device interface")
public class DeviceInterfaceAddCommand extends AbstractShellCommand {

    private static final String ONE_ACTION_ALLOWED =
            "One configuration action allowed at a time";
    private static final String CONFIG_VLAN_SUCCESS =
            "VLAN %s added on device %s interface %s.";
    private static final String CONFIG_VLAN_FAILURE =
            "Failed to add VLAN %s on device %s interface %s.";
    private static final String CONFIG_TRUNK_SUCCESS =
            "Trunk mode added for VLAN %s on device %s interface %s.";
    private static final String CONFIG_TRUNK_FAILURE =
            "Failed to add trunk mode for VLAN %s on device %s interface %s.";
    private static final String CONFIG_RATE_SUCCESS =
            "Rate limit %d%% added on device %s interface %s.";
    private static final String CONFIG_RATE_FAILURE =
            "Failed to add rate limit %d%% on device %s interface %s.";

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    private String uri = null;

    @Argument(index = 1, name = "interface",
              description = "Interface name",
              required = true, multiValued = false)
    private String portName = null;

    @Option(name = "-r", aliases = "--rate-limit",
            description = "Percentage for egress bandwidth limit",
            required = false, multiValued = false)
    private String limitString = null;

    @Option(name = "-t", aliases = "--trunk",
            description = "VLAN(s) for trunk port (multiple values are allowed)",
            required = false, multiValued = true)
    private String[] trunkVlanStrings = null;

    @Option(name = "-a", aliases = "--access",
            description = "VLAN for access port",
            required = false, multiValued = false)
    private String accessVlanString = null;

    @Override
    protected void doExecute() {
        DriverService service = get(DriverService.class);
        DeviceId deviceId = DeviceId.deviceId(uri);
        DriverHandler h = service.createHandler(deviceId);
        InterfaceConfig interfaceConfig = h.behaviour(InterfaceConfig.class);

        if (accessVlanString != null && trunkVlanStrings == null &&
                limitString == null) {
            // Access mode to be enabled for VLAN.
            addAccessModeToIntf(interfaceConfig);
        } else if (trunkVlanStrings != null && accessVlanString == null &&
                limitString == null) {
            // Trunk mode to be enabled for VLANs.
            addTrunkModeToIntf(interfaceConfig);
        } else if (limitString != null && accessVlanString == null &&
                trunkVlanStrings == null) {
            // Rate limit to be set on interface.
            addRateLimitToIntf(interfaceConfig);
        } else {
            // Option has not been correctly set.
            print(ONE_ACTION_ALLOWED);
        }
    }

    private void addRateLimitToIntf(InterfaceConfig config) {
        short rate = Short.parseShort(limitString);
        if (config.addRateLimit(portName, rate)) {
            print(CONFIG_RATE_SUCCESS, rate, uri, portName);
        } else {
            print(CONFIG_RATE_FAILURE, rate, uri, portName);
        }
    }

    private void addTrunkModeToIntf(InterfaceConfig config) {
        List<VlanId> vlanIds = new ArrayList<>();
        for (String vlanString : trunkVlanStrings) {
            vlanIds.add(VlanId.vlanId(Short.parseShort(vlanString)));
        }
        if (config.addTrunkMode(portName, vlanIds)) {
            print(CONFIG_TRUNK_SUCCESS, vlanIds, uri, portName);
        } else {
            print(CONFIG_TRUNK_FAILURE, vlanIds, uri, portName);
        }
    }

    private void addAccessModeToIntf(InterfaceConfig config) {
        VlanId accessVlanId = VlanId.vlanId(Short.parseShort(accessVlanString));
        if (config.addAccessMode(portName, accessVlanId)) {
            print(CONFIG_VLAN_SUCCESS, accessVlanId, uri, portName);
        } else {
            print(CONFIG_VLAN_FAILURE, accessVlanId, uri, portName);
        }
    }

}
