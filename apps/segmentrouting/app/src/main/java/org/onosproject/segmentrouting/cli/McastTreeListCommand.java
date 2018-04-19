/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.segmentrouting.cli;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.mcast.cli.McastGroupCompleter;
import org.onosproject.net.ConnectPoint;
import org.onosproject.segmentrouting.SegmentRoutingService;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Command to show the list of mcast trees.
 */
@Command(scope = "onos", name = "sr-mcast-tree",
        description = "Lists all mcast trees")
public class McastTreeListCommand extends AbstractShellCommand {

    // OSGi workaround to introduce package dependency
    McastGroupCompleter completer;

    // Format for group line
    private static final String G_FORMAT_MAPPING = "group=%s";
    // Format for sink line
    private static final String S_FORMAT_MAPPING = "  sink=%s\tpath=%s";

    @Option(name = "-gAddr", aliases = "--groupAddress",
            description = "IP Address of the multicast group",
            valueToShowInHelp = "224.0.0.0",
            required = false, multiValued = false)
    String gAddr = null;

    @Option(name = "-src", aliases = "--connectPoint",
            description = "Source port of:XXXXXXXXXX/XX",
            valueToShowInHelp = "of:0000000000000001/1",
            required = false, multiValued = false)
    String source = null;

    @Override
    protected void execute() {
        // Get SR service and the handled mcast groups
        SegmentRoutingService srService = get(SegmentRoutingService.class);
        Set<IpAddress> mcastGroups = ImmutableSet.copyOf(srService.getMcastLeaders(null)
                                                                         .keySet());

        if (!isNullOrEmpty(gAddr)) {
            mcastGroups = mcastGroups.stream()
                    .filter(mcastIp -> mcastIp.equals(IpAddress.valueOf(gAddr)))
                    .collect(Collectors.toSet());
        }

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();

        // Print the trees for each group or build json objects
        mcastGroups.forEach(group -> {
            // We want to use source cp only for a specific group
            ConnectPoint sourcecp = null;
            if (!isNullOrEmpty(source) &&
                    !isNullOrEmpty(gAddr)) {
                sourcecp = ConnectPoint.deviceConnectPoint(source);
            }
            Multimap<ConnectPoint, List<ConnectPoint>> mcastTree = srService.getMcastTrees(group,
                                                                                           sourcecp);
            if (!mcastTree.isEmpty()) {
                // Build a json object for each group
                if (outputJson()) {
                    root.putPOJO(group.toString(), json(mcastTree));
                } else {
                    // Banner and then the trees
                    printMcastGroup(group);
                    mcastTree.forEach(this::printMcastSink);
                }
            }
        });

        // Print the json object at the end
        if (outputJson()) {
            print("%s", root);
        }

    }

    private void printMcastGroup(IpAddress mcastGroup) {
        print(G_FORMAT_MAPPING, mcastGroup);
    }

    private void printMcastSink(ConnectPoint sink, List<ConnectPoint> path) {
        print(S_FORMAT_MAPPING, sink, path);
    }

    private ObjectNode json(Multimap<ConnectPoint, List<ConnectPoint>> mcastTree) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode jsonSinks = mapper.createObjectNode();
        mcastTree.asMap().forEach((sink, paths) -> {
            ArrayNode jsonPaths = mapper.createArrayNode();
            paths.forEach(path -> {
                ArrayNode jsonPath = mapper.createArrayNode();
                path.forEach(connectPoint -> jsonPath.add(connectPoint.toString()));
                jsonPaths.addPOJO(jsonPath);
            });
            jsonSinks.putPOJO(sink.toString(), jsonPaths);
        });
        return jsonSinks;
    }

}