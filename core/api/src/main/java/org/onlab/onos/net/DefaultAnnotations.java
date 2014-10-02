package org.onlab.onos.net;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a set of simple annotations that can be used to add arbitrary
 * attributes to various parts of the data model.
 */
public final class DefaultAnnotations implements SparseAnnotations {

    private final Map<String, String> map;

    // For serialization
    private DefaultAnnotations() {
        this.map = null;
    }

    /**
     * Creates a new set of annotations using the specified immutable map.
     *
     * @param map immutable map of key/value pairs
     */
    private DefaultAnnotations(Map<String, String> map) {
        this.map = map;
    }

    /**
     * Creates a new annotations builder.
     *
     * @return new annotations builder
     */
    public static Builder builder() {
        return new Builder();
    }


    @Override
    public Set<String> keys() {
        return map.keySet();
    }

    @Override
    public String value(String key) {
        String value = map.get(key);
        return Objects.equals(Builder.REMOVED, value) ? null : value;
    }

    @Override
    public boolean isRemoved(String key) {
        return Objects.equals(Builder.REMOVED, map.get(key));
    }

    /**
     * Facility for gradually building model annotations.
     */
    public static final class Builder {

        private static final String REMOVED = "~rEmOvEd~";

        // FIXME: Figure out whether and how to make this immutable and serializable
//        private final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
        private final Map<String, String> builder = new HashMap<>();

        // Private construction is forbidden.
        private Builder() {
        }

        /**
         * Adds the specified annotation. Any previous value associated with
         * the given annotation key will be overwritten.
         *
         * @param key   annotation key
         * @param value annotation value
         * @return self
         */
        public Builder set(String key, String value) {
            builder.put(key, value);
            return this;
        }

        /**
         * Adds the specified annotation. Any previous value associated with
         * the given annotation key will be tagged for removal.
         *
         * @param key annotation key
         * @return self
         */
        public Builder remove(String key) {
            builder.put(key, REMOVED);
            return this;
        }

        /**
         * Returns immutable annotations built from the accrued key/values pairs.
         *
         * @return annotations
         */
        public DefaultAnnotations build() {
//            return new DefaultAnnotations(builder.build());
            return new DefaultAnnotations(builder);
        }
    }
}
