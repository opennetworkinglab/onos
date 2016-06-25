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
package org.onosproject.cpman.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.NodeId;
import org.onosproject.cpman.ControlLoadSnapshot;
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

    @Argument(index = 0, name = "node", description = "ONOS node identifier",
            required = true, multiValued = false)
    String node = null;

    @Argument(index = 1, name = "type",
            description = "Resource type (cpu|memory|disk|network|control_message)",
            required = true, multiValued = false)
    String type = null;

    @Argument(index = 2, name = "name", description = "Resource name (or Device Id)",
            required = false, multiValued = false)
    String name = null;

    @Override
    protected void execute() {
        ControlPlaneMonitorService service = get(ControlPlaneMonitorService.class);
        NodeId nodeId = NodeId.nodeId(node);
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
                    printMetricsStats(service, nodeId, CONTROL_MESSAGE_METRICS,
                            DeviceId.deviceId(name));
                }
                break;
            default:
                print(INVALID_TYPE);
                break;
        }
    }

    /**
     * Prints system metric statistic information.
     *
     * @param service monitor service
     * @param nodeId  node identifier
     * @param typeSet control metric type
     */
    private void printMetricsStats(ControlPlaneMonitorService service, NodeId nodeId,
                                   Set<ControlMetricType> typeSet) {
        printMetricsStats(service, nodeId, typeSet, null, null);
    }

    /**
     * Prints disk and network metric statistic information.
     *
     * @param service monitor service
     * @param nodeId  node identifier
     * @param typeSet control metric type
     * @param resName resource name
     */
    private void printMetricsStats(ControlPlaneMonitorService service, NodeId nodeId,
                                   Set<ControlMetricType> typeSet, String resName) {
        printMetricsStats(service, nodeId, typeSet, resName, null);
    }

    /**
     * Prints control message metric statistic information.
     *
     * @param service monitor service
     * @param nodeId  node identifier
     * @param typeSet control metric type
     * @param did     device identifier
     */
    private void printMetricsStats(ControlPlaneMonitorService service, NodeId nodeId,
                                   Set<ControlMetricType> typeSet, DeviceId did) {
        printMetricsStats(service, nodeId, typeSet, null, did);
    }

    /**
     * Prints control plane metric statistic information.
     *
     * @param service monitor service
     * @param nodeId  node identifier
     * @param typeSet control metric type
     * @param resName resource name
     * @param did     device identifier
     */
    private void printMetricsStats(ControlPlaneMonitorService service, NodeId nodeId,
                                   Set<ControlMetricType> typeSet, String resName, DeviceId did) {
        if (resName == null && did == null) {
            typeSet.forEach(s -> {
                ControlLoadSnapshot cls = service.getLoadSync(nodeId, s, Optional.empty());
                printControlLoadSnapshot(s, cls);
            });
        } else if (resName == null) {
            typeSet.forEach(s -> {
                ControlLoadSnapshot cls = service.getLoadSync(nodeId, s, Optional.of(did));
                printControlLoadSnapshot(s, cls);
            });
        } else if (did == null) {
            typeSet.forEach(s -> {
                ControlLoadSnapshot cls = service.getLoadSync(nodeId, s, resName);
                printControlLoadSnapshot(s, cls);
            });
        }
    }

    /**
     * Prints control load snapshot.
     *
     * @param cmType control metric type
     * @param cls    control load snapshot
     */
    private void printControlLoadSnapshot(ControlMetricType cmType, ControlLoadSnapshot cls) {
        if (cls != null) {
            print(FMT, cmType.toString(), cls.latest(), cls.average(), cls.time());
        } else {
            print("Failed to retrieve metric value for type {}", cmType.toString());
        }
    }
}
