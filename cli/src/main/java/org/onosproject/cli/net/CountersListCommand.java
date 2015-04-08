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

import java.util.Map;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.store.service.StorageAdminService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Command to list the various counters in the system.
 */
@Command(scope = "onos", name = "counters",
        description = "Lists information about atomic counters in the system")
public class CountersListCommand extends AbstractShellCommand {

    private static final String FMT = "name=%s value=%d";

    /**
     * Displays counters as text.
     *
     * @param mapInfo map descriptions
     */
    private void displayCounters(Map<String, Long> counters) {
        counters.forEach((name, value) -> print(FMT, name, value));
    }

    /**
     * Converts info for counters into a JSON object.
     *
     * @param counters counter info
     */
    private JsonNode json(Map<String, Long> counters) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode jsonCounters = mapper.createArrayNode();

        // Create a JSON node for each counter
        counters.forEach((name, value) -> {
                ObjectNode jsonCounter = mapper.createObjectNode();
                jsonCounter.put("name", name)
                   .put("value", value);
                jsonCounters.add(jsonCounter);
            });

        return jsonCounters;
    }

    @Override
    protected void execute() {
        StorageAdminService storageAdminService = get(StorageAdminService.class);
        Map<String, Long> counters = storageAdminService.getCounters();
        if (outputJson()) {
            print("%s", json(counters));
        } else {
            displayCounters(counters);
        }
    }
}
