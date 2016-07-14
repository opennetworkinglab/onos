/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.vtnrsc.cli.routerinterface;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.vtnrsc.RouterInterface;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.routerinterface.RouterInterfaceService;

/**
 * Supports for remove a router interface.
 */
@Command(scope = "onos", name = "routerinterface-remove", description = "Supports for removing a router interface")
public class RouterInterfaceRemoveCommand extends AbstractShellCommand {
    @Option(name = "-s", aliases = "--subnetId", description = "The subnet identifier of router interface",
            required = true, multiValued = false)
    String subnetId = null;

    @Override
    protected void execute() {
        RouterInterfaceService service = get(RouterInterfaceService.class);
        try {
            RouterInterface routerInterface = service
                    .getRouterInterface(SubnetId.subnetId(subnetId));
            if (routerInterface == null) {
                print(null, "subnet ID of interface doesn't exist");
                return;
            }
            service.removeRouterInterface(routerInterface);
        } catch (Exception e) {
            print(null, e.getMessage());
        }

    }
}
