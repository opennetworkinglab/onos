/*
 * Copyright 2017-present Open Networking Laboratory
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.openstacknetworking.api.OpenstackSecurityGroupService;
import org.openstack4j.core.transport.ObjectMapperSingleton;
import org.openstack4j.model.network.SecurityGroup;
import org.openstack4j.openstack.networking.domain.NeutronSecurityGroup;

import java.util.Comparator;
import java.util.List;

import static com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT;

/**
 * Lists OpenStack security groups.
 */
@Command(scope = "onos", name = "openstack-security-groups",
        description = "Lists all OpenStack security groups")
public class OpenstackSecurityGroupListCommand extends AbstractShellCommand {

    private static final String FORMAT = "%-40s%-20s";

    @Argument(name = "networkId", description = "Network ID")
    private String networkId = null;

    @Override
    protected void execute() {
        OpenstackSecurityGroupService service =
                AbstractShellCommand.get(OpenstackSecurityGroupService.class);

        List<SecurityGroup> sgs = Lists.newArrayList(service.securityGroups());
        sgs.sort(Comparator.comparing(SecurityGroup::getId));

        if (outputJson()) {
            try {
                print("%s", mapper().writeValueAsString(json(sgs)));
            } catch (JsonProcessingException e) {
                error("Failed to list security groups in JSON format");
            }
            return;
        }

        print("Hint: use --json option to see security group rules as well\n");
        print(FORMAT, "ID", "Name");
        for (SecurityGroup sg: service.securityGroups()) {
            print(FORMAT, sg.getId(), sg.getName());
        }
    }

    private JsonNode json(List<SecurityGroup> securityGroups) {
        ArrayNode result = mapper().enable(INDENT_OUTPUT).createArrayNode();
        for (SecurityGroup sg: securityGroups) {
            result.add(writeSecurityGroup(sg));
        }
        return result;
    }

    private ObjectNode writeSecurityGroup(SecurityGroup sg) {
        try {
            String strSg = ObjectMapperSingleton.getContext(NeutronSecurityGroup.class)
                    .writerFor(NeutronSecurityGroup.class)
                    .writeValueAsString(sg);
            return (ObjectNode) mapper().readTree(strSg.getBytes());
        } catch (Exception e) {
            throw new IllegalArgumentException();
        }
    }
}
