package org.onosproject.net.tunnel;

import java.util.Objects;

import org.onlab.packet.IpAddress;

import com.google.common.base.MoreObjects;
/**
 * Represent for a tunnel point using ip address.
 */
public final class IpTunnelEndPoint implements TunnelEndPoint {

    private final IpAddress ip;

    /**
     * Public construction is prohibited.
     * @param ip ip address
     */
    private IpTunnelEndPoint(IpAddress ip) {
        this.ip = ip;
    }

    /**
     * Create a IP tunnel end point.
     * @param ip IP address
     * @return IpTunnelEndPoint
     */
    public static IpTunnelEndPoint ipTunnelPoint(IpAddress ip) {
        return new IpTunnelEndPoint(ip);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ip);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof IpTunnelEndPoint) {
            final IpTunnelEndPoint other = (IpTunnelEndPoint) obj;
            return Objects.equals(this.ip, other.ip);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass()).add("ip", ip).toString();
    }
}
