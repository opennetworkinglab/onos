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
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.openstack4j.model.network.RouterInterface;
import org.openstack4j.openstack.networking.domain.NeutronRouter;

import java.util.List;

import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.modelEntityToJson;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.prettyJson;

/**
 * Lists Openstack router interfaces.
 */
@Service
@Command(scope = "onos", name = "openstack-router-interfaces",
        description = "Lists all OpenStack routers interfaces")
public class OpenstackRouterInterfaceListCommand extends AbstractShellCommand {
    private static final String FORMAT = "%-40s%-40s%-40s%-40s%-40s";

    private final OpenstackRouterService routerService =
            AbstractShellCommand.get(OpenstackRouterService.class);

    @Override
    protected void doExecute() {
        List<RouterInterface> routerInterfaces =
                Lists.newArrayList(routerService.routerInterfaces());

        if (outputJson()) {
            print("%s", json(routerInterfaces));
        } else {
            print(FORMAT, "ID", "RouterName", "SubnetID", "PortID", "TenantID");
            for (RouterInterface routerInterface: routerInterfaces) {
                print(FORMAT, routerInterface.getId(),
                        routerService.router(routerInterface.getId()),
                        routerInterface.getSubnetId(),
                        routerInterface.getPortId(),
                        routerInterface.getTenantId());
            }
        }
    }

    private String json(List<RouterInterface> routerInterfaces) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (RouterInterface routerInterface: routerInterfaces) {
            result.add(modelEntityToJson(routerInterface, NeutronRouter.class));
        }
        return prettyJson(mapper, result.toString());
    }
}