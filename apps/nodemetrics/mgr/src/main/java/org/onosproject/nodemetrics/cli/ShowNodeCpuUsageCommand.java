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
import org.onosproject.nodemetrics.NodeCpuUsage;
import org.onosproject.nodemetrics.NodeMetricsService;

import java.util.Collection;
import java.util.Objects;

/**
 * Lists cpu usage across nodes.
 */
@Service
@Command(scope = "onos", name = "node-cpu",
        description = "Lists all node cpu utilization")
public class ShowNodeCpuUsageCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "nodeId", description = "Node identity",
            required = false, multiValued = false)
    String nodeId = null;
    private NodeMetricsService nodeService = AbstractShellCommand
            .get(NodeMetricsService.class);

    @Override
    protected void doExecute() {
        if (nodeId != null) {
            NodeCpuUsage cpu = nodeService.cpu(NodeId.nodeId(nodeId));
            if (Objects.nonNull(cpu)) {
                print("CPU usage : %s ", cpu);
            } else {
                print("Node %s doesn't exists", nodeId);
            }
        } else {
            Collection<NodeCpuUsage> cpu = nodeService.cpu().values();
            printCpuUsage(cpu);
        }
    }

    private void printCpuUsage(Collection<NodeCpuUsage> cpuList) {
        cpuList.forEach(cpu -> print("%s", cpu));
    }
}

