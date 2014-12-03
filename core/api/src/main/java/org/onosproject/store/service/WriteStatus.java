package org.onlab.onos.store.service;

/**
 * Status of completed write request.
 */
public enum WriteStatus {

    /**
     * Write completed successfully.
     */
    OK,

    /**
     * Write was aborted (ex: if one or more write operations in a batch fail, others are aborted).
     */
    ABORTED,

    /**
     * Write failed due to pre-condition failure. (ex: version or value mis-match).
     */
    PRECONDITION_VIOLATION,

    /**
     * Write failed due to an invalid table name being specified.
     */
    NO_SUCH_TABLE,
}
