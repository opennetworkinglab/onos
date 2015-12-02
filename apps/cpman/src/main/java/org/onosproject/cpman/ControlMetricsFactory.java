/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.cpman;

import org.onlab.metrics.MetricsService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Singleton class to provide various control plane metrics to other components.
 */
public final class ControlMetricsFactory {
    private static volatile ControlMetricsFactory uniqueInstance;

    private MetricsService metricsService;
    private boolean enableMonitor = false;

    // define a set of MetricsAggregators
    private MetricsAggregator cpuInfoMetric;
    private MetricsAggregator memoryInfoMetric;
    private Map<DeviceId, MetricsAggregator> inboundPacketMetrics;
    private Map<DeviceId, MetricsAggregator> outboundPacketMetrics;
    private Map<DeviceId, MetricsAggregator> flowmodPacketMetrics;
    private Map<DeviceId, MetricsAggregator> flowrmvPacketMetrics;
    private Map<DeviceId, MetricsAggregator> requestPacketMetrics;
    private Map<DeviceId, MetricsAggregator> replyPacketMetrics;
    private Set<DeviceId> deviceIds = new HashSet<>();

    private ControlMetricsFactory(MetricsService metricsService, DeviceService deviceService) {
        this.metricsService = metricsService;
        registerMetrics();

        deviceService.getDevices().forEach(d->deviceIds.add(d.id()));

        addAllDeviceMetrics(deviceIds);
    }

    public static ControlMetricsFactory getInstance(MetricsService metricsService,
                                                    DeviceService deviceService) {
        if (uniqueInstance == null) {
            synchronized (ControlMetricsFactory.class) {
                if (uniqueInstance == null) {
                    uniqueInstance = new ControlMetricsFactory(metricsService, deviceService);
                }
            }
        }
        return uniqueInstance;
    }

    /**
     * Adds control metrics of a new device.
     *
     * @param deviceId {@link org.onosproject.net.DeviceId}
     */
    public void addMetricsByDeviceId(DeviceId deviceId) {
        MetricsAggregator inbound = new MetricsAggregator(metricsService,
                        ControlMetricType.INBOUND_PACKET, Optional.of(deviceId));
        MetricsAggregator outbound = new MetricsAggregator(metricsService,
                        ControlMetricType.OUTBOUND_PACKET, Optional.of(deviceId));
        MetricsAggregator flowmod = new MetricsAggregator(metricsService,
                        ControlMetricType.FLOW_MOD_PACKET, Optional.of(deviceId));
        MetricsAggregator flowrmv = new MetricsAggregator(metricsService,
                        ControlMetricType.FLOW_REMOVED_PACKET, Optional.of(deviceId));
        MetricsAggregator request = new MetricsAggregator(metricsService,
                        ControlMetricType.REQUEST_PACKET, Optional.of(deviceId));
        MetricsAggregator reply = new MetricsAggregator(metricsService,
                        ControlMetricType.REPLY_PACKET, Optional.of(deviceId));

        inboundPacketMetrics.putIfAbsent(deviceId, inbound);
        outboundPacketMetrics.putIfAbsent(deviceId, outbound);
        flowmodPacketMetrics.putIfAbsent(deviceId, flowmod);
        flowrmvPacketMetrics.putIfAbsent(deviceId, flowrmv);
        requestPacketMetrics.putIfAbsent(deviceId, request);
        replyPacketMetrics.putIfAbsent(deviceId, reply);

        deviceIds.add(deviceId);
    }

    /**
     * Removes control metrics of an existing device.
     *
     * @param deviceId {@link org.onosproject.net.DeviceId}
     */
    public void removeMetricsByDeviceId(DeviceId deviceId) {
        inboundPacketMetrics.remove(deviceId);
        outboundPacketMetrics.remove(deviceId);
        flowmodPacketMetrics.remove(deviceId);
        flowrmvPacketMetrics.remove(deviceId);
        requestPacketMetrics.remove(deviceId);
        replyPacketMetrics.remove(deviceId);

        deviceIds.remove(deviceId);
    }

