package org.onosproject.store.service;

/**
 * Listener for lock events.
 */
public interface LockEventListener {

    /**
     * Notifies the listener of a lock's lease expiration event.
     * @param lock lock whose lease has expired.
     */
    void leaseExpired(Lock lock);
}
