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
package org.onosproject.cpman.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.cpman.ControlMetric;
import org.onosproject.cpman.ControlMetricType;
import org.onosproject.cpman.ControlPlaneMonitorService;
import org.onosproject.cpman.MetricValue;
import org.onosproject.cpman.SystemInfo;
import org.onosproject.cpman.impl.DefaultSystemInfo;
import org.onosproject.cpman.impl.SystemInfoFactory;
import org.onosproject.rest.AbstractWebResource;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Collect control plane metrics.
 */
@Path("collector")
public class ControlMetricsCollectorWebResource extends AbstractWebResource {

    final ControlPlaneMonitorService service = get(ControlPlaneMonitorService.class);
    public static final int UPDATE_INTERVAL_IN_MINUTE = 1;
    public static final String INVALID_SYSTEM_SPECS = "Invalid system specifications";
    public static final String INVALID_RESOURCE_NAME = "Invalid resource name";
    public static final String INVALID_REQUEST = "Invalid request";

    /**
     * Collects CPU metrics.
     *
     * @param stream JSON stream
     * @return 200 OK
     * @onos.rsModel CpuMetricsPost
     */
    @POST
    @Path("cpu_metrics")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response cpuMetrics(InputStream stream) {
        ObjectNode root = mapper().createObjectNode();
        ControlMetric cm;
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            long cpuLoad = nullIsIllegal(jsonTree.get("cpuLoad").asLong(), INVALID_REQUEST);
            long totalCpuTime = nullIsIllegal(jsonTree.get("totalCpuTime").asLong(), INVALID_REQUEST);
            long sysCpuTime = nullIsIllegal(jsonTree.get("sysCpuTime").asLong(), INVALID_REQUEST);
            long userCpuTime = nullIsIllegal(jsonTree.get("userCpuTime").asLong(), INVALID_REQUEST);
            long cpuIdleTime = nullIsIllegal(jsonTree.get("cpuIdleTime").asLong(), INVALID_REQUEST);

            cm = new ControlMetric(ControlMetricType.CPU_LOAD,
                    new MetricValue.Builder().load(cpuLoad).add());
            service.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, Optional.ofNullable(null));

