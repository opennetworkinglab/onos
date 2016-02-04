/*
 * Copyright 2015-2016 Open Networking Laboratory
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
package org.onosproject.cpman.impl;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.cpman.ControlLoad;
import org.onosproject.cpman.ControlMetric;
import org.onosproject.cpman.ControlMetricType;
import org.onosproject.cpman.ControlPlaneMonitorService;
import org.onosproject.cpman.MetricsDatabase;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.onosproject.cpman.ControlMetricType.CPU_IDLE_TIME;
import static org.onosproject.cpman.ControlMetricType.CPU_LOAD;
import static org.onosproject.cpman.ControlMetricType.DISK_READ_BYTES;
import static org.onosproject.cpman.ControlMetricType.DISK_WRITE_BYTES;
import static org.onosproject.cpman.ControlMetricType.FLOW_MOD_PACKET;
import static org.onosproject.cpman.ControlMetricType.FLOW_REMOVED_PACKET;
import static org.onosproject.cpman.ControlMetricType.INBOUND_PACKET;
import static org.onosproject.cpman.ControlMetricType.MEMORY_FREE;
import static org.onosproject.cpman.ControlMetricType.MEMORY_FREE_RATIO;
import static org.onosproject.cpman.ControlMetricType.MEMORY_USED;
import static org.onosproject.cpman.ControlMetricType.MEMORY_USED_RATIO;
import static org.onosproject.cpman.ControlMetricType.NW_INCOMING_BYTES;
import static org.onosproject.cpman.ControlMetricType.NW_INCOMING_PACKETS;
import static org.onosproject.cpman.ControlMetricType.NW_OUTGOING_BYTES;
import static org.onosproject.cpman.ControlMetricType.NW_OUTGOING_PACKETS;
import static org.onosproject.cpman.ControlMetricType.OUTBOUND_PACKET;
import static org.onosproject.cpman.ControlMetricType.REPLY_PACKET;
import static org.onosproject.cpman.ControlMetricType.REQUEST_PACKET;
import static org.onosproject.cpman.ControlMetricType.SYS_CPU_TIME;
import static org.onosproject.cpman.ControlMetricType.TOTAL_CPU_TIME;
import static org.onosproject.cpman.ControlMetricType.USER_CPU_TIME;

/**
 * Control plane monitoring service class.
 */
@Component(immediate = true)
@Service
public class ControlPlaneMonitor implements ControlPlaneMonitorService {

    private final Logger log = LoggerFactory.getLogger(getClass());
    private MetricsDatabase cpuMetrics;
    private MetricsDatabase memoryMetrics;
    private Map<DeviceId, MetricsDatabase> controlMessageMap;
    private Map<String, MetricsDatabase> diskMetricsMap;
    private Map<String, MetricsDatabase> networkMetricsMap;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    private static final String CPU = "Cpu";
    private static final String MEMORY = "Memory";
    private static final String CTRL_MSG = "ControlMessage";
    private static final String DISK = "Disk";
    private static final String NETWORK = "Network";

    private static final Set<ControlMetricType> CPU_METRICS =
            ImmutableSet.of(CPU_IDLE_TIME, CPU_LOAD, SYS_CPU_TIME,
                    USER_CPU_TIME, TOTAL_CPU_TIME);
    private static final Set<ControlMetricType> MEMORY_METRICS =
            ImmutableSet.of(MEMORY_FREE, MEMORY_FREE_RATIO, MEMORY_USED,
                    MEMORY_USED_RATIO);
    private static final Set<ControlMetricType> DISK_METRICS =
            ImmutableSet.of(DISK_READ_BYTES, DISK_WRITE_BYTES);
    private static final Set<ControlMetricType> NETWORK_METRICS =
            ImmutableSet.of(NW_INCOMING_BYTES, NW_OUTGOING_BYTES,
                    NW_INCOMING_PACKETS, NW_OUTGOING_PACKETS);
    private static final Set<ControlMetricType> CTRL_MSGS =
            ImmutableSet.of(INBOUND_PACKET, OUTBOUND_PACKET, FLOW_MOD_PACKET,
                    FLOW_REMOVED_PACKET, REQUEST_PACKET, REPLY_PACKET);
    private Map<ControlMetricType, Double> cpuBuf;
    private Map<ControlMetricType, Double> memoryBuf;
    private Map<String, Map<ControlMetricType, Double>> diskBuf;
    private Map<String, Map<ControlMetricType, Double>> networkBuf;
    private Map<DeviceId, Map<ControlMetricType, Double>> ctrlMsgBuf;

