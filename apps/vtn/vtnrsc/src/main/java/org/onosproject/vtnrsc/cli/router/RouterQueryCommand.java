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

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.vtnrsc.Router;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.router.RouterService;

/**
 * Supports for query a list of router.
 */
@Command(scope = "onos", name = "routers", description = "Supports for creating a router")
public class RouterQueryCommand extends AbstractShellCommand {
    @Option(name = "-i", aliases = "--id", description = "The router identifier",
            required = false, multiValued = false)
    String id = null;

    @Option(name = "-n", aliases = "--routerName", description = "The name of router",
            required = false, multiValued = false)
    String routerName = null;

    private static final String FMT = "routerId=%s, routerName=%s, tenantId=%s, gatewayPortId=%s,"
            + "externalGatewayInfo=%s, status=%s, adminStateUp=%s, distributed=%s, routers=%s";

    @Override
    protected void execute() {
        RouterService service = get(RouterService.class);
        if (id != null) {
            Router router = service.getRouter(RouterId.valueOf(id));
            printFloatingIp(router);
        } else if (routerName != null) {
            Iterable<Router> routers = service.getRouters();
            if (routers == null) {
                return;
            }
            for (Router router : routers) {
                if (router.name().equals(routerName)) {
                    printFloatingIp(router);
                    return;
                }
            }
            print(null, "The routerName is not existed");
        } else {
            Iterable<Router> routers = service.getRouters();
            if (routers == null) {
                return;
            }
            for (Router router : routers) {
                printFloatingIp(router);
            }
        }
    }

    private void printFloatingIp(Router router) {
        print(FMT, router.id(), router.name(), router.tenantId(),
              router.gatewayPortid(), router.externalGatewayInfo(),
              router.status(), router.adminStateUp(), router.distributed(),
              router.routes());
    }
}
