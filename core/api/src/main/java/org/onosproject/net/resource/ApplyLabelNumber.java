package org.onosproject.net.resource;

import java.util.Objects;
/**
 * the number of applying label.
 *
 */
public final class ApplyLabelNumber {
    long applyNum;

    private ApplyLabelNumber(long applyNum) {
        this.applyNum = applyNum;
    }

    public static ApplyLabelNumber applyLabelNumber(long applyNum) {
        return new ApplyLabelNumber(applyNum);
    }

    public long getApplyNum() {
        return applyNum;
    }

    public void setApplyNum(long applyNum) {
        this.applyNum = applyNum;
    }

    @Override
    public int hashCode() {
        return Objects.hash(applyNum);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ApplyLabelNumber) {
            final ApplyLabelNumber that = (ApplyLabelNumber) obj;
            return this.getClass() == that.getClass()
                    && Objects.equals(this.applyNum, that.applyNum);
        }
        return false;
    }

    @Override
    public String toString() {
        return applyNum + "";
    }
}
