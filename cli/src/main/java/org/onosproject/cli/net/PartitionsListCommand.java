/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.store.service.PartitionInfo;
import org.onosproject.store.service.StorageAdminService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Command to list the database partitions in the system.
 */
@Command(scope = "onos", name = "partitions",
        description = "Lists information about partitions in the system")
public class PartitionsListCommand extends AbstractShellCommand {

    private static final String FMT = "%-20s %8s %25s %s";

    /**
     * Displays partition info as text.
     *
     * @param partitionInfo partition descriptions
     */
    private void displayPartitions(List<PartitionInfo> partitionInfo) {
        print("----------------------------------------------------------");
        print(FMT, "Name", "Term", "Members", "");
        print("----------------------------------------------------------");

        for (PartitionInfo info : partitionInfo) {
            boolean first = true;
            for (String member : info.members()) {
                if (first) {
                    print(FMT, info.name(), info.term(), member,
                            member.equals(info.leader()) ? "*" : "");
                    first = false;
                } else {
                    print(FMT, "", "", member,
                            member.equals(info.leader()) ? "*" : "");
                }
            }
            if (!first) {
                print("----------------------------------------------------------");
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
        partitionInfo.stream()
            .forEach(info -> {
                ObjectNode partition = mapper.createObjectNode();

                // Add each member to the "members" array for this partition
                ArrayNode members = partition.putArray("members");
                info.members()
                        .stream()
                        .forEach(members::add);

                // Complete the partition attributes and add it to the array
                partition.put("name", info.name())
                         .put("term", info.term())
                         .put("leader", info.leader());
                partitions.add(partition);

            });

        return partitions;
    }

    @Override
    protected void execute() {
        StorageAdminService storageAdminService = get(StorageAdminService.class);
        List<PartitionInfo> partitionInfo = storageAdminService.getPartitionInfo();

        if (outputJson()) {
            print("%s", json(partitionInfo));
        } else {
            displayPartitions(partitionInfo);
        }
    }
}
