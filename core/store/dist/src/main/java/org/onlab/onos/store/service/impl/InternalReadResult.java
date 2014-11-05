package org.onlab.onos.store.service.impl;

import org.onlab.onos.store.service.ReadResult;

public class InternalReadResult {

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