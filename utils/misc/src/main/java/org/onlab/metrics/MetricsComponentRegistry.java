package org.onlab.metrics;

/**
 * Registry Entry for Metrics Components.
 */
public interface MetricsComponentRegistry {
    String getName();

    MetricsFeature registerFeature(String featureName);
}
