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
package org.onosproject.cpman.impl;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.KryoNamespace;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.NodeId;
import org.onosproject.cpman.ControlLoad;
import org.onosproject.cpman.ControlLoadSnapshot;
import org.onosproject.cpman.ControlMetric;
import org.onosproject.cpman.ControlMetricType;
import org.onosproject.cpman.ControlMetricsRequest;
import org.onosproject.cpman.ControlPlaneMonitorService;
import org.onosproject.cpman.ControlResource;
import org.onosproject.cpman.ControlResourceRequest;
import org.onosproject.cpman.MetricsDatabase;
import org.onosproject.net.DeviceId;
import org.onosproject.store.cluster.messaging.ClusterCommunicationService;
import org.onosproject.store.cluster.messaging.MessageSubject;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;
import static org.onosproject.cpman.ControlResource.CONTROL_MESSAGE_METRICS;
import static org.onosproject.cpman.ControlResource.CPU_METRICS;
import static org.onosproject.cpman.ControlResource.DISK_METRICS;
import static org.onosproject.cpman.ControlResource.MEMORY_METRICS;
import static org.onosproject.cpman.ControlResource.NETWORK_METRICS;
import static org.onosproject.cpman.ControlResource.Type;

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterCommunicationService communicationService;

    private static final String DEFAULT_RESOURCE = "default";

    private static final Set RESOURCE_TYPE_SET =
            ImmutableSet.of(Type.CONTROL_MESSAGE, Type.DISK, Type.NETWORK);

    private static final MessageSubject CONTROL_STATS =
            new MessageSubject("control-plane-stats");

    private static final MessageSubject CONTROL_RESOURCE =
            new MessageSubject("control-plane-resources");

    private Map<ControlMetricType, Double> cpuBuf;
    private Map<ControlMetricType, Double> memoryBuf;
    private Map<String, Map<ControlMetricType, Double>> diskBuf;
    private Map<String, Map<ControlMetricType, Double>> networkBuf;
    private Map<DeviceId, Map<ControlMetricType, Double>> ctrlMsgBuf;

    private Map<Type, Set<String>> availableResourceMap;
    private Set<DeviceId> availableDeviceIdSet;

    private static final String METRIC_TYPE_NULL = "Control metric type cannot be null";
    private static final String RESOURCE_TYPE_NULL = "Control resource type cannot be null";

    private static final Serializer SERIALIZER = Serializer
            .using(new KryoNamespace.Builder()
                    .register(KryoNamespaces.API)
                    .register(ControlMetricsRequest.class)
                    .register(ControlResourceRequest.class)
                    .register(ControlLoadSnapshot.class)
                    .register(ControlMetricType.class)
                    .register(ControlResource.Type.class)
                    .register(TimeUnit.class)
                    .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID).build());

    @Activate
    public void activate() {
        cpuMetrics = genMDbBuilder(DEFAULT_RESOURCE, Type.CPU, CPU_METRICS);
        memoryMetrics = genMDbBuilder(DEFAULT_RESOURCE, Type.MEMORY, MEMORY_METRICS);
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

        communicationService.<ControlMetricsRequest, ControlLoadSnapshot>addSubscriber(CONTROL_STATS,
                SERIALIZER::decode, this::handleMetricsRequest, SERIALIZER::encode);

        communicationService.<ControlResourceRequest, Set<String>>addSubscriber(CONTROL_RESOURCE,
                SERIALIZER::decode, this::handleResourceRequest, SERIALIZER::encode);

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

        communicationService.removeSubscriber(CONTROL_STATS);
        communicationService.removeSubscriber(CONTROL_RESOURCE);

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
                    ctrlMsgBuf.clear();
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
    public CompletableFuture<ControlLoadSnapshot> getLoad(NodeId nodeId,
                                                          ControlMetricType type,
                                                          Optional<DeviceId> deviceId) {
        if (clusterService.getLocalNode().id().equals(nodeId)) {
            return CompletableFuture.completedFuture(snapshot(getLocalLoad(type, deviceId)));
        } else {
            return communicationService.sendAndReceive(createMetricsRequest(type, deviceId),
                    CONTROL_STATS, SERIALIZER::encode, SERIALIZER::decode, nodeId);
        }
    }

    @Override
    public CompletableFuture<ControlLoadSnapshot> getLoad(NodeId nodeId,
                                                          ControlMetricType type,
                                                          String resourceName) {
        if (clusterService.getLocalNode().id().equals(nodeId)) {
            return CompletableFuture.completedFuture(snapshot(getLocalLoad(type, resourceName)));
        } else {
            return communicationService.sendAndReceive(createMetricsRequest(type, resourceName),
                    CONTROL_STATS, SERIALIZER::encode, SERIALIZER::decode, nodeId);
        }
    }

    @Override
    public CompletableFuture<ControlLoadSnapshot> getLoad(NodeId nodeId,
                                                          ControlMetricType type,
                                                          int duration, TimeUnit unit,
                                                          Optional<DeviceId> deviceId) {
        if (clusterService.getLocalNode().id().equals(nodeId)) {
            return CompletableFuture.completedFuture(snapshot(getLocalLoad(type, deviceId), duration, unit));
        } else {
            return communicationService.sendAndReceive(createMetricsRequest(type, duration, unit, deviceId),
                    CONTROL_STATS, SERIALIZER::encode, SERIALIZER::decode, nodeId);
        }
    }

    @Override
    public CompletableFuture<ControlLoadSnapshot> getLoad(NodeId nodeId,
                                                          ControlMetricType type,
                                                          int duration, TimeUnit unit,
                                                          String resourceName) {
        if (clusterService.getLocalNode().id().equals(nodeId)) {
            return CompletableFuture.completedFuture(snapshot(getLocalLoad(type, resourceName), duration, unit));
        } else {
            return communicationService.sendAndReceive(createMetricsRequest(type, duration, unit, resourceName),
                    CONTROL_STATS, SERIALIZER::encode, SERIALIZER::decode, nodeId);
        }
    }

    @Override
    public CompletableFuture<Set<String>> availableResources(NodeId nodeId,
                                                             Type resourceType) {
        if (clusterService.getLocalNode().id().equals(nodeId)) {
            Set<String> resources = getLocalAvailableResources(resourceType);
            return CompletableFuture.completedFuture(resources);
        } else {
            return communicationService.sendAndReceive(createResourceRequest(resourceType),
                    CONTROL_RESOURCE, SERIALIZER::encode, SERIALIZER::decode, nodeId);
        }
    }

    /**
     * Builds and returns metric database instance with given resource name,
     * resource type and metric type.
     *
     * @param resourceName resource name
     * @param resourceType resource type
     * @param metricTypes  metric type
     * @return metric database instance
     */
    private MetricsDatabase genMDbBuilder(String resourceName,
                                          Type resourceType,
                                          Set<ControlMetricType> metricTypes) {
        MetricsDatabase.Builder builder = new DefaultMetricsDatabase.Builder();
        builder.withMetricName(resourceType.toString());
        builder.withResourceName(resourceName);
        metricTypes.forEach(type -> builder.addMetricType(type.toString()));
        return builder.build();
    }

    /**
     * Updates network metrics with given metric map and resource name.
     *
     * @param metricMap    a metric map which is comprised of metric type and value
     * @param resourceName resource name
     */
    private void updateNetworkMetrics(Map<ControlMetricType, Double> metricMap,
                                      String resourceName) {
        if (!networkMetricsMap.containsKey(resourceName)) {
            networkMetricsMap.put(resourceName, genMDbBuilder(resourceName,
                    Type.NETWORK, NETWORK_METRICS));
        }
        networkMetricsMap.get(resourceName).updateMetrics(convertMap(metricMap));
    }

    /**
     * Updates disk metrics with given metric map and resource name.
     *
     * @param metricMap    a metric map which is comprised of metric type and value
     * @param resourceName resource name
     */
    private void updateDiskMetrics(Map<ControlMetricType, Double> metricMap,
                                   String resourceName) {
        if (!diskMetricsMap.containsKey(resourceName)) {
            diskMetricsMap.put(resourceName, genMDbBuilder(resourceName,
                    Type.DISK, DISK_METRICS));
        }
        diskMetricsMap.get(resourceName).updateMetrics(convertMap(metricMap));
    }

    /**
     * Updates control message metrics with given metric map and device identifier.
     *
     * @param metricMap a metric map which is comprised of metric type and value
     * @param deviceId  device identifier
     */
    private void updateControlMessages(Map<ControlMetricType, Double> metricMap,
                                       DeviceId deviceId) {
        if (!controlMessageMap.containsKey(deviceId)) {
            controlMessageMap.put(deviceId, genMDbBuilder(deviceId.toString(),
                    Type.CONTROL_MESSAGE, CONTROL_MESSAGE_METRICS));
        }
        controlMessageMap.get(deviceId).updateMetrics(convertMap(metricMap));
    }

    /**
     * Converts metric map into a new map which contains string formatted metric type as key.
     *
     * @param metricMap metric map in which ControlMetricType is key
     * @return a new map in which string formatted metric type is key
     */
    private Map<String, Double> convertMap(Map<ControlMetricType, Double> metricMap) {
        if (metricMap == null) {
            return ImmutableMap.of();
        }
        Map newMap = Maps.newConcurrentMap();
        metricMap.forEach((k, v) -> newMap.putIfAbsent(k.toString(), v));
        return newMap;
    }

    /**
     * Handles control metric request from remote node.
     *
     * @param request control metric request
     * @return completable future object of control load snapshot
     */
    private CompletableFuture<ControlLoadSnapshot>
        handleMetricsRequest(ControlMetricsRequest request) {

        checkArgument(request.getType() != null, METRIC_TYPE_NULL);

        ControlLoad load;
        if (request.getResourceName() != null && request.getUnit() != null) {
            load = getLocalLoad(request.getType(), request.getResourceName());
        } else {
            load = getLocalLoad(request.getType(), request.getDeviceId());
        }

        long average;
        if (request.getUnit() != null) {
            average = load.average(request.getDuration(), request.getUnit());
        } else {
            average = load.average();
        }
        ControlLoadSnapshot resp =
                new ControlLoadSnapshot(load.latest(), average, load.time());
        return CompletableFuture.completedFuture(resp);
    }

    /**
     * Handles control resource request from remote node.
     *
     * @param request control resource type
     * @return completable future object of control resource set
     */
    private CompletableFuture<Set<String>>
        handleResourceRequest(ControlResourceRequest request) {

        checkArgument(request.getType() != null, RESOURCE_TYPE_NULL);

        Set<String> resources = getLocalAvailableResources(request.getType());
        return CompletableFuture.completedFuture(resources);
    }

    /**
     * Generates a control metric request.
     *
     * @param type     control metric type
     * @param deviceId device identifier
     * @return control metric request instance
     */
    private ControlMetricsRequest createMetricsRequest(ControlMetricType type,
                                                       Optional<DeviceId> deviceId) {
        return new ControlMetricsRequest(type, deviceId);
    }

    /**
     * Generates a control metric request with given projected time range.
     *
     * @param type     control metric type
     * @param duration projected time duration
     * @param unit     projected time unit
     * @param deviceId device identifier
     * @return control metric request instance
     */
    private ControlMetricsRequest createMetricsRequest(ControlMetricType type,
                                                       int duration, TimeUnit unit,
                                                       Optional<DeviceId> deviceId) {
        return new ControlMetricsRequest(type, duration, unit, deviceId);
    }

    /**
     * Generates a control metric request.
     *
     * @param type         control metric type
     * @param resourceName resource name
     * @return control metric request instance
     */
    private ControlMetricsRequest createMetricsRequest(ControlMetricType type,
                                                       String resourceName) {
        return new ControlMetricsRequest(type, resourceName);
    }

    /**
     * Generates a control metric request with given projected time range.
     *
     * @param type         control metric type
     * @param duration     projected time duration
     * @param unit         projected time unit
     * @param resourceName resource name
     * @return control metric request instance
     */
    private ControlMetricsRequest createMetricsRequest(ControlMetricType type,
                                                       int duration, TimeUnit unit,
                                                       String resourceName) {
        return new ControlMetricsRequest(type, duration, unit, resourceName);
    }

    /**
     * Generates a control resource request with given resource type.
     *
     * @param type control resource type
     * @return control resource request instance
     */
    private ControlResourceRequest createResourceRequest(ControlResource.Type type) {
        return new ControlResourceRequest(type);
    }

    /**
     * Returns a snapshot of control load.
     *
     * @param cl control load
     * @return a snapshot of control load
     */
    private ControlLoadSnapshot snapshot(ControlLoad cl) {
        if (cl != null) {
            return new ControlLoadSnapshot(cl.latest(), cl.average(), cl.time());
        }
        return null;
    }

    /**
     * Returns a snapshot of control load with given projected time range.
     *
     * @param cl       control load
     * @param duration projected time duration
     * @param unit     projected time unit
     * @return a snapshot of control load
     */
    private ControlLoadSnapshot snapshot(ControlLoad cl, int duration, TimeUnit unit) {
        if (cl != null) {

            return new ControlLoadSnapshot(cl.latest(), cl.average(duration, unit),
                    cl.time(), cl.recent(duration, unit));
        }
        return null;
    }

    /**
     * Returns local control load.
     *
     * @param type     metric type
     * @param deviceId device identifier
     * @return control load
     */
    private ControlLoad getLocalLoad(ControlMetricType type,
                                     Optional<DeviceId> deviceId) {
        if (deviceId.isPresent()) {
            // returns control message stats
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
        return null;
    }

    /**
     * Returns local control load.
     *
     * @param type         metric type
     * @param resourceName resource name
     * @return control load
     */
    private ControlLoad getLocalLoad(ControlMetricType type, String resourceName) {
        NodeId localNodeId = clusterService.getLocalNode().id();

        // returns disk I/O stats
        if (DISK_METRICS.contains(type) &&
                availableResourcesSync(localNodeId, Type.DISK).contains(resourceName)) {
            return new DefaultControlLoad(diskMetricsMap.get(resourceName), type);
        }

        // returns network I/O stats
        if (NETWORK_METRICS.contains(type) &&
                availableResourcesSync(localNodeId, Type.NETWORK).contains(resourceName)) {
            return new DefaultControlLoad(networkMetricsMap.get(resourceName), type);
        }
        return null;
    }

    /**
     * Obtains the available resource list from local node.
     *
     * @param resourceType control resource type
     * @return a set of available control resource
     */
    private Set<String> getLocalAvailableResources(Type resourceType) {
        Set<String> resources = ImmutableSet.of();
        if (RESOURCE_TYPE_SET.contains(resourceType)) {
            if (Type.CONTROL_MESSAGE.equals(resourceType)) {
                resources = ImmutableSet.copyOf(availableDeviceIdSet.stream()
                        .map(DeviceId::toString).collect(Collectors.toSet()));
            } else {
                Set<String> res = availableResourceMap.get(resourceType);
                resources = res == null ? ImmutableSet.of() : res;
            }
        }
        return resources;
    }
}
