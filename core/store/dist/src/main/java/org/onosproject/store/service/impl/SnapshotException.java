package org.onosproject.store.service.impl;

import org.onosproject.store.service.DatabaseException;

/**
 * Exception that indicates a problem with the state machine snapshotting.
 */
@SuppressWarnings("serial")
public class SnapshotException extends DatabaseException {
    public SnapshotException(Throwable t) {
        super(t);
    }
}