    @Activate
    public void activate() {
        cpuMetrics = genMDbBuilder(CPU, CPU_METRICS);
        memoryMetrics = genMDbBuilder(MEMORY, MEMORY_METRICS);
        controlMessageMap = new ConcurrentHashMap<>();
        diskMetricsMap = new ConcurrentHashMap<>();
        networkMetricsMap = new ConcurrentHashMap<>();

        cpuBuf = new ConcurrentHashMap<>();
        memoryBuf = new ConcurrentHashMap<>();
        diskBuf = new ConcurrentHashMap<>();
        networkBuf = new ConcurrentHashMap<>();
        ctrlMsgBuf = new ConcurrentHashMap<>();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {

        // TODO: need to handle the mdb close.
        cpuBuf.clear();
        memoryBuf.clear();
        diskBuf.clear();
        networkBuf.clear();
        ctrlMsgBuf.clear();

        log.info("Stopped");
    }

    @Override
    public void updateMetric(ControlMetric cm, int updateIntervalInMinutes,
                             Optional<DeviceId> deviceId) {
        if (deviceId.isPresent()) {

            // insert a new device entry if we cannot find any
            ctrlMsgBuf.putIfAbsent(deviceId.get(), new ConcurrentHashMap<>());

            // update control message metrics
            if (CTRL_MSGS.contains(cm.metricType())) {

                // we will accumulate the metric value into buffer first
                ctrlMsgBuf.get(deviceId.get()).putIfAbsent(cm.metricType(),
                        (double) cm.metricValue().getLoad());

                // if buffer contains all control message metrics,
                // we simply set and update the values into MetricsDatabase.
                if (ctrlMsgBuf.get(deviceId.get()).keySet().containsAll(CTRL_MSGS)) {
                    updateControlMessages(ctrlMsgBuf.get(deviceId.get()), deviceId.get());
                    ctrlMsgBuf.get(deviceId.get()).clear();
                }
            }
        } else {

            // update cpu metrics
            if (CPU_METRICS.contains(cm.metricType())) {
                cpuBuf.putIfAbsent(cm.metricType(),
                        (double) cm.metricValue().getLoad());
                if (cpuBuf.keySet().containsAll(CPU_METRICS)) {
                    cpuMetrics.updateMetrics(convertMap(cpuBuf));
                    cpuBuf.clear();
                }
            }

            // update memory metrics
            if (MEMORY_METRICS.contains(cm.metricType())) {
                memoryBuf.putIfAbsent(cm.metricType(),
                        (double) cm.metricValue().getLoad());
                if (memoryBuf.keySet().containsAll(MEMORY_METRICS)) {
                    memoryMetrics.updateMetrics(convertMap(memoryBuf));
                    memoryBuf.clear();
                }
            }
        }
    }

    @Override
    public void updateMetric(ControlMetric cm, int updateIntervalInMinutes,
                             String resourceName) {
        // update disk metrics
        if (DISK_METRICS.contains(cm.metricType())) {
            diskBuf.putIfAbsent(resourceName, new ConcurrentHashMap<>());
            diskBuf.get(resourceName).putIfAbsent(cm.metricType(),
                    (double) cm.metricValue().getLoad());
            if (diskBuf.get(resourceName).keySet().containsAll(DISK_METRICS)) {
                updateDiskMetrics(diskBuf.get(resourceName), resourceName);
                diskBuf.clear();
            }
        }

        // update network metrics
        if (NETWORK_METRICS.contains(cm.metricType())) {
            networkBuf.putIfAbsent(resourceName, new ConcurrentHashMap<>());
            networkBuf.get(resourceName).putIfAbsent(cm.metricType(),
                    (double) cm.metricValue().getLoad());
            if (networkBuf.get(resourceName).keySet().containsAll(NETWORK_METRICS)) {
                updateNetworkMetrics(networkBuf.get(resourceName), resourceName);
                networkBuf.clear();
            }
        }
    }

    @Override
    public ControlLoad getLoad(NodeId nodeId, ControlMetricType type,
                               Optional<DeviceId> deviceId) {
        ControllerNode node = clusterService.getNode(nodeId);
        if (clusterService.getLocalNode().equals(node)) {

            if (deviceId.isPresent()) {
                if (CTRL_MSGS.contains(type)) {
                    return new DefaultControlLoad(controlMessageMap.get(deviceId.get()), type);
                }
            } else {
                // returns controlLoad of CPU metrics
                if (CPU_METRICS.contains(type)) {
                    return new DefaultControlLoad(cpuMetrics, type);
                }

                // returns memoryLoad of memory metrics
                if (MEMORY_METRICS.contains(type)) {
                    return new DefaultControlLoad(memoryMetrics, type);
                }
            }
        } else {
            // TODO: currently only query the metrics of local node
            return null;
        }
        return null;
    }

    @Override
    public ControlLoad getLoad(NodeId nodeId, ControlMetricType type,
                               String resourceName) {
        if (clusterService.getLocalNode().id().equals(nodeId)) {
            if (DISK_METRICS.contains(type)) {
                return new DefaultControlLoad(diskMetricsMap.get(resourceName), type);
            }

            if (NETWORK_METRICS.contains(type)) {
                return new DefaultControlLoad(networkMetricsMap.get(resourceName), type);
            }
        } else {
            // TODO: currently only query the metrics of local node
            return null;
        }
        return null;
    }

    private MetricsDatabase genMDbBuilder(String metricName,
                                          Set<ControlMetricType> metricTypes) {
        MetricsDatabase.Builder builder = new DefaultMetricsDatabase.Builder();
        builder.withMetricName(metricName);
        metricTypes.forEach(type -> builder.addMetricType(type.toString()));
        return builder.build();
    }

    private void updateNetworkMetrics(Map<ControlMetricType, Double> metricMap,
                                      String resName) {
        networkMetricsMap.putIfAbsent(resName, genMDbBuilder(NETWORK, NETWORK_METRICS));
        networkMetricsMap.get(resName).updateMetrics(convertMap(metricMap));
    }

    private void updateDiskMetrics(Map<ControlMetricType, Double> metricMap,
                                   String resName) {
        diskMetricsMap.putIfAbsent(resName, genMDbBuilder(DISK, DISK_METRICS));
        diskMetricsMap.get(resName).updateMetrics(convertMap(metricMap));
    }

    private void updateControlMessages(Map<ControlMetricType, Double> metricMap,
                                       DeviceId devId) {
        controlMessageMap.putIfAbsent(devId, genMDbBuilder(CTRL_MSG, CTRL_MSGS));
        controlMessageMap.get(devId).updateMetrics(convertMap(metricMap));
    }

    private Map convertMap(Map<ControlMetricType, Double> map) {
        Map newMap = new ConcurrentHashMap<>();
        map.forEach((k, v) -> newMap.putIfAbsent(k.toString(), v));
        return newMap;
    }
}