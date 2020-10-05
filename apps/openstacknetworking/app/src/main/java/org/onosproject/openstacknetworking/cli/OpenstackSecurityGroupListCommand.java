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
import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupService;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.openstack.networking.domain.NeutronSecurityGroup;

import java.util.Comparator;
import java.util.List;

import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.deriveResourceName;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.modelEntityToJson;
import static org.onosproject.openstacknetworking.util.OpenstackNetworkingUtil.prettyJson;

/**
 * Lists OpenStack security groups.
 */
@Service
@Command(scope = "onos", name = "openstack-security-groups",
        description = "Lists all OpenStack security groups")
public class OpenstackSecurityGroupListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-40s%-20s";

    @Argument(name = "networkId", description = "Network ID")
    private String networkId = null;

    @Override
    protected void doExecute() {
        OpenstackSecurityGroupService service = get(OpenstackSecurityGroupService.class);

        List<SecurityGroup> sgs = Lists.newArrayList(service.securityGroups());
        sgs.sort(Comparator.comparing(SecurityGroup::getId));

        if (outputJson()) {
            print("%s", json(sgs));
        } else {
            print("Hint: use --json option to see security group rules as well\n");
            print(FORMAT, "ID", "Name");
            for (SecurityGroup sg: service.securityGroups()) {
                print(FORMAT, sg.getId(), deriveResourceName(sg));
            }
        }
    }

    private String json(List<SecurityGroup> securityGroups) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (SecurityGroup sg: securityGroups) {
            result.add(modelEntityToJson(sg, NeutronSecurityGroup.class));
        }
        return prettyJson(mapper, result.toString());
    }
}
