package org.onosproject.incubator.net.resource.label;

import org.onosproject.net.resource.ResourceId;

import java.util.Objects;

/**
 * Representation of a label.
 */
public final class LabelResourceId implements ResourceId {

    private long labelId;

    public static LabelResourceId labelResourceId(long labelResourceId) {
        return new LabelResourceId(labelResourceId);
    }

    // Public construction is prohibited
    private LabelResourceId(long labelId) {
        this.labelId = labelId;
    }

    public long labelId() {
        return labelId;
    }

    @Override
    public int hashCode() {
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
        return String.valueOf(this.labelId);
    }

}
