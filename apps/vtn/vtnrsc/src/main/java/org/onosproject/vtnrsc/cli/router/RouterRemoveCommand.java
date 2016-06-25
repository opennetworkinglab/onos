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

import java.util.Set;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.vtnrsc.Router;
import org.onosproject.vtnrsc.RouterId;
import org.onosproject.vtnrsc.router.RouterService;

import com.google.common.collect.Sets;

/**
 * Supports for remove a router.
 */
@Command(scope = "onos", name = "router-remove", description = "Supports for removing a router")
public class RouterRemoveCommand extends AbstractShellCommand {
    @Option(name = "-i", aliases = "--id", description = "The router identifier",
            required = false, multiValued = false)
    String id = null;

    @Option(name = "-n", aliases = "--routerName", description = "The name of router",
            required = false, multiValued = false)
    String routerName = null;

    @Override
    protected void execute() {
        RouterService service = get(RouterService.class);
        if (id == null && routerName == null) {
            print(null, "one of id, routerName should not be null");
        }
        try {
            Set<RouterId> routerSet = Sets.newHashSet();
            if (id != null) {
                routerSet.add(RouterId.valueOf(id));
                service.removeRouters(routerSet);
            } else {
                Iterable<Router> routers = service.getRouters();
                if (routers == null) {
                    return;
                }
                for (Router router : routers) {
                    if (router.name().equals(routerName)) {
                        routerSet.add(router.id());
                        service.removeRouters(routerSet);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            print(null, e.getMessage());
        }
    }

}
