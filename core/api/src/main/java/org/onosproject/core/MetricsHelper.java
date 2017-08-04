/*
 * Copyright 2014-present Open Networking Foundation
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

package org.onosproject.core;

import org.onlab.metrics.MetricsComponent;
import org.onlab.metrics.MetricsFeature;
import org.onlab.metrics.MetricsService;

import com.codahale.metrics.Timer;

/**
 * Collection of utility methods used for providing Metrics.
 */
public interface MetricsHelper {

    /**
     * Returns MetricService instance.
     *
     * @return MetricService instance
     */
    MetricsService metricsService();


    /**
     * Creates a Timer instance with given name.
     *
     * @param component component name
     * @param feature   feature name
     * @param name      timer name
     * @return          Timer instance
     */
    default Timer createTimer(String component, String feature, String name) {
        final MetricsService metricsService = metricsService();
        if (metricsService != null) {
            MetricsComponent c = metricsService.registerComponent(component);
            MetricsFeature f = c.registerFeature(feature);
            return metricsService.createTimer(c, f, name);
        }
        return null;
    }

}
