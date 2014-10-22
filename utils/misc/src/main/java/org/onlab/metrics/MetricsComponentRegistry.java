package org.onlab.metrics;

/**
 * Registry Entry for Metrics Components.
 */
public interface MetricsComponentRegistry {
    /**
     * Fetches the name of the Component.
     *
     * @return name of the Component
     */
    String getName();

    /**
     * Registers a Feature for this component.
     *
     * @param featureName name of the Feature to register
     * @return Feature object that can be used when creating Metrics
     */
    MetricsFeature registerFeature(String featureName);
}
