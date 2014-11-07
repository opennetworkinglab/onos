package org.onlab.onos.store.service.impl;

import org.onlab.onos.store.service.WriteResult;

/**
 * Result of a write operation executed on the DatabaseStateMachine.
 */
public class InternalWriteResult {

    public enum Status {
        OK,
        ABORTED,
        NO_SUCH_TABLE,
        PREVIOUS_VERSION_MISMATCH,
        PREVIOUS_VALUE_MISMATCH
    }

    private final Status status;
    private final WriteResult result;

    public static InternalWriteResult ok(WriteResult result) {
        return new InternalWriteResult(Status.OK, result);
    }

    public InternalWriteResult(Status status, WriteResult result) {
        this.status = status;
        this.result = result;
    }

    public Status status() {
        return status;
    }

    public WriteResult result() {
        return result;
    }
}
