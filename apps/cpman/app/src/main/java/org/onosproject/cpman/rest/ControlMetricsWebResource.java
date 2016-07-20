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
package org.onosproject.cpman.rest;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.StringUtils;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.cpman.ControlLoadSnapshot;
import org.onosproject.cpman.ControlMetricType;
import org.onosproject.cpman.ControlPlaneMonitorService;
import org.onosproject.net.DeviceId;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Optional;
import java.util.Set;

import static org.onosproject.cpman.ControlResource.CONTROL_MESSAGE_METRICS;
import static org.onosproject.cpman.ControlResource.CPU_METRICS;
import static org.onosproject.cpman.ControlResource.DISK_METRICS;
import static org.onosproject.cpman.ControlResource.MEMORY_METRICS;
import static org.onosproject.cpman.ControlResource.NETWORK_METRICS;
import static org.onosproject.cpman.ControlResource.Type.CONTROL_MESSAGE;
import static org.onosproject.cpman.ControlResource.Type.DISK;
import static org.onosproject.cpman.ControlResource.Type.NETWORK;

/**
 * Query control metrics.
 */
@Path("controlmetrics")
public class ControlMetricsWebResource extends AbstractWebResource {

    private final ControlPlaneMonitorService monitorService =
            get(ControlPlaneMonitorService.class);
    private final ClusterService clusterService = get(ClusterService.class);
    private final NodeId localNodeId = clusterService.getLocalNode().id();
    private final ObjectNode root = mapper().createObjectNode();

    /**
     * Returns control message metrics of all devices.
     *
     * @return array of all control message metrics
     * @onos.rsModel ControlMessageMetrics
     */
    @GET
    @Path("messages")
    @Produces(MediaType.APPLICATION_JSON)
    public Response controlMessageMetrics() {

        ArrayNode deviceNodes = root.putArray("devices");
        monitorService.availableResourcesSync(localNodeId, CONTROL_MESSAGE).forEach(name -> {
            ObjectNode deviceNode = mapper().createObjectNode();
            ObjectNode valueNode = mapper().createObjectNode();

            metricsStats(monitorService, localNodeId, CONTROL_MESSAGE_METRICS,
                    DeviceId.deviceId(name), valueNode);
            deviceNode.put("name", name);
            deviceNode.set("value", valueNode);

            deviceNodes.add(deviceNode);
        });

        return ok(root).build();
    }

    /**
     * Returns control message metrics of a given device.
     *
     * @param deviceId device identification
     * @return control message metrics of a given device
     * @onos.rsModel ControlMessageMetric
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("messages/{deviceId}")
    public Response controlMessageMetrics(@PathParam("deviceId") String deviceId) {

        metricsStats(monitorService, localNodeId, CONTROL_MESSAGE_METRICS,
                DeviceId.deviceId(deviceId), root);

        return ok(root).build();
    }

    /**
     * Returns cpu metrics.
     *
     * @return cpu metrics
     * @onos.rsModel CpuMetrics
     */
    @GET
    @Path("cpu_metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response cpuMetrics() {

        metricsStats(monitorService, localNodeId, CPU_METRICS, root);
        return ok(root).build();
    }

    /**
     * Returns memory metrics.
     *
     * @return memory metrics
     * @onos.rsModel MemoryMetrics
     */
    @GET
    @Path("memory_metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response memoryMetrics() {

        metricsStats(monitorService, localNodeId, MEMORY_METRICS, root);
        return ok(root).build();
    }

    /**
     * Returns disk metrics of all resources.
     *
     * @return disk metrics of all resources
     * @onos.rsModel DiskMetrics
     */
    @GET
    @Path("disk_metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response diskMetrics() {

        ArrayNode diskNodes = root.putArray("disks");
        monitorService.availableResourcesSync(localNodeId, DISK).forEach(name -> {
            ObjectNode diskNode = mapper().createObjectNode();
            ObjectNode valueNode = mapper().createObjectNode();

            metricsStats(monitorService, localNodeId, DISK_METRICS, name, valueNode);
            diskNode.put("name", name);
            diskNode.set("value", valueNode);

            diskNodes.add(diskNode);
        });

        return ok(root).build();
    }

