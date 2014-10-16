package org.onlab.onos.sdnip.config;

import java.util.Objects;
import java.util.Set;

import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.host.PortAddresses;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;

/**
 * An Interface is a set of addresses that are logically mapped to a switch
 * port in the network.
 */
public class Interface {
    private final ConnectPoint connectPoint;
    private final Set<IpPrefix> ipAddresses;
    private final MacAddress macAddress;

    /**
     * Creates an Interface based on a connection point, a set of IP addresses
     * and a MAC address.
     *
     * @param connectPoint the connect point this interface is mapped to
     * @param prefixAddress the IP addresses for the interface
     * @param macAddress the MAC address of the interface
     */
    public Interface(ConnectPoint connectPoint, Set<IpPrefix> prefixAddress,
                     MacAddress macAddress) {
        this.connectPoint = connectPoint;
        this.ipAddresses = Sets.newHashSet(prefixAddress);
        this.macAddress = macAddress;
    }

    /**
     * Creates an Interface based on a PortAddresses object.
     *
     * @param portAddresses the PortAddresses object to turn into an Interface
     */
    public Interface(PortAddresses portAddresses) {
        connectPoint = portAddresses.connectPoint();
        ipAddresses = Sets.newHashSet(portAddresses.ips());
        macAddress = portAddresses.mac();
    }

    /**
     * Retrieves the connection point that this interface maps to.
     *
     * @return the connection point
     */
    public ConnectPoint connectPoint() {
        return connectPoint;
    }

    /**
     * Retrieves the set of IP addresses that are assigned to the interface.
     *
     * @return the set of IP addresses
     */
   public Set<IpPrefix> ips() {
        return ipAddresses;
    }

   /**
    * Retrieves the MAC address that is assigned to the interface.
    *
    * @return the MAC address
    */
   public MacAddress mac() {
       return macAddress;
   }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Interface)) {
            return false;
        }

        Interface otherInterface = (Interface) other;

        return  connectPoint.equals(otherInterface.connectPoint) &&
                ipAddresses.equals(otherInterface.ipAddresses) &&
                macAddress.equals(otherInterface.macAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(connectPoint, ipAddresses, macAddress);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("connectPoint", connectPoint)
                .add("ipAddresses", ipAddresses)
                .add("macAddress", macAddress)
                .toString();
    }
}
