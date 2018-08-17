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

package org.onosproject.cli;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cluster.ClusterAdminService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.Member;
import org.onosproject.cluster.MembershipService;

import java.util.List;
import java.util.Optional;

import static com.google.common.collect.Lists.newArrayList;
import static org.onosproject.utils.Comparators.MEMBERSHIP_COMPARATOR;

/**
 * Command to list the memberships in the system.
 */
@Service
@Command(scope = "onos", name = "memberships",
        description = "Lists information about memberships in the system")
public class MembershipsListCommand extends AbstractShellCommand {

    @Override
    protected void doExecute() {
        MembershipService service = get(MembershipService.class);
        ClusterAdminService clusterService = get(ClusterAdminService.class);

        if (outputJson()) {
            print("%s", json(service));
        } else {
            service.getGroups().forEach(group -> {
                List<Member> members = newArrayList(group.members());
                print("-------------------------------------------------------------------");
                print("Version: %s, Members: %d", group.version(), members.size());
                members.sort(MEMBERSHIP_COMPARATOR);
                members.forEach(
                        member -> {
                            Optional<ControllerNode> controllerNode =
                                    Optional.ofNullable(clusterService.getNode(member.nodeId()));

                            if (!controllerNode.isPresent()) {
                                print(" id=%s, version=%s, self=%s",
                                        member.nodeId(),
                                        member.version(),
                                        member.equals(service.getLocalMember()) ? "*" : "");
                            } else {
                                ControllerNode node = controllerNode.get();
                                print(" id=%s, ip=%s, tcpPort=%s, state=%s, self=%s",
                                        member.nodeId(),
                                        node.ip(),
                                        node.tcpPort(),
                                        clusterService.getState(node.id()),
                                        member.equals(service.getLocalMember()) ? "*" : "");
                            }
                        }
                );
                print("-------------------------------------------------------------------");
            });
        }
    }

    /**
     * Produces JSON structure.
     *
     * @param service membership service
     * @return json structure
     */
    private JsonNode json(MembershipService service) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        service.getGroups().forEach(group -> {
            ObjectNode groupNode = mapper.createObjectNode();

            ArrayNode membersNode = mapper.createArrayNode();
            groupNode.put("version", group.version().toString());
            groupNode.put("members", membersNode);

            group.members().forEach(member -> membersNode.add(member.nodeId().toString()));

            result.add(groupNode);
        });

        return result;
    }
}
