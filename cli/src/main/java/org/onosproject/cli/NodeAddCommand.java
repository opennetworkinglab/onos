/*
 * Copyright 2014-present Open Networking Laboratory
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
import org.onosproject.cluster.ClusterAdminService;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onlab.packet.IpAddress;

/**
 * Adds a new controller cluster node.
 */
@Command(scope = "onos", name = "add-node",
         description = "Adds a new controller cluster node")
public class NodeAddCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "nodeId", description = "Node ID",
              required = true, multiValued = false)
    String nodeId = null;

    @Argument(index = 1, name = "ip", description = "Node IP address",
              required = true, multiValued = false)
    String ip = null;

    @Argument(index = 2, name = "tcpPort", description = "Node TCP listen port",
              required = false, multiValued = false)
    int tcpPort = DefaultControllerNode.DEFAULT_PORT;

    @Override
    protected void execute() {
        ClusterAdminService service = get(ClusterAdminService.class);
        service.addNode(new NodeId(nodeId), IpAddress.valueOf(ip), tcpPort);
    }

}
