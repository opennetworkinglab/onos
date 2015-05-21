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

/**
 * Removes a vlan from the fabric.
 */
@Command(scope = "onos", name = "remove-fabric-vlan",
        description = "Removes a VLAN from the fabric")
public class FabricRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "vlanid", description = "VLAN ID",
            required = true, multiValued = false)
    private String vlanIdString = null;

    @Override
    protected void execute() {
        FabricService service = AbstractShellCommand.get(FabricService.class);

        VlanId vlan = VlanId.vlanId(Short.parseShort(vlanIdString));

        service.removeVlan(vlan);
    }
}
