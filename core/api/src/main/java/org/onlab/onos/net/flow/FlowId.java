package org.onlab.onos.net.flow;

/**
 * Representation of a Flow ID.
 */
public final class FlowId {

    private final int flowid;

    private FlowId(int id) {
        this.flowid = id;
    }

    public static FlowId valueOf(int id) {
        return new FlowId(id);
    }

    public int value() {
        return flowid;
    }
}
