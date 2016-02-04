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
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
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
import java.util.stream.Collectors;

import static org.onosproject.cpman.ControlResource.*;

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

    private static final Set RESOURCE_TYPE_SET =
            ImmutableSet.of(Type.CONTROL_MESSAGE, Type.DISK, Type.NETWORK);

    private Map<ControlMetricType, Double> cpuBuf;
    private Map<ControlMetricType, Double> memoryBuf;
    private Map<String, Map<ControlMetricType, Double>> diskBuf;
    private Map<String, Map<ControlMetricType, Double>> networkBuf;
    private Map<DeviceId, Map<ControlMetricType, Double>> ctrlMsgBuf;

    private Map<Type, Set<String>> availableResourceMap;
    private Set<DeviceId> availableDeviceIdSet;

    @Activate
    public void activate() {
        cpuMetrics = genMDbBuilder(Type.CPU, CPU_METRICS);
        memoryMetrics = genMDbBuilder(Type.MEMORY, MEMORY_METRICS);
        controlMessageMap = Maps.newConcurrentMap();
        diskMetricsMap = Maps.newConcurrentMap();
        networkMetricsMap = Maps.newConcurrentMap();

        cpuBuf = Maps.newConcurrentMap();
        memoryBuf = Maps.newConcurrentMap();
        diskBuf = Maps.newConcurrentMap();
        networkBuf = Maps.newConcurrentMap();
        ctrlMsgBuf = Maps.newConcurrentMap();

        availableResourceMap = Maps.newConcurrentMap();
        availableDeviceIdSet = Sets.newConcurrentHashSet();

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
            ctrlMsgBuf.putIfAbsent(deviceId.get(), Maps.newConcurrentMap());

            // update control message metrics
            if (CONTROL_MESSAGE_METRICS.contains(cm.metricType())) {

                if (!availableDeviceIdSet.contains(deviceId.get())) {
                    availableDeviceIdSet.add(deviceId.get());
                }

                // we will accumulate the metric value into buffer first
                ctrlMsgBuf.get(deviceId.get()).putIfAbsent(cm.metricType(),
                        (double) cm.metricValue().getLoad());

                // if buffer contains all control message metrics,
                // we simply set and update the values into MetricsDatabase.
                if (ctrlMsgBuf.get(deviceId.get()).keySet()
                        .containsAll(CONTROL_MESSAGE_METRICS)) {
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
            diskBuf.putIfAbsent(resourceName, Maps.newConcurrentMap());

            availableResourceMap.putIfAbsent(Type.DISK, Sets.newHashSet());
            availableResourceMap.computeIfPresent(Type.DISK, (k, v) -> {
                v.add(resourceName);
                return v;
            });

            diskBuf.get(resourceName).putIfAbsent(cm.metricType(),
                    (double) cm.metricValue().getLoad());
            if (diskBuf.get(resourceName).keySet().containsAll(DISK_METRICS)) {
                updateDiskMetrics(diskBuf.get(resourceName), resourceName);
                diskBuf.clear();
            }
        }

        // update network metrics
        if (NETWORK_METRICS.contains(cm.metricType())) {
            networkBuf.putIfAbsent(resourceName, Maps.newConcurrentMap());

            availableResourceMap.putIfAbsent(Type.NETWORK, Sets.newHashSet());
            availableResourceMap.computeIfPresent(Type.NETWORK, (k, v) -> {
                v.add(resourceName);
                return v;
            });

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
                if (CONTROL_MESSAGE_METRICS.contains(type) &&
                        availableDeviceIdSet.contains(deviceId.get())) {
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
            if (DISK_METRICS.contains(type) &&
                    availableResources(Type.DISK).contains(resourceName)) {
                return new DefaultControlLoad(diskMetricsMap.get(resourceName), type);
            }

            if (NETWORK_METRICS.contains(type) &&
                    availableResources(Type.NETWORK).contains(resourceName)) {
                return new DefaultControlLoad(networkMetricsMap.get(resourceName), type);
            }
        } else {
            // TODO: currently only query the metrics of local node
            return null;
        }
        return null;
    }

    @Override
    public Set<String> availableResources(Type resourceType) {
        if (RESOURCE_TYPE_SET.contains(resourceType)) {
            if (Type.CONTROL_MESSAGE.equals(resourceType)) {
                return availableDeviceIdSet.stream().map(id ->
                        id.toString()).collect(Collectors.toSet());
            } else {
                return availableResourceMap.get(resourceType);
            }
        }
        return null;
    }

    private MetricsDatabase genMDbBuilder(Type resourceType,
                                          Set<ControlMetricType> metricTypes) {
        MetricsDatabase.Builder builder = new DefaultMetricsDatabase.Builder();
        builder.withMetricName(resourceType.toString());
        metricTypes.forEach(type -> builder.addMetricType(type.toString()));
        return builder.build();
    }

    private void updateNetworkMetrics(Map<ControlMetricType, Double> metricMap,
                                      String resName) {
        networkMetricsMap.putIfAbsent(resName,
                genMDbBuilder(Type.NETWORK, NETWORK_METRICS));
        networkMetricsMap.get(resName).updateMetrics(convertMap(metricMap));
    }

    private void updateDiskMetrics(Map<ControlMetricType, Double> metricMap,
                                   String resName) {
        diskMetricsMap.putIfAbsent(resName, genMDbBuilder(Type.DISK, DISK_METRICS));
        diskMetricsMap.get(resName).updateMetrics(convertMap(metricMap));
    }

    private void updateControlMessages(Map<ControlMetricType, Double> metricMap,
                                       DeviceId devId) {
        controlMessageMap.putIfAbsent(devId,
                genMDbBuilder(Type.CONTROL_MESSAGE, CONTROL_MESSAGE_METRICS));
        controlMessageMap.get(devId).updateMetrics(convertMap(metricMap));
    }

    private Map convertMap(Map<ControlMetricType, Double> map) {
        Map newMap = Maps.newConcurrentMap();
        map.forEach((k, v) -> newMap.putIfAbsent(k.toString(), v));
        return newMap;
    }
}