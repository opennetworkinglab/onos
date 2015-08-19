package org.onosproject.net.flow.criteria;

import static com.google.common.base.MoreObjects.toStringHelper;
import java.util.Objects;

/**
 * Implementation of MPLS BOS criterion (1 bit).
 */
public class MplsBosCriterion implements Criterion {
    private boolean mplsBos;

    MplsBosCriterion(boolean mplsBos) {
        this.mplsBos = mplsBos;
    }

    @Override
    public Type type() {
        return Type.MPLS_BOS;
    }

    public boolean mplsBos() {
        return mplsBos;
    }

    @Override
    public String toString() {
        return toStringHelper(type().toString())
                .add("bos", mplsBos).toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(type().ordinal(), mplsBos);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MplsBosCriterion) {
            MplsBosCriterion that = (MplsBosCriterion) obj;
            return Objects.equals(mplsBos, that.mplsBos()) &&
                    Objects.equals(this.type(), that.type());
        }
        return false;
    }
}
