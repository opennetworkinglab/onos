package org.onosproject.store.service;


/**
 * Builder for consistent maps.
 *
 * @param <K> type for map key
 * @param <V> type for map value
 */
public interface ConsistentMapBuilder<K, V> {

    /**
     * Sets the name of the map.
     * <p>
     * Each consistent map is identified by a unique map name.
     * </p>
     * <p>
     * Note: This is a mandatory parameter.
     * </p>
     *
     * @param name name of the consistent map
     * @return this ConsistentMapBuilder
     */
    ConsistentMapBuilder<K, V> withName(String name);

    /**
     * Sets a serializer that can be used to serialize
     * both the keys and values inserted into the map. The serializer
     * builder should be pre-populated with any classes that will be
     * put into the map.
     * <p>
     * Note: This is a mandatory parameter.
     * </p>
     *
     * @param serializer serializer
     * @return this ConsistentMapBuilder
     */
    ConsistentMapBuilder<K, V> withSerializer(Serializer serializer);

    /**
     * Disables distribution of map entries across multiple database partitions.
     * <p>
     * When partitioning is disabled, the returned map will have a single partition
     * that spans the entire cluster. Furthermore, the changes made to the map are
     * ephemeral and do not survive a full cluster restart.
     * </p>
     * <p>
     * Disabling partitions is more appropriate when the returned map is used for
     * coordination activities such as leader election and not for long term data persistence.
     * </p>
     * <p>
     * Note: By default partitions are enabled and entries in the map are durable.
     * </p>
     * @return this ConsistentMapBuilder
     */
    ConsistentMapBuilder<K, V> withPartitionsDisabled();

    /**
     * Disables map updates.
     * <p>
     * Attempt to update the built map will throw {@code UnsupportedOperationException}.
     *
     * @return this ConsistentMapBuilder
     */
    ConsistentMapBuilder<K, V> withUpdatesDisabled();

    /**
     * Builds an consistent map based on the configuration options
     * supplied to this builder.
     *
     * @return new consistent map
     * @throws java.lang.RuntimeException if a mandatory parameter is missing
     */
    ConsistentMap<K, V> build();

    /**
     * Builds an async consistent map based on the configuration options
     * supplied to this builder.
     *
     * @return new async consistent map
     * @throws java.lang.RuntimeException if a mandatory parameter is missing
     */
    AsyncConsistentMap<K, V> buildAsyncMap();
}
