/*
 * Copyright 2016-present Open Networking Laboratory
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
import org.apache.karaf.shell.commands.Option;
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
@Command(scope = "onos", name = "device-add-interface",
         description = "Configures a device interface")
public class DeviceInterfaceAddCommand extends AbstractShellCommand {

    private static final String CONFIG_VLAN_SUCCESS =
            "VLAN %s added on device %s interface %s.";
    private static final String CONFIG_VLAN_FAILURE =
            "Failed to add VLAN %s on device %s interface %s.";
    private static final String ONE_VLAN_ALLOWED =
            "Only one VLAN allowed for access mode on device %s interface %s.";

    private static final String CONFIG_TRUNK_SUCCESS =
            "Trunk mode added for VLAN %s on device %s interface %s.";
    private static final String CONFIG_TRUNK_FAILURE =
            "Failed to add trunk mode for VLAN %s on device %s interface %s.";

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = true, multiValued = false)
    private String uri = null;

    @Argument(index = 1, name = "interface",
              description = "Interface name",
              required = true, multiValued = false)
    private String portName = null;

    @Argument(index = 2, name = "vlan",
            description = "VLAN ID",
            required = true, multiValued = true)
    private String[] vlanStrings = null;

    @Option(name = "-t", aliases = "--trunk",
            description = "Configure interface as trunk for VLAN(s)",
            required = false, multiValued = false)
    private boolean trunkMode = false;

    @Override
    protected void execute() {
        DriverService service = get(DriverService.class);
        DeviceId deviceId = DeviceId.deviceId(uri);
        DriverHandler h = service.createHandler(deviceId);
        InterfaceConfig interfaceConfig = h.behaviour(InterfaceConfig.class);

        List<VlanId> vlanIds = new ArrayList<>();
        for (String vlanString : vlanStrings) {
            vlanIds.add(VlanId.vlanId(Short.parseShort(vlanString)));
        }

        if (trunkMode) {
            // Trunk mode to be enabled for VLAN.
            if (interfaceConfig.addTrunkInterface(deviceId, portName, vlanIds)) {
                print(CONFIG_TRUNK_SUCCESS, vlanIds, deviceId, portName);
            } else {
                print(CONFIG_TRUNK_FAILURE, vlanIds, deviceId, portName);
            }
            return;
        }

        // Access mode to be enabled for VLAN.
        if (vlanIds.size() != 1) {
            print(ONE_VLAN_ALLOWED, deviceId, portName);
            return;
        }
        VlanId accessVlanId = vlanIds.get(0);
        if (interfaceConfig.addAccessInterface(deviceId, portName, accessVlanId)) {
            print(CONFIG_VLAN_SUCCESS, accessVlanId, deviceId, portName);
        } else {
            print(CONFIG_VLAN_FAILURE, accessVlanId, deviceId, portName);
        }
    }

}
