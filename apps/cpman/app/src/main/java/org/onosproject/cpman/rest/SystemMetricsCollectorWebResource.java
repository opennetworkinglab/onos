/*
 * Copyright 2016-present Open Networking Foundation
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
import org.apache.commons.lang3.StringUtils;
import org.onlab.metrics.MetricsService;
import org.onosproject.cpman.ControlMetric;
import org.onosproject.cpman.ControlMetricType;
import org.onosproject.cpman.ControlPlaneMonitorService;
import org.onosproject.cpman.ControlResource;
import org.onosproject.cpman.MetricValue;
import org.onosproject.cpman.SystemInfo;
import org.onosproject.cpman.impl.DefaultSystemInfo;
import org.onosproject.cpman.impl.SystemInfoFactory;
import org.onosproject.cpman.impl.SystemMetricsAggregator;
import org.onosproject.rest.AbstractWebResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.onlab.util.Tools.nullIsIllegal;
import static org.onlab.util.Tools.readTreeFromStream;

/**
 * Collect system metrics.
 */
@Path("collector")
public class SystemMetricsCollectorWebResource extends AbstractWebResource {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private static final int UPDATE_INTERVAL_IN_MINUTE = 1;
    private static final String INVALID_SYSTEM_SPECS = "Invalid system specifications";
    private static final String INVALID_RESOURCE_NAME = "Invalid resource name";
    private static final String INVALID_REQUEST = "Invalid request";
    private static final int PERCENT_CONSTANT = 100;
    private static final String SYSTEM_TYPE = "system";
    private static final String DISK_TYPE = "disk";
    private static final String NETWORK_TYPE = "network";

    private static final Set<String> MEMORY_FIELD_SET = ControlResource.MEMORY_METRICS
            .stream().map(type -> toCamelCase(type.toString(), true))
            .collect(Collectors.toSet());

    private static final Set<String> CPU_FIELD_SET = ControlResource.CPU_METRICS
            .stream().map(type -> toCamelCase(type.toString(), true))
            .collect(Collectors.toSet());

    private SystemMetricsAggregator aggregator = SystemMetricsAggregator.getInstance();

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
    public Response cpuMetrics(InputStream stream) {
        ObjectNode root = mapper().createObjectNode();
        ControlPlaneMonitorService monitorService = get(ControlPlaneMonitorService.class);
        MetricsService metricsService = get(MetricsService.class);
        ControlMetric cm;
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);

            if (jsonTree == null || !checkFields(jsonTree, CPU_FIELD_SET)) {
                return ok(root).build();
            }

            long cpuLoad = nullIsIllegal((long) (jsonTree.get("cpuLoad").asDouble()
                    * PERCENT_CONSTANT), INVALID_REQUEST);
            long totalCpuTime = nullIsIllegal(jsonTree.get("totalCpuTime").asLong(), INVALID_REQUEST);
            long sysCpuTime = nullIsIllegal(jsonTree.get("sysCpuTime").asLong(), INVALID_REQUEST);
            long userCpuTime = nullIsIllegal(jsonTree.get("userCpuTime").asLong(), INVALID_REQUEST);
            long cpuIdleTime = nullIsIllegal(jsonTree.get("cpuIdleTime").asLong(), INVALID_REQUEST);

            aggregator.setMetricsService(metricsService);
            aggregator.addMetrics(Optional.empty(), SYSTEM_TYPE);

