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

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cpman.ControlMetric;
import org.onosproject.cpman.ControlPlaneMonitorService;
import org.onosproject.cpman.MetricValue;
import org.onosproject.net.DeviceId;

import java.util.Optional;

/**
 * Default ControlMetricsObserver.
 */
public class DefaultControlMetricsObserver implements ControlMetricsObserver {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ControlPlaneMonitorService controlPlaneMonitorService;

    @Override
    public void feedMetrics(MetricsAggregator ma, Optional<DeviceId> deviceId) {
        MetricValue mv = new MetricValue.Builder()
                        .rate(ma.getRate())
                        .count(ma.getCount())
                        .load(ma.getLoad())
                        .add();
        ControlMetric cm = new ControlMetric(ma.getMetricsType(), mv);
        controlPlaneMonitorService.updateMetric(cm, 1, deviceId);
    }

    @Override
    public void feedMetrics(MetricsAggregator ma, String resourceName) {
        MetricValue mv = new MetricValue.Builder()
                        .rate(ma.getRate())
                        .count(ma.getCount())
                        .load(ma.getLoad())
                        .add();
        ControlMetric cm = new ControlMetric(ma.getMetricsType(), mv);
        controlPlaneMonitorService.updateMetric(cm, 1, resourceName);
    }
}