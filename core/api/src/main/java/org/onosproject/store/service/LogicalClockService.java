package org.onosproject.store.service;

import org.onosproject.store.Timestamp;

/**
 * Service that issues logical timestamps.
 * <p>
 * The logical timestamps are useful for establishing a total ordering of
 * arbitrary cluster wide events without relying on a fully synchronized
 * system clock (wall clock)
 */
public interface LogicalClockService {

    /**
     * Generates a new logical timestamp.
     *
     * @return timestamp
     */
    Timestamp getTimestamp();
}
