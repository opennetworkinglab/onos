/*
 * Copyright 2014-present Open Networking Foundation
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
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.device.DeviceService;

import java.util.Set;

import static org.onosproject.cli.net.LinksListCommand.compactLinkString;
import static org.onosproject.net.DeviceId.deviceId;

/**
 * Lists all shortest-paths paths between the specified source and
 * destination devices.
 */
@Service
@Command(scope = "onos", name = "paths",
         description = "Lists all shortest-paths paths between the specified source and destination devices")
public class PathListCommand extends TopologyCommand {

    private static final String SEP = "==>";

    @Argument(index = 0, name = "src", description = "Source device ID",
              required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String src = null;

    @Argument(index = 1, name = "dst", description = "Destination device ID",
              required = true, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String dst = null;

    @Option(name = "--disjoint", description = "Show disjoint Paths")
    boolean disjoint = false;

    @Override
    protected void doExecute() {
        init();
        DeviceService deviceService = get(DeviceService.class);
        DeviceId srcDid = deviceId(src);
        if (deviceService.getDevice(srcDid) == null) {
            print("Unknown device %s", src);
            return;
        }
        DeviceId dstDid = deviceId(dst);
        if (deviceService.getDevice(dstDid) == null) {
            print("Unknown device %s", dst);
            return;
        }
        Set<? extends Path> paths;
        if (disjoint) {
            paths = service.getDisjointPaths(topology, srcDid, dstDid);
        } else {
            paths = service.getPaths(topology, srcDid, dstDid);
        }
        if (outputJson()) {
            print("%s", json(this, paths));
        } else {
            for (Path path : paths) {
                print(pathString(path));
                if (path instanceof DisjointPath) {
                    // print backup right after primary
                    print(pathString(((DisjointPath) path).backup()));
                }
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
    public static JsonNode json(AbstractShellCommand context,
                                Iterable<? extends Path> paths) {
        ObjectMapper mapper = context.mapper();
        ArrayNode result = mapper.createArrayNode();
        for (Path path : paths) {
            result.add(LinksListCommand.json(context, path)
                    .put("cost", path.cost())
                    .set("links", LinksListCommand.json(context, path.links())));

            if (path instanceof DisjointPath) {
                // [ (primay), (backup), ...]
                DisjointPath backup = (DisjointPath) path;
                result.add(LinksListCommand.json(context, backup.backup())
                           .put("cost", backup.cost())
                           .set("links", LinksListCommand.json(context, backup.links())));
            }
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
