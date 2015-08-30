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
package org.onosproject.vtnrsc.cli.virtualport;

import java.util.Set;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.virtualport.VirtualPortService;

import com.google.common.collect.Sets;

/**
 * Supports for removing a virtualPort.
 */
@Command(scope = "onos", name = "virtualport-remove",
        description = "Supports for removing a virtualPort.")
public class VirtualPortRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "id", description = "virtualPort id.", required = true,
            multiValued = false)
    String id = null;

    @Override
    protected void execute() {
        VirtualPortService service = get(VirtualPortService.class);
        Set<VirtualPortId> virtualPorts = Sets.newHashSet(VirtualPortId.portId(id));
        service.removePorts(virtualPorts);
    }
}
