package org.onlab.metrics;

/**
 * Features to tag metrics.
 */
public class MetricsFeature {
    private final String name;

    /**
     * Constructs a Feature from a name.
     *
     * @param newName name of the Feature
     */
    public MetricsFeature(final String newName) {
        name = newName;
    }

    /**
     * Fetches the name of the Feature.
     *
     * @return name of the Feature
     */
    public String getName() {
        return name;
    }
}
