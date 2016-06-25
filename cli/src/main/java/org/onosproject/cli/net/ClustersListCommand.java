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
import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.utils.Comparators;
import org.onosproject.net.topology.TopologyCluster;

import java.util.Collections;
import java.util.List;

/**
 * Lists all clusters in the current topology.
 */
@Command(scope = "onos", name = "clusters",
         description = "Lists all clusters in the current topology")
public class ClustersListCommand extends TopologyCommand {

    private static final String FMT =
            "id=%d, devices=%d, links=%d";

    @Override
    protected void execute() {
        init();
        List<TopologyCluster> clusters = Lists.newArrayList(service.getClusters(topology));
        Collections.sort(clusters, Comparators.CLUSTER_COMPARATOR);

        if (outputJson()) {
            print("%s", json(clusters));
        } else {
            for (TopologyCluster cluster : clusters) {
                print(FMT, cluster.id().index(), cluster.deviceCount(), cluster.linkCount());
            }
        }
    }

    // Produces a JSON result.
    private JsonNode json(Iterable<TopologyCluster> clusters) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        clusters.spliterator()
                .forEachRemaining(cluster ->
                        result.add(jsonForEntity(cluster, TopologyCluster.class)));

        return result;
    }

}
