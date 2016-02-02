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
import org.onosproject.cpman.impl.ControlMetricsSystemSpec;
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
    public static final int UPDATE_INTERVAL = 1;           // 1 minute update interval
    public static final String INVALID_SYSTEM_SPECS = "Invalid system specifications";
    public static final String INVALID_RESOURCE_NAME = "Invalid resource name";

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
            JsonNode cpuLoadJson = jsonTree.get("cpuLoad");
            JsonNode totalCpuTimeJson = jsonTree.get("totalCpuTime");
            JsonNode sysCpuTimeJson = jsonTree.get("sysCpuTime");
            JsonNode userCpuTimeJson = jsonTree.get("userCpuTime");
            JsonNode cpuIdleTimeJson = jsonTree.get("cpuIdleTime");

            if (cpuLoadJson != null) {
                cm = new ControlMetric(ControlMetricType.CPU_LOAD,
                        new MetricValue.Builder().load(cpuLoadJson.asLong()).add());
                service.updateMetric(cm, UPDATE_INTERVAL, Optional.ofNullable(null));
            }

            if (totalCpuTimeJson != null) {
                cm = new ControlMetric(ControlMetricType.TOTAL_CPU_TIME,
                        new MetricValue.Builder().load(totalCpuTimeJson.asLong()).add());
                service.updateMetric(cm, UPDATE_INTERVAL, Optional.ofNullable(null));
            }

            if (sysCpuTimeJson != null) {
                cm = new ControlMetric(ControlMetricType.SYS_CPU_TIME,
                        new MetricValue.Builder().load(sysCpuTimeJson.asLong()).add());
                service.updateMetric(cm, UPDATE_INTERVAL, Optional.ofNullable(null));
            }

            if (userCpuTimeJson != null) {
                cm = new ControlMetric(ControlMetricType.USER_CPU_TIME,
                        new MetricValue.Builder().load(userCpuTimeJson.asLong()).add());
                service.updateMetric(cm, UPDATE_INTERVAL, Optional.ofNullable(null));
            }

            if (cpuIdleTimeJson != null) {
                cm = new ControlMetric(ControlMetricType.CPU_IDLE_TIME,
                        new MetricValue.Builder().load(cpuIdleTimeJson.asLong()).add());
                service.updateMetric(cm, UPDATE_INTERVAL, Optional.ofNullable(null));
            }

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
            JsonNode memUsedRatio = jsonTree.get("memoryUsedRatio");
            JsonNode memFreeRatio = jsonTree.get("memoryFreeRatio");
            JsonNode memUsed = jsonTree.get("memoryUsed");
            JsonNode memFree = jsonTree.get("memoryFree");

            if (memUsedRatio != null) {
                cm = new ControlMetric(ControlMetricType.MEMORY_USED_RATIO,
                        new MetricValue.Builder().load(memUsedRatio.asLong()).add());
                service.updateMetric(cm, UPDATE_INTERVAL, Optional.ofNullable(null));
            }

            if (memFreeRatio != null) {
                cm = new ControlMetric(ControlMetricType.MEMORY_FREE_RATIO,
                        new MetricValue.Builder().load(memFreeRatio.asLong()).add());
                service.updateMetric(cm, UPDATE_INTERVAL, Optional.ofNullable(null));
            }

            if (memUsed != null) {
                cm = new ControlMetric(ControlMetricType.MEMORY_USED,
                        new MetricValue.Builder().load(memUsed.asLong()).add());
                service.updateMetric(cm, UPDATE_INTERVAL, Optional.ofNullable(null));
            }

            if (memFree != null) {
                cm = new ControlMetric(ControlMetricType.MEMORY_FREE,
                        new MetricValue.Builder().load(memFree.asLong()).add());
                service.updateMetric(cm, UPDATE_INTERVAL, Optional.ofNullable(null));
            }

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
        final ControlMetric[] cm = new ControlMetric[1];
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            ArrayNode diskRes = (ArrayNode) jsonTree.get("disks");
            diskRes.forEach(node-> {
                JsonNode resourceName = node.get("resourceName");
                nullIsIllegal(resourceName, INVALID_RESOURCE_NAME);

                JsonNode readBytes = jsonTree.get("readBytes");
                JsonNode writeBytes = jsonTree.get("writeBytes");

                if (readBytes != null) {
                    cm[0] = new ControlMetric(ControlMetricType.DISK_READ_BYTES,
                            new MetricValue.Builder().load(readBytes.asLong()).add());
                    service.updateMetric(cm[0], UPDATE_INTERVAL, resourceName.asText());
                }

                if (writeBytes != null) {
                    cm[0] = new ControlMetric(ControlMetricType.DISK_WRITE_BYTES,
                            new MetricValue.Builder().load(writeBytes.asLong()).add());
                    service.updateMetric(cm[0], UPDATE_INTERVAL, resourceName.asText());
                }
            });
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
        final ControlMetric[] cm = new ControlMetric[1];
        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            ArrayNode networkRes = (ArrayNode) jsonTree.get("networks");
            networkRes.forEach(node -> {
                JsonNode resourceName = node.get("resourceName");
                nullIsIllegal(resourceName, INVALID_RESOURCE_NAME);

                JsonNode inBytes = jsonTree.get("incomingBytes");
                JsonNode outBytes = jsonTree.get("outgoingBytes");
                JsonNode inPackets = jsonTree.get("incomingPackets");
                JsonNode outPackets = jsonTree.get("outgoingPackets");

                if (inBytes != null) {
                    cm[0] = new ControlMetric(ControlMetricType.NW_INCOMING_BYTES,
                            new MetricValue.Builder().load(inBytes.asLong()).add());
                    service.updateMetric(cm[0], UPDATE_INTERVAL, resourceName.asText());
                }

                if (outBytes != null) {
                    cm[0] = new ControlMetric(ControlMetricType.NW_OUTGOING_BYTES,
                            new MetricValue.Builder().load(outBytes.asLong()).add());
                    service.updateMetric(cm[0], UPDATE_INTERVAL, resourceName.asText());
                }

                if (inPackets != null) {
                    cm[0] = new ControlMetric(ControlMetricType.NW_INCOMING_PACKETS,
                            new MetricValue.Builder().load(inPackets.asLong()).add());
                    service.updateMetric(cm[0], UPDATE_INTERVAL, resourceName.asText());
                }

                if (outPackets != null) {
                    cm[0] = new ControlMetric(ControlMetricType.NW_OUTGOING_PACKETS,
                            new MetricValue.Builder().load(outPackets.asLong()).add());
                    service.updateMetric(cm[0], UPDATE_INTERVAL, resourceName.asText());
                }
            });
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return ok(root).build();
    }

    /**
     * Collects system specifications.
     * The system specs include the various control metrics
     * which do not require aggregation.
     *
     * @param stream JSON stream
     * @return 200 OK
     * @onos.rsModel SystemSpecsPost
     */
    @POST
    @Path("system_specs")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response systemSpecs(InputStream stream) {
        ObjectNode root = mapper().createObjectNode();

        try {
            ObjectNode jsonTree = (ObjectNode) mapper().readTree(stream);
            JsonNode numOfCores = jsonTree.get("numOfCores");
            JsonNode numOfCpus = jsonTree.get("numOfCpus");
            JsonNode cpuSpeed = jsonTree.get("cpuSpeed");
            JsonNode totalMemory = jsonTree.get("totalMemory");

            if (numOfCores != null && numOfCpus != null && cpuSpeed != null && totalMemory != null) {
                ControlMetricsSystemSpec.Builder builder = new ControlMetricsSystemSpec.Builder();
                ControlMetricsSystemSpec cmss = builder.numOfCores(numOfCores.asInt())
                        .numOfCpus(numOfCpus.asInt())
                        .cpuSpeed(cpuSpeed.asInt())
                        .totalMemory(totalMemory.asLong())
                        .build();
                // TODO: need to implement spec store

            } else {
                throw new IllegalArgumentException(INVALID_SYSTEM_SPECS);
            }

        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return ok(root).build();
    }
}