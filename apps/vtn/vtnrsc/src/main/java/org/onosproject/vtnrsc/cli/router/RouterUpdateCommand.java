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
package org.onosproject.vtnrsc.cli.router;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.vtnrsc.DefaultRouter;
import org.onosproject.vtnrsc.Router;
import org.onosproject.vtnrsc.Router.Status;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.TenantId;
import org.onosproject.vtnrsc.VirtualPortId;
import org.onosproject.vtnrsc.router.RouterService;

import com.google.common.collect.Sets;

/**
 * Supports for update a router.
 */
@Command(scope = "onos", name = "router-update", description = "Supports for updating a router")
public class RouterUpdateCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "id", description = "The router identifier",
            required = true, multiValued = false)
    String id = null;

    @Option(name = "-r", aliases = "--routerName", description = "The name of router",
            required = false, multiValued = false)
    String routerName = null;

    @Option(name = "-t", aliases = "--tenantId", description = "The tenant identifier of router",
            required = false, multiValued = false)
    String tenantId = null;

    @Option(name = "-g", aliases = "--gatewayPortId", description = "The gatewayPort identifier of router",
            required = false, multiValued = false)
    String gatewayPortId = null;

    @Option(name = "-e", aliases = "--externalGatewayInfo", description = "The externalGatewayInfo of router",
            required = false, multiValued = false)
    String externalGatewayInfo = null;

    @Option(name = "-s", aliases = "--status", description = "The status of router",
            required = false, multiValued = false)
    String status = null;

    @Option(name = "-a", aliases = "--adminStateUp", description = "The boolean adminStateUp of router",
            required = false, multiValued = false)
    boolean adminStateUp = true;

    @Option(name = "-d", aliases = "--distributed", description = "The boolean distributed of router",
            required = false, multiValued = false)
    boolean distributed = false;

    @Override
    protected void execute() {
        RouterService service = get(RouterService.class);
        RouterId routerId = RouterId.valueOf(id);
        Router router = get(RouterService.class).getRouter(routerId);
        try {
            List<String> routes = new ArrayList<String>();
            Router routerObj = new DefaultRouter(
                                                 RouterId.valueOf(id),
                                                 routerName == null ? router.name() : routerName,
                                                 adminStateUp,
                                                 status == null ? Status.ACTIVE
                                                               : Status.valueOf(status),
                                                 distributed,
                                                 null,
                                                 gatewayPortId == null ? router.gatewayPortid()
                                                                      : VirtualPortId.portId(gatewayPortId),
                                                 tenantId == null ? router.tenantId()
                                                                  : TenantId.tenantId(tenantId),
                                                 routes);
            Set<Router> routerSet = Sets.newHashSet(routerObj);
            service.createRouters(routerSet);
        } catch (Exception e) {
            print(null, e.getMessage());
        }
    }
}
