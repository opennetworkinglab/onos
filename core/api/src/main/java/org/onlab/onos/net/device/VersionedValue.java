package org.onlab.onos.net.device;

import java.util.Objects;

/**
 * Wrapper class for a entity that is versioned
 * and can either be up or down.
 *
 * @param <T> type of the value.
 */
public class VersionedValue<T> {
    private final T entity;
    private final Timestamp timestamp;
    private final boolean isUp;

    public VersionedValue(T entity, boolean isUp, Timestamp timestamp) {
        this.entity = entity;
        this.isUp = isUp;
        this.timestamp = timestamp;
    }

    /**
     * Returns the value.
     * @return value.
     */
    public T entity() {
        return entity;
    }

    /**
     * Tells whether the entity is up or down.
     * @return true if up, false otherwise.
     */
    public boolean isUp() {
        return isUp;
    }

    /**
     * Returns the timestamp (version) associated with this entity.
     * @return timestamp.
     */
    public Timestamp timestamp() {
        return timestamp;
    }


    @Override
    public int hashCode() {
        return Objects.hash(entity, timestamp, isUp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        @SuppressWarnings("unchecked")
        VersionedValue<T> that = (VersionedValue<T>) obj;
        return Objects.equals(this.entity, that.entity) &&
                Objects.equals(this.timestamp, that.timestamp) &&
                Objects.equals(this.isUp, that.isUp);
    }

    // Default constructor for serializer
    protected VersionedValue() {
        this.entity = null;
        this.isUp = false;
        this.timestamp = null;
    }
}
