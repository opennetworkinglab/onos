package org.onlab.onos.net.flow;

import com.google.common.base.Objects;

/**
 * Representation of a Flow ID.
 */
public final class FlowId {

    private final long flowid;

    private FlowId(long id) {
        this.flowid = id;
    }

    public static FlowId valueOf(long id) {
        return new FlowId(id);
    }

    public long value() {
        return flowid;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (obj.getClass()  == this.getClass()) {
            FlowId that = (FlowId) obj;
            return Objects.equal(this.flowid, that.flowid);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.flowid);
    }
}
