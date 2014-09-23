package org.onlab.onos.net;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.onlab.onos.net.provider.ProviderId;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

/**
 * A basic implementation of a Host.
 */
public class DefaultHost extends AbstractElement implements Host {

    private final MacAddress mac;
    private final VlanId vlan;
    private final HostLocation location;
    private final Set<IpPrefix> ips;

    public DefaultHost(ProviderId providerId, HostId id, MacAddress mac,
            VlanId vlan, HostLocation loc, Set<IpPrefix> ips) {
        super(providerId, id);
        this.mac = mac;
        this.vlan = vlan;
        this.location = loc;
        this.ips = new HashSet<IpPrefix>(ips);
    }

    @Override
    public HostId id() {
        return (HostId) super.id();
    }

    @Override
    public MacAddress mac() {
        return mac;
    }

    @Override
    public Set<IpPrefix> ipAddresses() {
        return Collections.unmodifiableSet(ips);
    }

    @Override
    public HostLocation location() {
        return location;
    }

    @Override
    public VlanId vlan() {
        return vlan;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, mac, vlan, location);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof DefaultHost) {
            final DefaultHost other = (DefaultHost) obj;
            return Objects.equals(this.id, other.id) &&
                    Objects.equals(this.mac, other.mac) &&
                    Objects.equals(this.vlan, other.vlan) &&
                    Objects.equals(this.location, other.location);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("mac", mac)
                .add("vlan", vlan)
                .add("location", location)
                .add("ipAddresses", ips)
                .toString();
    }

}
