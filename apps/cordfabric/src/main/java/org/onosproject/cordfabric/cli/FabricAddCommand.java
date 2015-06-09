/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.cordfabric.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.VlanId;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cordfabric.FabricService;
import org.onosproject.cordfabric.FabricVlan;
import org.onosproject.net.ConnectPoint;

import java.util.ArrayList;
import java.util.List;

/**
 * Adds a vlan to the fabric.
 */
@Command(scope = "onos", name = "add-fabric-vlan",
        description = "Adds a VLAN to the fabric")
public class FabricAddCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "vlanid", description = "VLAN ID",
            required = true, multiValued = false)
    private String vlanIdString = null;

    @Argument(index = 1, name = "ports",
            description = "List of ports in the VLAN",
            required = true, multiValued = true)
    private String[] portStrings = null;

    @Override
    protected void execute() {
        FabricService service = AbstractShellCommand.get(FabricService.class);

        VlanId vlan = VlanId.vlanId(Short.parseShort(vlanIdString));

        if (portStrings.length < 2) {
            throw new IllegalArgumentException("Must have at least 2 ports");
        }

        List<ConnectPoint> ports = new ArrayList<>(portStrings.length);

        for (String portString : portStrings) {
            ports.add(ConnectPoint.deviceConnectPoint(portString));
        }

        service.addVlan(new FabricVlan(vlan, ports, false));
    }
}
