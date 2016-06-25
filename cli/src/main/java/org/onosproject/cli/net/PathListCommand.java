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
package org.onosproject.cli.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.Link;
import org.onosproject.net.Path;

import java.util.Set;

import static org.onosproject.cli.net.LinksListCommand.compactLinkString;
import static org.onosproject.net.DeviceId.deviceId;

/**
 * Lists all shortest-paths paths between the specified source and
 * destination devices.
 */
@Command(scope = "onos", name = "paths",
         description = "Lists all shortest-paths paths between the specified source and destination devices")
public class PathListCommand extends TopologyCommand {

    private static final String SEP = "==>";

    @Argument(index = 0, name = "src", description = "Source device ID",
              required = true, multiValued = false)
    String src = null;

    @Argument(index = 1, name = "dst", description = "Destination device ID",
              required = true, multiValued = false)
    String dst = null;

    @Override
    protected void execute() {
        init();
        if (src.split("/").length != 1 || dst.split("/").length != 1) {
            print("Expected device IDs as arguments");
            return;
        }
        Set<Path> paths = service.getPaths(topology, deviceId(src), deviceId(dst));
        if (outputJson()) {
            print("%s", json(this, paths));
        } else {
            for (Path path : paths) {
                print(pathString(path));
            }
        }
    }

    /**
     * Produces a JSON array containing the specified paths.
     *
     * @param context context to use for looking up codecs
     * @param paths collection of paths
     * @return JSON array
     */
    public static JsonNode json(AbstractShellCommand context, Iterable<Path> paths) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Path path : paths) {
            result.add(LinksListCommand.json(context, path)
                    .put("cost", path.cost())
                    .set("links", LinksListCommand.json(context, path.links())));
        }
        return result;
    }

    /**
     * Produces a formatted string representing the specified path.
     *
     * @param path network path
     * @return formatted path string
     */
    protected String pathString(Path path) {
        StringBuilder sb = new StringBuilder();
        for (Link link : path.links()) {
            sb.append(compactLinkString(link)).append(SEP);
        }
        sb.delete(sb.lastIndexOf(SEP), sb.length());
        sb.append("; cost=").append(path.cost());
        return sb.toString();
    }

}
