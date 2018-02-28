/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacknetworking.cli;

import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;

import java.util.List;

/**
 * Lists external peer router lists.
 */
@Command(scope = "onos", name = "openstack-peer-routers",
        description = "Lists external peer router lists")
public class ExternalPeerRouterListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-20s%-20s%-20s";

    @Override
    protected void execute() {
        OpenstackNetworkService service = AbstractShellCommand.get(OpenstackNetworkService.class);

        print(FORMAT, "Router IP", "Mac Address", "VLAN ID");
        List<ExternalPeerRouter> routers = Lists.newArrayList(service.externalPeerRouters());

        for (ExternalPeerRouter router: routers) {
            print(FORMAT, router.externalPeerRouterIp(),
                    router.externalPeerRouterMac().toString(),
                    router.externalPeerRouterVlanId());
        }
    }
}
