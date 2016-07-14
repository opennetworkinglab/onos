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
 * Supports for query a router interface.
 */
@Command(scope = "onos", name = "routerinterfaces", description = "Supports for querying a router interface")
public class RouterInterfaceQueryCommand extends AbstractShellCommand {
    @Option(name = "-s", aliases = "--subnetId", description = "The subnet identifier of router interface",
            required = false, multiValued = false)
    String subnetId = null;

    private static final String FMT = "subnetId=%s, tenantId=%s, portId=%s, routerId=%s";

    @Override
    protected void execute() {
        RouterInterfaceService service = get(RouterInterfaceService.class);
        if (subnetId != null) {
            RouterInterface routerInterface = service
                    .getRouterInterface(SubnetId.subnetId(subnetId));
            printRouterInterface(routerInterface);
        } else {
            Iterable<RouterInterface> routerInterfaces = service
                    .getRouterInterfaces();
            for (RouterInterface routerInterface : routerInterfaces) {
                printRouterInterface(routerInterface);
            }
        }
    }

    private void printRouterInterface(RouterInterface routerInterface) {
        print(FMT, routerInterface.subnetId(), routerInterface.tenantId(),
              routerInterface.portId(), routerInterface.routerId());
    }
}
