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

package org.onosproject.pim.cli;

import org.apache.karaf.shell.commands.Command;
import org.onlab.util.Tools;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.pim.impl.PimInterface;
import org.onosproject.pim.impl.PimInterfaceService;
import org.onosproject.pim.impl.PimNeighbor;

import java.util.Set;

/**
 * Lists PIM neighbors.
 */
@Command(scope = "onos", name = "pim-neighbors",
        description = "Lists the PIM neighbors")
public class PimNeighborsListCommand extends AbstractShellCommand {

    private static final String INTF_FORMAT = "interface=%s, address=%s";
    private static final String NEIGHBOR_FORMAT = "  neighbor=%s, uptime=%s, holdtime=%s, drPriority=%s, genId=%s";

    @Override
    protected void execute() {
        PimInterfaceService interfaceService = get(PimInterfaceService.class);

        Set<PimInterface> interfaces = interfaceService.getPimInterfaces();

        for (PimInterface intf : interfaces) {
            print(INTF_FORMAT, intf.getInterface().name(), intf.getIpAddress());
            for (PimNeighbor neighbor : intf.getNeighbors()) {
                // Filter out the PIM neighbor representing 'us'
                if (!neighbor.ipAddress().equals(intf.getIpAddress())) {
                    print(NEIGHBOR_FORMAT, neighbor.ipAddress(),
                            Tools.timeAgo(neighbor.upTime()), neighbor.holdtime(),
                            neighbor.priority(), neighbor.generationId());
                }
            }
        }
    }

}
