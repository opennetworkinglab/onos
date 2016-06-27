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
        if (operation.equals("run")) {
            print(leaderElector.run(topic, localNodeId));
        } else if (operation.equals("withdraw")) {
            leaderElector.withdraw(topic);
        } else if (operation.equals("show")) {
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
