/*
 * Copyright 2014 Open Networking Laboratory
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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.service.DatabaseAdminService;

/**
 * Removes a controller cluster node.
 */
@Command(scope = "onos", name = "tablet-remove",
         description = "Removes a member from tablet")
public class TabletRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "nodeId", description = "Node ID",
              required = true, multiValued = false)
    String nodeId = null;

    // TODO add tablet name argument when we support multiple tablets

    @Override
    protected void execute() {
        DatabaseAdminService service = get(DatabaseAdminService.class);
        ClusterService clusterService = get(ClusterService.class);
        ControllerNode node = clusterService.getNode(new NodeId(nodeId));
        if (node != null) {
            service.removeMember(node);
        }
    }
}
