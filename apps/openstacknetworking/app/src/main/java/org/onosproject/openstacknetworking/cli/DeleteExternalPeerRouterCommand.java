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
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;
import org.onosproject.openstacknetworking.api.OpenstackNetworkAdminService;

import java.util.List;

import static org.onosproject.cli.AbstractShellCommand.get;

/**
 * Deletes external peer router.
 */
@Service
@Command(scope = "onos", name = "openstack-delete-peer-router",
        description = "Delete external peer router")
public class DeleteExternalPeerRouterCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "ip address", description = "ip address",
            required = true, multiValued = false)
    @Completion(IpAddressCompleter.class)
    private String ipAddress = null;

    private static final String FORMAT = "%-20s%-20s%-20s";
    private static final String NO_ELEMENT =
            "There's no external peer router information with given ip address";

    @Override
    protected void doExecute() {
        OpenstackNetworkAdminService service = get(OpenstackNetworkAdminService.class);

        if (service.externalPeerRouters().stream()
                .noneMatch(router -> router.ipAddress().toString().equals(ipAddress))) {
            print(NO_ELEMENT);
            return;
        }

        try {
            service.deleteExternalPeerRouter(ipAddress);
        } catch (IllegalArgumentException e) {
            log.error("Exception occurred because of {}", e);
        }
        print(FORMAT, "Router IP", "Mac Address", "VLAN ID");
        List<ExternalPeerRouter> routers = Lists.newArrayList(service.externalPeerRouters());

        for (ExternalPeerRouter router: routers) {
            print(FORMAT, router.ipAddress(),
                    router.macAddress().toString(),
                    router.vlanId());
        }

    }
}
