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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.net.Link;
import org.onosproject.net.topology.TopologyCluster;

import static org.onosproject.cli.net.LinksListCommand.json;
import static org.onosproject.cli.net.LinksListCommand.linkString;
import static org.onosproject.net.topology.ClusterId.clusterId;

/**
 * Lists links of the specified topology cluster in the current topology.
 */
@Command(scope = "onos", name = "cluster-links",
         description = "Lists links of the specified topology cluster in the current topology")
public class ClusterLinksCommand extends ClustersListCommand {

    @Argument(index = 0, name = "id", description = "Cluster ID",
              required = true, multiValued = false)
    String id = null;

    @Override
    protected void execute() {
        int cid = Integer.parseInt(id);
        init();
        TopologyCluster cluster = service.getCluster(topology, clusterId(cid));
        if (cluster == null) {
            error("No such cluster %s", cid);
        } else if (outputJson()) {
            print("%s", json(this, service.getClusterLinks(topology, cluster)));
        } else {
            for (Link link : service.getClusterLinks(topology, cluster)) {
                print(linkString(link));
            }
        }
    }

}
