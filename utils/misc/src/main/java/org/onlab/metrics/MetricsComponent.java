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
package org.onlab.metrics;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Components to register for metrics.
 */
public class MetricsComponent implements MetricsComponentRegistry {
    private final String name;

    /**
     * Registry to hold the Features defined in this Component.
     */
    private final ConcurrentMap<String, MetricsFeature> featuresRegistry =
            new ConcurrentHashMap<>();

    /**
     * Constructs a component from a name.
     *
     * @param newName name of the component
     */
    public MetricsComponent(final String newName) {
        name = newName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public MetricsFeature registerFeature(final String featureName) {
        MetricsFeature feature = featuresRegistry.get(featureName);
        if (feature == null) {
            final MetricsFeature createdFeature =
                new MetricsFeature(featureName);
            feature = featuresRegistry.putIfAbsent(featureName, createdFeature);
            if (feature == null) {
                feature = createdFeature;
            }
        }
        return feature;
    }
}
