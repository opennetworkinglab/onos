package org.onosproject.net.resource;

import java.util.Objects;

public class LabelResourceId implements ResourceId {

    protected long labelId;

    public static LabelResourceId labelResourceId(long labelResourceId) {
        return new LabelResourceId(labelResourceId);
    }

    public LabelResourceId(long labelId) {
        this.labelId = labelId;
    }

    public long getLabelId() {
        return labelId;
    }

    public void setLabelId(long labelId) {
        this.labelId = labelId;
    }

    @Override
    public int hashCode() {
        // TODO Auto-generated method stub
        return Objects.hashCode(labelId);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof LabelResourceId) {
            LabelResourceId that = (LabelResourceId) obj;
            return Objects.equals(this.labelId, that.labelId);
        }
        return false;
    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return String.valueOf(this.labelId);
    }

}