    /**
     * Returns network metrics of all resources.
     *
     * @return network metrics of all resources
     * @onos.rsModel NetworkMetrics
     */
    @GET
    @Path("network_metrics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response networkMetrics() {

        ArrayNode networkNodes = root.putArray("networks");
        monitorService.availableResourcesSync(localNodeId, NETWORK).forEach(name -> {
            ObjectNode networkNode = mapper().createObjectNode();
            ObjectNode valueNode = mapper().createObjectNode();

            metricsStats(monitorService, localNodeId, NETWORK_METRICS, name, valueNode);
            networkNode.put("name", name);
            networkNode.set("value", valueNode);

            networkNodes.add(networkNode);
        });

        return ok(root).build();
    }

    /**
     * Returns a collection of control message stats.
     *
     * @param service control plane monitoring service
     * @param nodeId  node identification
     * @param typeSet a set of control message types
     * @param did     device identification
     * @param node    object node
     * @return a collection of control message stats
     */
    private ArrayNode metricsStats(ControlPlaneMonitorService service,
                                   NodeId nodeId, Set<ControlMetricType> typeSet,
                                   DeviceId did, ObjectNode node) {
        return metricsStats(service, nodeId, typeSet, null, did, node);
    }

    /**
     * Returns a collection of system metric stats.
     *
     * @param service control plane monitoring service
     * @param nodeId  node identification
     * @param typeSet a set of system metric types
     * @param node    object node
     * @return a collection of system metric stats
     */
    private ArrayNode metricsStats(ControlPlaneMonitorService service,
                                   NodeId nodeId, Set<ControlMetricType> typeSet,
                                   ObjectNode node) {
        return metricsStats(service, nodeId, typeSet, null, null, node);
    }

    /**
     * Returns a collection of system metric stats.
     *
     * @param service      control plane monitoring service
     * @param nodeId       node identification
     * @param typeSet      a set of control message types
     * @param resourceName device identification
     * @param node         object node
     * @return a collection of system metric stats
     */
    private ArrayNode metricsStats(ControlPlaneMonitorService service,
                                   NodeId nodeId, Set<ControlMetricType> typeSet,
                                   String resourceName, ObjectNode node) {
        return metricsStats(service, nodeId, typeSet, resourceName, null, node);
    }

    /**
     * Returns a collection of control loads of the given control metric types.
     *
     * @param service control plane monitoring service
     * @param nodeId  node identification
     * @param typeSet a group of control metric types
     * @param name    resource name
     * @param did     device identification
     * @return a collection of control loads
     */
    private ArrayNode metricsStats(ControlPlaneMonitorService service,
                                   NodeId nodeId, Set<ControlMetricType> typeSet,
                                   String name, DeviceId did, ObjectNode node) {
        ArrayNode metricsNode = node.putArray("metrics");

        if (name == null && did == null) {
            typeSet.forEach(type -> {
                ControlLoadSnapshot cls = service.getLoadSync(nodeId, type, Optional.empty());
                processRest(cls, type, metricsNode);
            });
        } else if (name == null) {
            typeSet.forEach(type -> {
                ControlLoadSnapshot cls = service.getLoadSync(nodeId, type, Optional.of(did));
                processRest(cls, type, metricsNode);
            });
        } else if (did == null) {
            typeSet.forEach(type -> {
                ControlLoadSnapshot cls = service.getLoadSync(nodeId, type, name);
                processRest(cls, type, metricsNode);
            });
        }

        return metricsNode;
    }

    /**
     * Camelizes the input string.
     *
     * @param value              original string
     * @param startWithLowerCase flag that determines whether to use lower case
     *                           for the camelized string
     * @return camelized string
     */
    private String toCamelCase(String value, boolean startWithLowerCase) {
        String[] strings = StringUtils.split(value.toLowerCase(), "_");
        for (int i = startWithLowerCase ? 1 : 0; i < strings.length; i++) {
            strings[i] = StringUtils.capitalize(strings[i]);
        }
        return StringUtils.join(strings);
    }

    /**
     * Transforms control load snapshot object into JSON object.
     *
     * @param cls         control load snapshot
     * @param type        control metric type
     * @param metricsNode array of JSON node
     */
    private void processRest(ControlLoadSnapshot cls, ControlMetricType type, ArrayNode metricsNode) {
        ObjectNode metricNode = mapper().createObjectNode();

        if (cls != null) {
            metricNode.set(toCamelCase(type.toString(), true),
                    codec(ControlLoadSnapshot.class).encode(cls, this));
            metricsNode.add(metricNode);
        }
    }
}
