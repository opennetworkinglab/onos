package org.onlab.onos.store.device.impl;

import org.onlab.onos.store.Timestamp;

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
}
