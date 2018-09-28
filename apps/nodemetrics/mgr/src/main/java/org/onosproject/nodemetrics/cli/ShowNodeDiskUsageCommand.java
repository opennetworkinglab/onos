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

package org.onosproject.nodemetrics.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.NodeId;
import org.onosproject.nodemetrics.NodeDiskUsage;
import org.onosproject.nodemetrics.NodeMetricsService;

import java.util.Collection;
import java.util.Objects;

/**
 * Lists disk usage across nodes.
 */
@Service
@Command(scope = "onos", name = "node-disk",
        description = "Lists all node disk utilization")
public class ShowNodeDiskUsageCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "nodeId", description = "Node identity",
            required = false, multiValued = false)
    String nodeId = null;
    private NodeMetricsService nodeService = AbstractShellCommand
            .get(NodeMetricsService.class);

    @Override
    protected void doExecute() {
        if (nodeId != null) {
            NodeDiskUsage disk = nodeService.disk(NodeId.nodeId(nodeId));
            if (Objects.nonNull(disk)) {
                print("Disk usage : %s", disk);
            } else {
                print("Node %s doesn't exists", nodeId);
            }

        } else {
            Collection<NodeDiskUsage> disk = nodeService.disk().values();
            printDiskUsage(disk);
        }
    }

    private void printDiskUsage(Collection<NodeDiskUsage> diskList) {
        diskList.forEach(disk -> print("%s", disk));
    }
}
