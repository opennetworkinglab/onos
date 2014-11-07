package org.onlab.onos.store.service.impl;

import java.io.Serializable;

import org.onlab.onos.store.service.ReadResult;

/**
 * Result of a read operation executed on the DatabaseStateMachine.
 */
@SuppressWarnings("serial")
public class InternalReadResult implements Serializable {

    public enum Status {
        OK,
        NO_SUCH_TABLE
    }

    private final Status status;
    private final ReadResult result;

    public InternalReadResult(Status status, ReadResult result) {
        this.status = status;
        this.result = result;
    }

    public Status status() {
        return status;
    }

    public ReadResult result() {
        return result;
    }

    @Override
    public String toString() {
        return "InternalReadResult [status=" + status + ", result=" + result
                + "]";
    }
}