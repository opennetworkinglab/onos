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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.RouterInterface;
import org.onosproject.vtnrsc.SubnetId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.routerinterface.RouterInterfaceService;

/**
 * Supports for create a router interface.
 */
@Command(scope = "onos", name = "routerinterface-create", description = "Supports for creating a router interface")
public class RouterInterfaceCreateCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "routerId", description = "The router identifier of router interface",
            required = true, multiValued = false)
    String routerId = null;

    @Argument(index = 1, name = "tenantId", description = "The tenant identifier of router interface",
            required = true, multiValued = false)
    String tenantId = null;

    @Argument(index = 2, name = "portId", description = "The port identifier of router interface",
            required = true, multiValued = false)
    String portId = null;

    @Argument(index = 3, name = "subnetId", description = "The subnet identifier of router interface",
            required = true, multiValued = false)
    String subnetId = null;

    @Override
    protected void execute() {
        RouterInterfaceService service = get(RouterInterfaceService.class);
        try {
            RouterInterface routerInterface = RouterInterface.routerInterface(
                                                                  SubnetId.subnetId(subnetId),
                                                                  VirtualPortId.portId(portId),
                                                                  RouterId.valueOf(routerId),
                                                                  TenantId.tenantId(tenantId));
            service.addRouterInterface(routerInterface);
        } catch (Exception e) {
            print(null, e.getMessage());
        }
    }

}
