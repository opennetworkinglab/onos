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

import com.codahale.metrics.Meter;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onlab.metrics.MetricsComponent;
import org.onlab.metrics.MetricsFeature;
import org.onlab.metrics.MetricsService;
import org.onosproject.cpman.ControlMetricType;
import org.onosproject.cpman.ControlResource;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Aggregate system metrics.
 */
public final class SystemMetricsAggregator {

    private final Logger log = getLogger(getClass());

    private static final String DEFAULT_RESOURCE_NAME = "system";
    private static final String DEFAULT_METER_SUFFIX = "rate";
    private static final String DISK_RESOURCE_TYPE = "disk";
    private static final String NETWORK_RESOURCE_TYPE = "network";
    private final Map<ControlMetricType, Meter> systemMap = Maps.newHashMap();
    private final Map<String, Map<ControlMetricType, Meter>> diskMap = Maps.newHashMap();
    private final Map<String, Map<ControlMetricType, Meter>> networkMap = Maps.newHashMap();

    private MetricsService metricsService;

    public static SystemMetricsAggregator getInstance() {
        return SingletonHelper.INSTANCE;
    }

    /**
     * Configures metric services.
     *
     * @param service metrics service
     */
    public void setMetricsService(MetricsService service) {

        metricsService = service;
    }

    /**
     * Increments system metric value.
     *
     * @param type  metric type
     * @param value metric value
     */
    public void increment(ControlMetricType type, long value) {
        systemMap.get(type).mark(value);
    }

    /**
     * Increments disk or network metric value.
     *
     * @param resourceName resource name
     * @param resourceType resource type
     * @param type         control metric type
     * @param value        metric value
     */
    public void increment(String resourceName, String resourceType, ControlMetricType type, long value) {
        if (DISK_RESOURCE_TYPE.equals(resourceType) && diskMap.containsKey(resourceName)) {
            diskMap.get(resourceName).get(type).mark(value);
        }

        if (NETWORK_RESOURCE_TYPE.equals(resourceType) && networkMap.containsKey(resourceName)) {
            networkMap.get(resourceName).get(type).mark(value);
        }
    }

    /**
     * Adds a set of new monitoring metric types.
     *
     * @param optResourceName optional resource name, null denotes system metric
     * @param resType         resource type
     */
    public void addMetrics(Optional<String> optResourceName, String resType) {
        Set<ControlMetricType> metricTypeSet = Sets.newHashSet();
        String resourceName = optResourceName.isPresent() ?
                optResourceName.get() : DEFAULT_RESOURCE_NAME;

        MetricsComponent metricsComponent = metricsService.registerComponent(resourceName);

        if (optResourceName.isPresent()) {
            if (!diskMap.containsKey(resourceName) && DISK_RESOURCE_TYPE.equals(resType)) {
                metricTypeSet.addAll(ControlResource.DISK_METRICS);
                diskMap.putIfAbsent(resourceName,
                        getMeterMap(metricTypeSet, metricsComponent, metricsService));
                metricsService.notifyReporters();
            } else if (!networkMap.containsKey(resourceName) && NETWORK_RESOURCE_TYPE.equals(resType)) {
                metricTypeSet.addAll(ControlResource.NETWORK_METRICS);
                networkMap.putIfAbsent(resourceName,
                        getMeterMap(metricTypeSet, metricsComponent, metricsService));
                metricsService.notifyReporters();
            } else {
                return;
            }
        } else {
            if (systemMap.isEmpty()) {
                metricTypeSet.addAll(ControlResource.MEMORY_METRICS);
                metricTypeSet.addAll(ControlResource.CPU_METRICS);

                systemMap.putAll(getMeterMap(metricTypeSet, metricsComponent, metricsService));
                metricsService.notifyReporters();
            }
        }
    }

    private Map<ControlMetricType, Meter> getMeterMap(Set<ControlMetricType> types,
                                                      MetricsComponent component,
                                                      MetricsService service) {
        Map<ControlMetricType, Meter> meterMap = Maps.newHashMap();
        types.forEach(type -> {
            MetricsFeature metricsFeature = component.registerFeature(type.toString());
            Meter meter = service.createMeter(component, metricsFeature, DEFAULT_METER_SUFFIX);
            meterMap.putIfAbsent(type, meter);
        });
        return meterMap;
    }

    private SystemMetricsAggregator() {
    }

    private static class SingletonHelper {
        private static final SystemMetricsAggregator INSTANCE = new SystemMetricsAggregator();
    }
}
