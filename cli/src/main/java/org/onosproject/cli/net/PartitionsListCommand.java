/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.cli.net;

import java.util.List;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.primitives.PartitionAdminService;
import org.onosproject.store.service.PartitionClientInfo;
import org.onosproject.store.service.PartitionInfo;
import org.onosproject.store.service.StorageAdminService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;

/**
 * Command to list the database partitions in the system.
 */
@Command(scope = "onos", name = "partitions",
        description = "Lists information about partitions in the system")
public class PartitionsListCommand extends AbstractShellCommand {

    @Option(name = "-c", aliases = "--clients",
            description = "Show inforamtion about partition clients",
            required = false, multiValued = false)
    private boolean reportClientInfo = false;

    private static final String SERVER_FMT = "%-20s %8s %25s %s";
    private static final String CLIENT_FMT = "%-20s %8s %10s %25s";

    /**
     * Displays partition info as text.
     *
     * @param partitionInfo partition descriptions
     */
    private void displayPartitions(List<PartitionInfo> partitionInfo) {
        if (partitionInfo.isEmpty()) {
            return;
        }
        print("----------------------------------------------------------");
        print(SERVER_FMT, "Name", "Term", "Members", "");
        print("----------------------------------------------------------");

        for (PartitionInfo info : partitionInfo) {
            boolean first = true;
            for (String member : Ordering.natural().sortedCopy(info.members())) {
                if (first) {
                    print(SERVER_FMT, info.name(), info.term(), member,
                            member.equals(info.leader()) ? "*" : "");
                    first = false;
                } else {
                    print(SERVER_FMT, "", "", member,
                            member.equals(info.leader()) ? "*" : "");
                }
            }
            if (!first) {
                print("----------------------------------------------------------");
            }
        }
    }

    /**
     * Displays partition client info as text.
     *
     * @param partitionClientInfo partition client information
     */
    private void displayPartitionClients(List<PartitionClientInfo> partitionClientInfo) {
        if (partitionClientInfo.isEmpty()) {
            return;
        }
        ClusterService clusterService = get(ClusterService.class);
        print("-------------------------------------------------------------------");
        print(CLIENT_FMT, "Name", "SessionId", "Status", "Servers");
        print("-------------------------------------------------------------------");

        for (PartitionClientInfo info : partitionClientInfo) {
            boolean first = true;
            for (NodeId serverId : Ordering.natural().sortedCopy(info.servers())) {
                ControllerNode server = clusterService.getNode(serverId);
                String serverString = String.format("%s:%d", server.id(), server.tcpPort());
                if (first) {
                    print(CLIENT_FMT, info.partitionId(), info.sessionId(),
                            info.status(), serverString);
                    first = false;
                } else {
                    print(CLIENT_FMT, "", "", "", serverString);
                }
            }
            if (!first) {
                print("-------------------------------------------------------------------");
            }
        }
    }

    /**
     * Converts partition info into a JSON object.
     *
     * @param partitionInfo partition descriptions
     */
    private JsonNode json(List<PartitionInfo> partitionInfo) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode partitions = mapper.createArrayNode();

        // Create a JSON node for each partition
        partitionInfo.forEach(info -> {
            ObjectNode partition = mapper.createObjectNode();

            // Add each member to the "members" array for this partition
            ArrayNode members = partition.putArray("members");
            info.members().forEach(members::add);

            // Complete the partition attributes and add it to the array
            partition.put("name", info.name())
                    .put("term", info.term())
                    .put("leader", info.leader());
            partitions.add(partition);

        });

        return partitions;
    }

    /**
     * Converts partition client info into a JSON object.
     *
     * @param partitionClientInfo partition client descriptions
     */
    private JsonNode jsonForClientInfo(List<PartitionClientInfo> partitionClientInfo) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode partitions = mapper.createArrayNode();
        ClusterService clusterService = get(ClusterService.class);

        // Create a JSON node for each partition client
        partitionClientInfo.forEach(info -> {
            ObjectNode partition = mapper.createObjectNode();

            // Add each member to the "servers" array for this partition
            ArrayNode servers = partition.putArray("servers");
            info.servers()
                    .stream()
                    .map(clusterService::getNode)
                    .map(node -> String.format("%s:%d", node.ip(), node.tcpPort()))
                    .forEach(servers::add);

            // Complete the partition attributes and add it to the array
            partition.put("partitionId", info.partitionId().toString())
                    .put("sessionId", info.sessionId())
                    .put("status", info.status().toString());
            partitions.add(partition);

        });

        return partitions;
    }

    @Override
    protected void execute() {
        StorageAdminService storageAdminService = get(StorageAdminService.class);
        if (reportClientInfo) {
            PartitionAdminService partitionAdminService = get(PartitionAdminService.class);
            List<PartitionClientInfo> partitionClientInfo = partitionAdminService.partitionClientInfo();
            if (outputJson()) {
                print("%s", jsonForClientInfo(partitionClientInfo));
            } else {
                displayPartitionClients(partitionClientInfo);
            }
        } else {
            List<PartitionInfo> partitionInfo = storageAdminService.getPartitionInfo();
            if (outputJson()) {
                print("%s", json(partitionInfo));
            } else {
                displayPartitions(partitionInfo);
            }
        }
    }
}
