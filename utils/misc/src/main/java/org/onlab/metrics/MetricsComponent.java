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
    MetricsComponent(final String newName) {
        name = newName;
    }

    @Override public String getName() {
        return name;
    }

    @Override public MetricsFeature registerFeature(final String featureName) {
        MetricsFeature feature = featuresRegistry.get(featureName);
        if (feature == null) {
            final MetricsFeature createdFeature = new MetricsFeature(featureName);
            feature = featuresRegistry.putIfAbsent(featureName, createdFeature);
            if (feature == null) {
                feature = createdFeature;
            }
        }
        return feature;
    }
}
