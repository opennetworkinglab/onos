/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.cpman.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.cpman.ControlLoad;
import org.onosproject.cpman.ControlMetricType;
import org.onosproject.cpman.ControlPlaneMonitorService;
import org.onosproject.net.DeviceId;

import java.util.Optional;
import java.util.Set;

import static org.onosproject.cpman.ControlResource.CONTROL_MESSAGE_METRICS;
import static org.onosproject.cpman.ControlResource.CPU_METRICS;
import static org.onosproject.cpman.ControlResource.DISK_METRICS;
import static org.onosproject.cpman.ControlResource.MEMORY_METRICS;
import static org.onosproject.cpman.ControlResource.NETWORK_METRICS;

/**
 * Lists all stats information of control plane metrics.
 */
@Command(scope = "onos", name = "cpman-stats-list",
        description = "Lists control metrics statistics")
public class ControlMetricsStatsListCommand extends AbstractShellCommand {

    private static final String FMT = "metricType=%s, latestValue=%d, " +
            "averageValue=%d, latestTime=%s";
    private static final String INVALID_TYPE = "Invalid control resource type.";

    @Argument(index = 0, name = "type",
            description = "Resource type (cpu|memory|disk|network|control_message)",
            required = true, multiValued = false)
    String type = null;

    @Argument(index = 1, name = "name", description = "Resource name (or Device Id)",
            required = false, multiValued = false)
    String name = null;

    @Override
    protected void execute() {
        ControlPlaneMonitorService service = get(ControlPlaneMonitorService.class);
        ClusterService clusterService = get(ClusterService.class);
        NodeId nodeId = clusterService.getLocalNode().id();
        switch (type) {
            case "cpu":
                printMetricsStats(service, nodeId, CPU_METRICS);
                break;
            case "memory":
                printMetricsStats(service, nodeId, MEMORY_METRICS);
                break;
            case "disk":
                printMetricsStats(service, nodeId, DISK_METRICS, name);
                break;
            case "network":
                printMetricsStats(service, nodeId, NETWORK_METRICS, name);
                break;
            case "control_message":
                if (name != null) {
                    printMetricsStats(service, nodeId, CONTROL_MESSAGE_METRICS, DeviceId.deviceId(name));
                }
                break;
            default:
                print(INVALID_TYPE);
                break;
        }
    }

    private void printMetricsStats(ControlPlaneMonitorService service, NodeId nodeId,
                                   Set<ControlMetricType> typeSet) {
        printMetricsStats(service, nodeId, typeSet, null, null);
    }

    private void printMetricsStats(ControlPlaneMonitorService service, NodeId nodeId,
                                   Set<ControlMetricType> typeSet, String name) {
        printMetricsStats(service, nodeId, typeSet, name, null);
    }

    private void printMetricsStats(ControlPlaneMonitorService service, NodeId nodeId,
                                   Set<ControlMetricType> typeSet, DeviceId did) {
        printMetricsStats(service, nodeId, typeSet, null, did);
    }

    private void printMetricsStats(ControlPlaneMonitorService service, NodeId nodeId,
                                   Set<ControlMetricType> typeSet, String name, DeviceId did) {
        if (name == null && did == null) {
            typeSet.forEach(s -> print(s, service.getLoad(nodeId, s, Optional.ofNullable(null))));
        } else if (name == null && did != null) {
            typeSet.forEach(s -> print(s, service.getLoad(nodeId, s, Optional.of(did))));
        } else if (name != null && did == null) {
            typeSet.forEach(s -> print(s, service.getLoad(nodeId, s, name)));
        }
    }

    private void print(ControlMetricType type, ControlLoad cl) {
        if (cl != null) {
            print(FMT, type.toString(), cl.latest(), cl.average(), cl.time());
        }
    }
}