            cm = new ControlMetric(ControlMetricType.TOTAL_CPU_TIME,
                    new MetricValue.Builder().load(totalCpuTime).add());
            service.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, Optional.ofNullable(null));

            cm = new ControlMetric(ControlMetricType.SYS_CPU_TIME,
                    new MetricValue.Builder().load(sysCpuTime).add());
            service.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, Optional.ofNullable(null));

            cm = new ControlMetric(ControlMetricType.USER_CPU_TIME,
                    new MetricValue.Builder().load(userCpuTime).add());
            service.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, Optional.ofNullable(null));

            cm = new ControlMetric(ControlMetricType.CPU_IDLE_TIME,
                    new MetricValue.Builder().load(cpuIdleTime).add());
            service.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, Optional.ofNullable(null));

        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return ok(root).build();
    }

    /**
     * Collects memory metrics.
     *
     * @param stream JSON stream
     * @return 200 OK
     * @onos.rsModel MemoryMetricsPost
     */
    @POST
    @Path("memory_metrics")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response memoryMetrics(InputStream stream) {
        ObjectNode root = mapper().createObjectNode();
        ControlMetric cm;
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            long memUsedRatio = nullIsIllegal(jsonTree.get("memoryUsedRatio").asLong(), INVALID_REQUEST);
            long memFreeRatio = nullIsIllegal(jsonTree.get("memoryFreeRatio").asLong(), INVALID_REQUEST);
            long memUsed = nullIsIllegal(jsonTree.get("memoryUsed").asLong(), INVALID_REQUEST);
            long memFree = nullIsIllegal(jsonTree.get("memoryFree").asLong(), INVALID_REQUEST);

            cm = new ControlMetric(ControlMetricType.MEMORY_USED_RATIO,
                    new MetricValue.Builder().load(memUsedRatio).add());
            service.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, Optional.ofNullable(null));

            cm = new ControlMetric(ControlMetricType.MEMORY_FREE_RATIO,
                    new MetricValue.Builder().load(memFreeRatio).add());
            service.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, Optional.ofNullable(null));

            cm = new ControlMetric(ControlMetricType.MEMORY_USED,
                    new MetricValue.Builder().load(memUsed).add());
            service.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, Optional.ofNullable(null));

            cm = new ControlMetric(ControlMetricType.MEMORY_FREE,
                    new MetricValue.Builder().load(memFree).add());
            service.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, Optional.ofNullable(null));

        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return ok(root).build();
    }

    /**
     * Collects disk metrics.
     *
     * @param stream JSON stream
     * @return 200 OK
     * @onos.rsModel DiskMetricsPost
     */
    @POST
    @Path("disk_metrics")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response diskMetrics(InputStream stream) {
        ObjectNode root = mapper().createObjectNode();
        ControlMetric cm;
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            ArrayNode diskRes = (ArrayNode) jsonTree.get("disks");
            for (JsonNode node : diskRes) {
                JsonNode resourceName = node.get("resourceName");
                nullIsIllegal(resourceName, INVALID_RESOURCE_NAME);

                long readBytes = nullIsIllegal(node.get("readBytes").asLong(), INVALID_REQUEST);
                long writeBytes = nullIsIllegal(node.get("writeBytes").asLong(), INVALID_REQUEST);

                cm = new ControlMetric(ControlMetricType.DISK_READ_BYTES,
                        new MetricValue.Builder().load(readBytes).add());
                service.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, resourceName.asText());

                cm = new ControlMetric(ControlMetricType.DISK_WRITE_BYTES,
                        new MetricValue.Builder().load(writeBytes).add());
                service.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, resourceName.asText());
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return ok(root).build();
    }

    /**
     * Collects network metrics.
     *
     * @param stream JSON stream
     * @return 200 OK
     * @onos.rsModel NetworkMetricsPost
     */
    @POST
    @Path("network_metrics")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response networkMetrics(InputStream stream) {
        ObjectNode root = mapper().createObjectNode();
        ControlMetric cm;
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            ArrayNode networkRes = (ArrayNode) jsonTree.get("networks");
            for (JsonNode node : networkRes) {
                JsonNode resourceName = node.get("resourceName");
                nullIsIllegal(resourceName, INVALID_RESOURCE_NAME);

                long inBytes = nullIsIllegal(node.get("incomingBytes").asLong(), INVALID_REQUEST);
                long outBytes = nullIsIllegal(node.get("outgoingBytes").asLong(), INVALID_REQUEST);
                long inPackets = nullIsIllegal(node.get("incomingPackets").asLong(), INVALID_REQUEST);
                long outPackets = nullIsIllegal(node.get("outgoingPackets").asLong(), INVALID_REQUEST);

                cm = new ControlMetric(ControlMetricType.NW_INCOMING_BYTES,
                        new MetricValue.Builder().load(inBytes).add());
                service.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, resourceName.asText());

                cm = new ControlMetric(ControlMetricType.NW_OUTGOING_BYTES,
                        new MetricValue.Builder().load(outBytes).add());
                service.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, resourceName.asText());

                cm = new ControlMetric(ControlMetricType.NW_INCOMING_PACKETS,
                        new MetricValue.Builder().load(inPackets).add());
                service.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, resourceName.asText());

                cm = new ControlMetric(ControlMetricType.NW_OUTGOING_PACKETS,
                        new MetricValue.Builder().load(outPackets).add());
                service.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, resourceName.asText());
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return ok(root).build();
    }

    /**
     * Collects system information.
     * The system information includes the various control metrics
     * which do not require aggregation.
     *
     * @param stream JSON stream
     * @return 200 OK
     * @onos.rsModel SystemInfoPost
     */
    @POST
    @Path("system_info")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response systemInfo(InputStream stream) {
        ObjectNode root = mapper().createObjectNode();

        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode numOfCores = jsonTree.get("numOfCores");
            JsonNode numOfCpus = jsonTree.get("numOfCpus");
            JsonNode cpuSpeed = jsonTree.get("cpuSpeed");
            JsonNode totalMemory = jsonTree.get("totalMemory");

            if (numOfCores != null && numOfCpus != null &&
                cpuSpeed != null && totalMemory != null) {
                SystemInfo systemInfo = new DefaultSystemInfo.Builder()
                        .numOfCores(numOfCores.asInt())
                        .numOfCpus(numOfCpus.asInt())
                        .cpuSpeed(cpuSpeed.asInt())
                        .totalMemory(totalMemory.asInt())
                        .build();

                // try to store the system info.
                SystemInfoFactory.getInstance().setSystemInfo(systemInfo);
            } else {
                throw new IllegalArgumentException(INVALID_SYSTEM_SPECS);
            }

        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return ok(root).build();
    }
}