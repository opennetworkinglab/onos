package org.onlab.onos.store.service;

/**
 * Service interface for mutual exclusion primitives.
 */
public interface LockService {

    /**
     * Creates a new lock instance.
     * A successful return from this method does not mean the resource guarded by the path is locked.
     * The caller is expected to call Lock.lock() to acquire the lock.
     * @param path unique lock name.
     * @return a Lock instance that can be used to acquire the lock.
     */
    Lock create(String path);

    /**
     * Adds a listener to be notified of lock events.
     * @param listener listener to be added.
     */
    void addListener(LockEventListener listener);

    /**
     * Removes a listener that was previously added.
     * @param listener listener to be removed.
     */
    void removeListener(LockEventListener listener);
}