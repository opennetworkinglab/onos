/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.distributedprimitives.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.Leadership;
import org.onosproject.cluster.NodeId;
import org.onosproject.store.service.LeaderElector;
import org.onosproject.store.service.StorageService;

import com.google.common.base.Joiner;

@Command(scope = "onos", name = "leader-test",
description = "LeaderElector test cli fixture")
public class LeaderElectorTestCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "name",
            description = "leader elector name",
            required = true, multiValued = false)
    String name = null;

    @Argument(index = 1, name = "operation",
            description = "operation",
            required = true, multiValued = false)
    String operation = null;


    @Argument(index = 2, name = "topic",
            description = "topic name",
            required = false, multiValued = false)
    String topic = null;

    LeaderElector leaderElector;

    @Override
    protected void execute() {
        StorageService storageService = get(StorageService.class);
        ClusterService clusterService = get(ClusterService.class);
        NodeId localNodeId = clusterService.getLocalNode().id();
        leaderElector = storageService.leaderElectorBuilder()
                .withName(name)
                .build()
                .asLeaderElector();
        if ("run".equals(operation)) {
            print(leaderElector.run(topic, localNodeId));
        } else if ("withdraw".equals(operation)) {
            leaderElector.withdraw(topic);
        } else if ("show".equals(operation)) {
            print(leaderElector.getLeadership(topic));
        }
    }

    private void print(Leadership leadership) {
        if (leadership.leader() != null) {
            print("leader=%s#term=%d#candidates=%s",
                    leadership.leaderNodeId(),
                    leadership.leader().term(),
                    leadership.candidates().isEmpty() ? "none" : Joiner.on(",").join(leadership.candidates()));
        } else {
            print("leader=none#candidates=%s",
                    leadership.candidates().isEmpty() ? "none" : Joiner.on(",").join(leadership.candidates()));
        }
    }
}
