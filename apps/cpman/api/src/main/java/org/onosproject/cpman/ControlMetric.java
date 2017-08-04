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
package org.onosproject.cpman;

/**
 * Include various control plane metrics.
 */
public class ControlMetric {

    private final ControlMetricType metricType;
    private final MetricValue metricValue;

    /**
     * Constructs a control metric using the given control metric type and
     * metric value.
     *
     * @param metricType metric type reference
     * @param metricValue metric value reference
     */
    public ControlMetric(ControlMetricType metricType, MetricValue metricValue) {
        this.metricType = metricType;
        this.metricValue = metricValue;
    }

    /**
     * Returns metric type reference.
     *
     * @return metric type reference
     */
    public ControlMetricType metricType() {
        return metricType;
    }

    /**
     * Returns metric value reference.
     *
     * @return metric value reference
     */
    public MetricValue metricValue() {
        return metricValue;
    }
}
