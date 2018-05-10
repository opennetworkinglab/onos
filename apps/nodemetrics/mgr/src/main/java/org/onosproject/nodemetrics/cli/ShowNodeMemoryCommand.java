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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.NodeId;
import org.onosproject.nodemetrics.NodeMemory;
import org.onosproject.nodemetrics.NodeMetricsService;

import java.util.Collection;
import java.util.Objects;

/**
 * Lists memory usage across nodes.
 */
@Command(scope = "onos", name = "node-memory",
        description = "Lists all node memory utilization")
public class ShowNodeMemoryCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "nodeId", description = "Node identity",
            required = false, multiValued = false)
    String nodeId = null;
    private NodeMetricsService nodeService = AbstractShellCommand
            .get(NodeMetricsService.class);

    @Override
    protected void execute() {
        if (nodeId != null) {
            NodeMemory memory = nodeService.memory(NodeId.nodeId(nodeId));
            if (Objects.nonNull(memory)) {
                print("Memory usage : %s", memory.toString());
            } else {
                print("Node %s doesn't exists");
            }
        } else {
            Collection<NodeMemory> memory = nodeService.memory().values();
            printMemory(memory);
        }
    }

    private void printMemory(Collection<NodeMemory> memoryList) {
        memoryList.forEach(memory -> print("%s", memory));
    }
}
