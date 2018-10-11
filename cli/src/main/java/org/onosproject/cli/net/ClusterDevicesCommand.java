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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onosproject.utils.Comparators;
import org.onosproject.net.DeviceId;
import org.onosproject.net.topology.TopologyCluster;

import java.util.Collections;
import java.util.List;

import static org.onosproject.cli.MastersListCommand.json;
import static org.onosproject.net.topology.ClusterId.clusterId;

/**
 * Lists devices of the specified topology cluster in the current topology.
 */
@Service
@Command(scope = "onos", name = "topo-cluster-devices",
         description = "Lists devices of the specified topology cluster in the current topology")
public class ClusterDevicesCommand extends ClustersListCommand {

    @Argument(index = 0, name = "id", description = "Cluster ID",
              required = true, multiValued = false)
    @Completion(ClusterIdCompleter.class)
    String id = null;

    @Override
    protected void doExecute() {
        int cid = Integer.parseInt(id);
        init();
        TopologyCluster cluster = service.getCluster(topology, clusterId(cid));
        if (cluster == null) {
            error("No such cluster %s", cid);
        } else {
            List<DeviceId> ids = Lists.newArrayList(service.getClusterDevices(topology, cluster));
            Collections.sort(ids, Comparators.ELEMENT_ID_COMPARATOR);
            if (outputJson()) {
                print("%s", json(new ObjectMapper(), ids));
            } else {
                for (DeviceId deviceId : ids) {
                    print("%s", deviceId);
                }
            }
        }
    }

}
