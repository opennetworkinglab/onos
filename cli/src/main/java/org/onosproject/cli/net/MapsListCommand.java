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
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.store.service.MapInfo;
import org.onosproject.store.service.StorageAdminService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Command to list the various maps in the system.
 */
@Command(scope = "onos", name = "maps",
        description = "Lists information about consistent maps in the system")
public class MapsListCommand extends AbstractShellCommand {

    // TODO: Add support to display different eventually
    // consistent maps as well.

    private static final String FMT = "name=%s size=%d";

    /**
     * Displays map info as text.
     *
     * @param mapInfo map descriptions
     */
    private void displayMaps(List<MapInfo> mapInfo) {
        for (MapInfo info : mapInfo) {
            print(FMT, info.name(), info.size());
        }
    }

    /**
     * Converts list of map info into a JSON object.
     *
     * @param mapInfo map descriptions
     */
    private JsonNode json(List<MapInfo> mapInfo) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode maps = mapper.createArrayNode();

        // Create a JSON node for each map
        mapInfo.forEach(info -> {
            ObjectNode map = mapper.createObjectNode();
            map.put("name", info.name())
                    .put("size", info.size());
            maps.add(map);
        });

        return maps;
    }

    @Override
    protected void execute() {
        StorageAdminService storageAdminService = get(StorageAdminService.class);
        List<MapInfo> mapInfo = storageAdminService.getMapInfo();
        if (outputJson()) {
            print("%s", json(mapInfo));
        } else {
            displayMaps(mapInfo);
        }
    }
}
