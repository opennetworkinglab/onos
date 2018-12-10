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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.ExternalPeerRouter;
import org.onosproject.openstacknetworking.api.OpenstackNetworkService;

import java.util.List;

import static org.onosproject.cli.AbstractShellCommand.get;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.prettyJson;

/**
 * Lists external peer router lists.
 */
@Service
@Command(scope = "onos", name = "openstack-peer-routers",
        description = "Lists external peer router lists")
public class ExternalPeerRouterListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-20s%-20s%-20s";

    @Override
    protected void doExecute() {
        OpenstackNetworkService service = get(OpenstackNetworkService.class);
        List<ExternalPeerRouter> routers =
                            Lists.newArrayList(service.externalPeerRouters());

        if (outputJson()) {
            print("%s", json(this, routers));
        } else {
            print(FORMAT, "Router IP", "Mac Address", "VLAN ID");
            for (ExternalPeerRouter router: routers) {
                print(FORMAT, router.ipAddress(),
                        router.macAddress().toString(),
                        router.vlanId());
            }
        }
    }

    private String json(AbstractShellCommand context, List<ExternalPeerRouter> routers) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        routers.forEach(r -> result.add(context.jsonForEntity(r, ExternalPeerRouter.class)));

        return prettyJson(mapper, result.toString());
    }
}
