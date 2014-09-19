package org.onlab.onos.net.host;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.HashSet;
import java.util.Set;

import org.onlab.onos.net.HostLocation;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import com.google.common.collect.ImmutableSet;

public class DefaultHostDescription implements HostDescription {

    private final MacAddress mac;
    private final VlanId vlan;
    private final HostLocation location;
    private final Set<IpPrefix> ips;

    public DefaultHostDescription(MacAddress mac, VlanId vlan,
            HostLocation loc) {
        this.mac = mac;
        this.vlan = vlan;
        this.location = loc;
        this.ips = new HashSet<IpPrefix>();
    }

    public DefaultHostDescription(MacAddress mac, VlanId vlan,
            HostLocation loc, Set<IpPrefix> ips) {
        this.mac = mac;
        this.vlan = vlan;
        this.location = loc;
        this.ips = new HashSet<IpPrefix>(ips);
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
