package org.onlab.onos.net;

import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a set of simple annotations that can be used to add arbitrary
 * attributes to various parts of the data model.
 */
public final class Annotations {

    private final Map<String, String> map;

    /**
     * Creates a new set of annotations using the specified immutable map.
     *
     * @param map immutable map of key/value pairs
     */
    private Annotations(ImmutableMap<String, String> map) {
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

    /**
     * Returns the set of keys for available annotations. Note that this set
     * includes keys for any attributes tagged for removal.
     *
     * @return annotation keys
     */
    public Set<String> keys() {
        return map.keySet();
    }

    /**
     * Returns the value of the specified annotation.
     *
     * @param key annotation key
     * @return annotation value
     */
    public String value(String key) {
        String value = map.get(key);
        return Objects.equals(Builder.REMOVED, value) ? null : value;
    }

    /**
     * Indicates whether the specified key has been tagged as removed. This is
     * used to for merging sparse annotation sets.
     *
     * @param key annotation key
     * @return true if the previous annotation has been tagged for removal
     */
    public boolean isRemoved(String key) {
        return Objects.equals(Builder.REMOVED, map.get(key));
    }

    /**
     * Facility for gradually building model annotations.
     */
    public static final class Builder {

        private static final String REMOVED = "~rEmOvEd~";
        private final ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

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
        public Annotations build() {
            return new Annotations(builder.build());
        }
    }
}
