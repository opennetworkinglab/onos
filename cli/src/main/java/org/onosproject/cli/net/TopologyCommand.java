/*
 * Copyright 2014 Open Networking Laboratory
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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyProvider;
import org.onosproject.net.topology.TopologyService;

/**
 * Lists summary of the current topology.
 */
@Command(scope = "onos", name = "topology",
         description = "Lists summary of the current topology")
public class TopologyCommand extends AbstractShellCommand {

    private static final String FMT =
            "time=%s, devices=%d, links=%d, clusters=%d, paths=%d";

    @Option(name = "-r", aliases = "--recompute", description = "Trigger topology re-computation",
            required = false, multiValued = false)
    private boolean recompute = false;

    protected TopologyService service;
    protected Topology topology;

    /**
     * Initializes the context for all cluster commands.
     */
    protected void init() {
        service = get(TopologyService.class);
        topology = service.currentTopology();
    }

    @Override
    protected void execute() {
        init();
        if (recompute) {
            get(TopologyProvider.class).triggerRecompute();

        } else if (outputJson()) {
            print("%s", new ObjectMapper().createObjectNode()
                    .put("time", topology.time())
                    .put("deviceCount", topology.deviceCount())
                    .put("linkCount", topology.linkCount())
                    .put("clusterCount", topology.clusterCount())
                    .put("pathCount", topology.pathCount()));
        } else {
            print(FMT, topology.time(), topology.deviceCount(), topology.linkCount(),
                  topology.clusterCount(), topology.pathCount());
        }
    }

}