            cm = new ControlMetric(ControlMetricType.CPU_LOAD,
                    new MetricValue.Builder().load(cpuLoad).add());
            monitorService.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, Optional.empty());
            aggregator.increment(ControlMetricType.CPU_LOAD, cpuLoad);

            cm = new ControlMetric(ControlMetricType.TOTAL_CPU_TIME,
                    new MetricValue.Builder().load(totalCpuTime).add());
            monitorService.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, Optional.empty());
            aggregator.increment(ControlMetricType.TOTAL_CPU_TIME, totalCpuTime);

            cm = new ControlMetric(ControlMetricType.SYS_CPU_TIME,
                    new MetricValue.Builder().load(sysCpuTime).add());
            monitorService.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, Optional.empty());
            aggregator.increment(ControlMetricType.SYS_CPU_TIME, sysCpuTime);

            cm = new ControlMetric(ControlMetricType.USER_CPU_TIME,
                    new MetricValue.Builder().load(userCpuTime).add());
            monitorService.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, Optional.empty());
            aggregator.increment(ControlMetricType.USER_CPU_TIME, userCpuTime);

            cm = new ControlMetric(ControlMetricType.CPU_IDLE_TIME,
                    new MetricValue.Builder().load(cpuIdleTime).add());
            monitorService.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, Optional.empty());
            aggregator.increment(ControlMetricType.CPU_IDLE_TIME, cpuIdleTime);

        } catch (IOException e) {
            throw new IllegalArgumentException(e);
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
    public Response memoryMetrics(InputStream stream) {
        ObjectNode root = mapper().createObjectNode();
        ControlPlaneMonitorService monitorService = get(ControlPlaneMonitorService.class);
        MetricsService metricsService = get(MetricsService.class);
        ControlMetric cm;
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);

            if (jsonTree == null || !checkFields(jsonTree, MEMORY_FIELD_SET)) {
                return ok(root).build();
            }

            long memUsed = nullIsIllegal(jsonTree.get("memoryUsed").asLong(), INVALID_REQUEST);
            long memFree = nullIsIllegal(jsonTree.get("memoryFree").asLong(), INVALID_REQUEST);
            long memTotal = memUsed + memFree;
            long memUsedRatio = memTotal == 0L ? 0L : (memUsed * PERCENT_CONSTANT) / memTotal;
            long memFreeRatio = memTotal == 0L ? 0L : (memFree * PERCENT_CONSTANT) / memTotal;

            aggregator.setMetricsService(metricsService);
            aggregator.addMetrics(Optional.empty(), SYSTEM_TYPE);

            cm = new ControlMetric(ControlMetricType.MEMORY_USED_RATIO,
                    new MetricValue.Builder().load(memUsedRatio).add());
            monitorService.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, Optional.empty());
            aggregator.increment(ControlMetricType.MEMORY_USED_RATIO, memUsedRatio);

            cm = new ControlMetric(ControlMetricType.MEMORY_FREE_RATIO,
                    new MetricValue.Builder().load(memFreeRatio).add());
            monitorService.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, Optional.empty());
            aggregator.increment(ControlMetricType.MEMORY_FREE_RATIO, memFreeRatio);

            cm = new ControlMetric(ControlMetricType.MEMORY_USED,
                    new MetricValue.Builder().load(memUsed).add());
            monitorService.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, Optional.empty());
            aggregator.increment(ControlMetricType.MEMORY_USED, memUsed);

            cm = new ControlMetric(ControlMetricType.MEMORY_FREE,
                    new MetricValue.Builder().load(memFree).add());
            monitorService.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, Optional.empty());
            aggregator.increment(ControlMetricType.MEMORY_FREE, memFree);

        } catch (IOException e) {
            throw new IllegalArgumentException(e);
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
    public Response diskMetrics(InputStream stream) {
        ObjectNode root = mapper().createObjectNode();
        ControlPlaneMonitorService monitorService = get(ControlPlaneMonitorService.class);
        MetricsService metricsService = get(MetricsService.class);
        ControlMetric cm;
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
            ArrayNode diskRes =
                    jsonTree.get("disks") == null ?
                            mapper().createArrayNode() : (ArrayNode) jsonTree.get("disks");

            for (JsonNode node : diskRes) {
                JsonNode resourceName = node.get("resourceName");
                nullIsIllegal(resourceName, INVALID_RESOURCE_NAME);

                aggregator.setMetricsService(metricsService);
                aggregator.addMetrics(Optional.of(resourceName.asText()), DISK_TYPE);

                long readBytes = nullIsIllegal(node.get("readBytes").asLong(), INVALID_REQUEST);
                long writeBytes = nullIsIllegal(node.get("writeBytes").asLong(), INVALID_REQUEST);

                cm = new ControlMetric(ControlMetricType.DISK_READ_BYTES,
                        new MetricValue.Builder().load(readBytes).add());
                monitorService.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, resourceName.asText());
                aggregator.increment(resourceName.asText(), DISK_TYPE,
                        ControlMetricType.DISK_READ_BYTES, readBytes);
                cm = new ControlMetric(ControlMetricType.DISK_WRITE_BYTES,
                        new MetricValue.Builder().load(writeBytes).add());
                monitorService.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, resourceName.asText());
                aggregator.increment(resourceName.asText(), DISK_TYPE,
                        ControlMetricType.DISK_WRITE_BYTES, writeBytes);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
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
    public Response networkMetrics(InputStream stream) {
        ObjectNode root = mapper().createObjectNode();
        ControlPlaneMonitorService monitorService = get(ControlPlaneMonitorService.class);
        MetricsService metricsService = get(MetricsService.class);
        ControlMetric cm;
        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);

            ArrayNode networkRes = jsonTree.get("networks") == null
                    ? mapper().createArrayNode() : (ArrayNode) jsonTree.get("networks");

            for (JsonNode node : networkRes) {
                JsonNode resourceName = node.get("resourceName");
                nullIsIllegal(resourceName, INVALID_RESOURCE_NAME);

                aggregator.setMetricsService(metricsService);
                aggregator.addMetrics(Optional.of(resourceName.asText()), NETWORK_TYPE);

                long inBytes = nullIsIllegal(node.get("incomingBytes").asLong(), INVALID_REQUEST);
                long outBytes = nullIsIllegal(node.get("outgoingBytes").asLong(), INVALID_REQUEST);
                long inPackets = nullIsIllegal(node.get("incomingPackets").asLong(), INVALID_REQUEST);
                long outPackets = nullIsIllegal(node.get("outgoingPackets").asLong(), INVALID_REQUEST);

                cm = new ControlMetric(ControlMetricType.NW_INCOMING_BYTES,
                        new MetricValue.Builder().load(inBytes).add());
                monitorService.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, resourceName.asText());
                aggregator.increment(resourceName.asText(), NETWORK_TYPE,
                        ControlMetricType.NW_INCOMING_BYTES, inBytes);

                cm = new ControlMetric(ControlMetricType.NW_OUTGOING_BYTES,
                        new MetricValue.Builder().load(outBytes).add());
                monitorService.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, resourceName.asText());
                aggregator.increment(resourceName.asText(), NETWORK_TYPE,
                        ControlMetricType.NW_OUTGOING_BYTES, outBytes);

                cm = new ControlMetric(ControlMetricType.NW_INCOMING_PACKETS,
                        new MetricValue.Builder().load(inPackets).add());
                monitorService.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, resourceName.asText());
                aggregator.increment(resourceName.asText(), NETWORK_TYPE,
                        ControlMetricType.NW_INCOMING_PACKETS, inPackets);

                cm = new ControlMetric(ControlMetricType.NW_OUTGOING_PACKETS,
                        new MetricValue.Builder().load(outPackets).add());
                monitorService.updateMetric(cm, UPDATE_INTERVAL_IN_MINUTE, resourceName.asText());
                aggregator.increment(resourceName.asText(), NETWORK_TYPE,
                        ControlMetricType.NW_OUTGOING_PACKETS, outPackets);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException(e);
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
    public Response systemInfo(InputStream stream) {
        ObjectNode root = mapper().createObjectNode();

        try {
            ObjectNode jsonTree = readTreeFromStream(mapper(), stream);
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
            throw new IllegalArgumentException(e);
        }
        return ok(root).build();
    }

    private boolean checkFields(ObjectNode node, Set<String> original) {
        Iterator<String> fieldNames = node.fieldNames();
        while (fieldNames.hasNext()) {
            String fieldName = fieldNames.next();
            if (!original.contains(fieldName) || node.get(fieldName) == null) {
                log.warn("Illegal field name: {}", fieldName);
                return false;
            }
        }
        return true;
    }

    private static String toCamelCase(String value, boolean startWithLowerCase) {
        String[] strings = StringUtils.split(value.toLowerCase(), "_");
        for (int i = startWithLowerCase ? 1 : 0; i < strings.length; i++) {
            strings[i] = StringUtils.capitalize(strings[i]);
        }
        return StringUtils.join(strings);
    }
}
