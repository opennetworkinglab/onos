package org.onlab.onos.net.host;

import com.google.common.collect.ImmutableSet;
import org.onlab.onos.net.AbstractDescription;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.SparseAnnotations;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default implementation of an immutable host description.
 */
public class DefaultHostDescription extends AbstractDescription
        implements HostDescription {

    private final MacAddress mac;
    private final VlanId vlan;
    private final HostLocation location;
    private final Set<IpPrefix> ips;

    /**
     * Creates a host description using the supplied information.
     *
     * @param mac         host MAC address
     * @param vlan        host VLAN identifier
     * @param location    host location
     * @param annotations optional key/value annotations map
     */
    public DefaultHostDescription(MacAddress mac, VlanId vlan,
                                  HostLocation location,
                                  SparseAnnotations... annotations) {
        this(mac, vlan, location, new HashSet<IpPrefix>(), annotations);
    }

    /**
     * Creates a host description using the supplied information.
     *
     * @param mac         host MAC address
     * @param vlan        host VLAN identifier
     * @param location    host location
     * @param ips         of host IP addresses
     * @param annotations optional key/value annotations map
     */
    public DefaultHostDescription(MacAddress mac, VlanId vlan,
                                  HostLocation location, Set<IpPrefix> ips,
                                  SparseAnnotations... annotations) {
        super(annotations);
        this.mac = mac;
        this.vlan = vlan;
        this.location = location;
        this.ips = new HashSet<>(ips);
    }

    @Override
    public MacAddress hwAddress() {
        return mac;
    }

    @Override
    public VlanId vlan() {
        return vlan;
    }

    @Override
    public HostLocation location() {
        return location;
    }

    @Override
    public Set<IpPrefix> ipAddresses() {
        return ImmutableSet.copyOf(ips);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("mac", mac)
                .add("vlan", vlan)
                .add("location", location)
                .add("ipAddresses", ips)
                .toString();
    }

}
