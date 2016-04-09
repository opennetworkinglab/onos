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
public class SystemMetricsAggregator {

    private final Logger log = getLogger(getClass());

    private static final String DEFAULT_RESOURCE_NAME = "system";
    private static final String DEFAULT_METER_SUFFIX = "rate";
    private static final String DISK_RESOURCE_TYPE = "disk";
    private static final String NETWORK_RESOURCE_TYPE = "network";
    private final Map<ControlMetricType, Meter> meterMap = Maps.newHashMap();
    private final Set<ControlMetricType> metricTypeSet = Sets.newHashSet();

    public SystemMetricsAggregator(MetricsService metricsService, Optional<String> resName, String resType) {
        String resourceName = resName.isPresent() ? resName.get() : DEFAULT_RESOURCE_NAME;
        MetricsComponent mc = metricsService.registerComponent(resourceName);

        if (resName.isPresent()) {
            if (DISK_RESOURCE_TYPE.equals(resType)) {
                metricTypeSet.addAll(ControlResource.DISK_METRICS);
            } else if (NETWORK_RESOURCE_TYPE.equals(resType)) {
                metricTypeSet.addAll(ControlResource.NETWORK_METRICS);
            } else {
                log.warn("Not valid resource type {}", resType);
            }
        } else {
            metricTypeSet.addAll(ControlResource.MEMORY_METRICS);
            metricTypeSet.addAll(ControlResource.CPU_METRICS);
        }

        metricTypeSet.forEach(type -> {
            MetricsFeature metricsFeature = mc.registerFeature(type.toString());
            Meter meter = metricsService.createMeter(mc, metricsFeature, DEFAULT_METER_SUFFIX);
            meterMap.putIfAbsent(type, meter);
        });
    }

    /**
     * Increments metric value.
     *
     * @param type metric type
     * @param value metric value
     */
    public void increment(ControlMetricType type, long value) {
        meterMap.get(type).mark(value);
    }
}
