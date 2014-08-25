package net.onrc.onos.of.ctl.util;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkArgument;

/**
 * The class representing an ONOS Instance ID.
 *
 * This class is immutable.
 */
public final class InstanceId {
    private final String id;

    /**
     * Constructor from a string value.
     *
     * @param id the value to use.
     */
    public InstanceId(String id) {
        this.id = checkNotNull(id);
        checkArgument(!id.isEmpty(), "Empty ONOS Instance ID");
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof InstanceId)) {
            return false;
        }

        InstanceId that = (InstanceId) obj;
        return this.id.equals(that.id);
    }

    @Override
    public String toString() {
        return id;
    }
}
