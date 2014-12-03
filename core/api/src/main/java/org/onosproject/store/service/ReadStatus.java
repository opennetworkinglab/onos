package org.onosproject.store.service;

/**
 * Status of completed read request.
 */
public enum ReadStatus {

    /**
     * Read completed successfully.
     */
    OK,

    /**
     * Read failed due to an invalid table name being specified.
     */
    NO_SUCH_TABLE
}
