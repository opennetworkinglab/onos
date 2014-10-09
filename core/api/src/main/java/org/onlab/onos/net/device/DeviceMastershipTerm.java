package org.onlab.onos.net.device;

import java.util.Objects;

import org.onlab.onos.cluster.NodeId;

public final class DeviceMastershipTerm {

    private final NodeId master;
    private final int termNumber;

    private DeviceMastershipTerm(NodeId master, int term) {
        this.master = master;
        this.termNumber = term;
    }

    public static DeviceMastershipTerm of(NodeId master, int term) {
        return new DeviceMastershipTerm(master, term);
    }

    public NodeId master() {
        return master;
    }

    public int termNumber() {
        return termNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(master, termNumber);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other instanceof DeviceMastershipTerm) {
            DeviceMastershipTerm that = (DeviceMastershipTerm) other;
            if (!this.master.equals(that.master)) {
                return false;
            }
            if (this.termNumber != that.termNumber) {
                return false;
            }
            return true;
        }
        return false;
    }
}
