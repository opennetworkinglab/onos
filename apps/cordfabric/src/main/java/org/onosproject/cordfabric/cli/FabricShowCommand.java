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

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cordfabric.FabricService;
import org.onosproject.cordfabric.FabricVlan;

import java.util.List;

/**
 * Shows the vlans in the fabric.
 */
@Command(scope = "onos", name = "fabric",
        description = "Shows the fabric vlans")
public class FabricShowCommand extends AbstractShellCommand {

    private static final String VLAN_HEADER_LINE_FORMAT = "VLAN %s";
    private static final String PORT_LINE_FORMAT = "\t%s";

    @Override
    protected void execute() {
        FabricService service = AbstractShellCommand.get(FabricService.class);

        List<FabricVlan> vlans = service.getVlans();

        vlans.forEach(fabricVlan -> {
            print(VLAN_HEADER_LINE_FORMAT, fabricVlan.vlan());
            fabricVlan.ports().forEach(cp -> print(PORT_LINE_FORMAT, cp));
        });
    }
}
