package org.onlab.onos.net.host;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;

import com.google.common.base.MoreObjects;

/**
 * Represents address information bound to a port.
 */
public class PortAddresses {

    private final ConnectPoint connectPoint;
    private final Set<IpPrefix> ipAddresses;
    private final MacAddress macAddress;

    /**
     * Constructs a PortAddress object for the given connection point, with a
     * set of IP addresses and a MAC address.
     * <p/>
     * Both address parameters are optional and can be set to null.
     *
     * @param connectPoint the connection point these addresses are for
     * @param ips a set of IP addresses
     * @param mac a MAC address
     */
    public PortAddresses(ConnectPoint connectPoint,
            Set<IpPrefix> ips, MacAddress mac) {
        this.connectPoint = connectPoint;
        this.ipAddresses = (ips == null) ? Collections.<IpPrefix>emptySet() : new HashSet<>(ips);
        this.macAddress = mac;
    }

    /**
     * Returns the connection point this address information is bound to.
     *
     * @return the connection point
     */
    public ConnectPoint connectPoint() {
        return connectPoint;
    }

    /**
     * Returns the set of IP addresses.
     *
     * @return the IP addresses
     */
    public Set<IpPrefix> ips() {
        return ipAddresses;
    }

    /**
     * Returns the MAC address.
     *
     * @return the MAC address
     */
    public MacAddress mac() {
        return macAddress;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof PortAddresses)) {
            return false;
        }

        PortAddresses otherPa = (PortAddresses) other;

        return Objects.equals(this.connectPoint, otherPa.connectPoint)
                && Objects.equals(this.ipAddresses, otherPa.ipAddresses)
                && Objects.equals(this.macAddress, otherPa.macAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectPoint, ipAddresses, macAddress);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
            .add("connect-point", connectPoint)
            .add("ip-addresses", ipAddresses)
            .add("mac-address", macAddress)
            .toString();
    }
}
