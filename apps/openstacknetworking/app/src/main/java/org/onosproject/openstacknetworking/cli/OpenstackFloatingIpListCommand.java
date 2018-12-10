/*
 * Copyright 2017-present Open Networking Foundation
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
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.OpenstackRouterService;
import org.openstack4j.model.network.NetFloatingIP;
import org.openstack4j.openstack.networking.domain.NeutronFloatingIP;

import java.util.Comparator;
import java.util.List;

import static org.onosproject.cli.AbstractShellCommand.get;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.modelEntityToJson;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.prettyJson;

/**
 * Lists OpenStack floating IP addresses.
 */
@Service
@Command(scope = "onos", name = "openstack-floatingips",
        description = "Lists all OpenStack floating IP addresses")
public class OpenstackFloatingIpListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-40s%-20s%-20s";

    @Override
    protected void doExecute() {
        OpenstackRouterService service = get(OpenstackRouterService.class);
        List<NetFloatingIP> floatingIps = Lists.newArrayList(service.floatingIps());
        floatingIps.sort(Comparator.comparing(NetFloatingIP::getFloatingIpAddress));

        if (outputJson()) {
            print("%s", json(floatingIps));
        } else {
            print(FORMAT, "ID", "Floating IP", "Fixed IP");
            for (NetFloatingIP floatingIp: floatingIps) {
                print(FORMAT, floatingIp.getId(),
                        floatingIp.getFloatingIpAddress(),
                        Strings.isNullOrEmpty(floatingIp.getFixedIpAddress()) ?
                                "" : floatingIp.getFixedIpAddress());
            }
        }
    }

    private String json(List<NetFloatingIP> floatingIps) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (NetFloatingIP floatingIp: floatingIps) {
            result.add(modelEntityToJson(floatingIp, NeutronFloatingIP.class));
        }
        return prettyJson(mapper, result.toString());
    }
}
