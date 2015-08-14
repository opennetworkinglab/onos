package org.onosproject.net;

import java.util.Objects;
import org.onlab.packet.IpAddress;
import com.google.common.base.MoreObjects;

/**
 * Represent for a Element ID using ip address.
 */
public final class IpElementId extends ElementId {

    private final IpAddress ipAddress;

    /**
     * Public construction is prohibited.
     * @param ipAddress ip address
     */
    private IpElementId(IpAddress ipAddress) {
        this.ipAddress = ipAddress;
    }

    /**
     * Create a IP Element ID.
     * @param ipAddress IP address
     * @return IpElementId
     */
    public static IpElementId ipElement(IpAddress ipAddress) {
        return new IpElementId(ipAddress);
    }

    /**
     * Returns the ip address.
     *
     * @return ipAddress
     */
    public IpAddress ipAddress() {
        return ipAddress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ipAddress);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IpElementId) {
            final IpElementId other = (IpElementId) obj;
            return Objects.equals(this.ipAddress, other.ipAddress);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("ipAddress", ipAddress).toString();
    }
}
