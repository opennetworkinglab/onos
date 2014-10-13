package org.onlab.onos.mastership;

import java.util.Objects;

import org.onlab.onos.cluster.NodeId;

public final class MastershipTerm {

    private final NodeId master;
    private final int termNumber;

    private MastershipTerm(NodeId master, int term) {
        this.master = master;
        this.termNumber = term;
    }

    public static MastershipTerm of(NodeId master, int term) {
        return new MastershipTerm(master, term);
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
        if (other instanceof MastershipTerm) {
            MastershipTerm that = (MastershipTerm) other;
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