    public Set<DeviceId> getDeviceIds() {
        return this.deviceIds;
    }

    /**
     * Adds control metrics for all devices.
     *
     * @param deviceIds a set of deviceIds
     */
    public void addAllDeviceMetrics(Set<DeviceId> deviceIds) {
        deviceIds.forEach(v -> addMetricsByDeviceId(v));
    }

    /**
     * Returns monitoring status.
     *
     * @return monitoring status
     */
    public boolean isMonitor() {
        return this.enableMonitor;
    }

    /**
     * Enable control plane monitoring.
     */
    protected void startMonitor() {
        this.enableMonitor = true;
    }

    /**
     * Disable control plane monitoring.
     */
    protected void stopMonitor() {
        this.enableMonitor = false;
    }

    /**
     * Registers new control metrics.
     */
    protected void registerMetrics() {
        cpuInfoMetric = new MetricsAggregator(metricsService,
                        ControlMetricType.CPU_INFO, Optional.ofNullable(null));
        memoryInfoMetric = new MetricsAggregator(metricsService,
                        ControlMetricType.MEMORY_INFO, Optional.ofNullable(null));

        inboundPacketMetrics = new ConcurrentHashMap<>();
        outboundPacketMetrics = new ConcurrentHashMap<>();
        flowmodPacketMetrics = new ConcurrentHashMap<>();
        flowrmvPacketMetrics = new ConcurrentHashMap<>();
        requestPacketMetrics = new ConcurrentHashMap<>();
        replyPacketMetrics = new ConcurrentHashMap<>();
    }

    /**
     * Unregisters all control metrics.
     */
    protected void unregisterMetrics() {
        cpuInfoMetric.removeMetrics();
        memoryInfoMetric.removeMetrics();

        inboundPacketMetrics.clear();
        outboundPacketMetrics.clear();
        flowmodPacketMetrics.clear();
        flowrmvPacketMetrics.clear();
        requestPacketMetrics.clear();
        replyPacketMetrics.clear();
    }

    public MetricsAggregator cpuInfoMetric() {
        return cpuInfoMetric;
    }

    public MetricsAggregator memoryInfoMetric() {
        return memoryInfoMetric;
    }

    public Map<DeviceId, MetricsAggregator> inboundPacketMetrics() {
        return inboundPacketMetrics;
    }

    public Map<DeviceId, MetricsAggregator> outboundPacketMetrics() {
        return outboundPacketMetrics;
    }

    public Map<DeviceId, MetricsAggregator> flowmodPacketMetrics() {
        return flowmodPacketMetrics;
    }

    public Map<DeviceId, MetricsAggregator> flowrmvPacketMetrics() {
        return flowrmvPacketMetrics;
    }

    public Map<DeviceId, MetricsAggregator> requestPacketMetrics() {
        return requestPacketMetrics;
    }

    public Map<DeviceId, MetricsAggregator> replyPacketMetrics() {
        return replyPacketMetrics;
    }

    public MetricsAggregator inboundPacketMetrics(DeviceId deviceId) {
        return inboundPacketMetrics.get(deviceId);
    }

    public MetricsAggregator outboundPacketMetrics(DeviceId deviceId) {
        return outboundPacketMetrics.get(deviceId);
    }

    public MetricsAggregator flowmodPacketMetrics(DeviceId deviceId) {
        return flowmodPacketMetrics.get(deviceId);
    }

    public MetricsAggregator flowrmvPacketMetrics(DeviceId deviceId) {
        return flowrmvPacketMetrics.get(deviceId);
    }

    public MetricsAggregator requestPacketMetrics(DeviceId deviceId) {
        return requestPacketMetrics.get(deviceId);
    }

    public MetricsAggregator replyPacketMetrics(DeviceId deviceId) {
        return replyPacketMetrics.get(deviceId);
    }
}