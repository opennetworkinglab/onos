package org.onosproject.store.service;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Representation of a ConsistentMap update notification.
 *
 * @param <K> key type
 * @param <V> value type
 */
public class MapEvent<K, V> {

    /**
     * MapEvent type.
     */
    public enum Type {
        /**
         * Entry inserted into the map.
         */
        INSERT,

        /**
         * Existing map entry updated.
         */
        UPDATE,

        /**
         * Entry removed from map.
         */
        REMOVE
    }

    private final String name;
    private final Type type;
    private final K key;
    private final Versioned<V> value;

    /**
     * Creates a new event object.
     *
     * @param name map name
     * @param type the type of the event
     * @param key the key the event concerns
     * @param value the value related to the key, or null for remove events
     */
    public MapEvent(String name, Type type, K key, Versioned<V> value) {
        this.name = name;
        this.type = type;
        this.key = key;
        this.value = value;
    }

    /**
     * Returns the map name.
     *
     * @return name of map
     */
    public String name() {
        return name;
    }

    /**
     * Returns the type of the event.
     *
     * @return the type of the event
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the key this event concerns.
     *
     * @return the key
     */
    public K key() {
        return key;
    }

    /**
     * Returns the value associated with this event. If type is REMOVE,
     * this is the value that was removed. If type is INSERT/UPDATE, this is
     * the new value.
     *
     * @return the value
     */
    public Versioned<V> value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MapEvent)) {
            return false;
        }

        MapEvent<K, V> that = (MapEvent) o;
        return Objects.equals(this.name, that.name) &&
                Objects.equals(this.type, that.type) &&
                Objects.equals(this.key, that.key) &&
                Objects.equals(this.value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, key, value);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("name", name)
                .add("type", type)
                .add("key", key)
                .add("value", value)
                .toString();
    }
}
