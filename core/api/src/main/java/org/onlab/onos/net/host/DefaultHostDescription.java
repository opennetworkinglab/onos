package org.onlab.onos.net.host;

import static com.google.common.base.MoreObjects.toStringHelper;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.onlab.onos.net.HostLocation;
import org.onlab.packet.IPv4;
import org.onlab.packet.MACAddress;

public class DefaultHostDescription implements HostDescription {

    private final MACAddress mac;
    private final short vlan;
    private final HostLocation location;
    private final Set<IPv4> ips;

    public DefaultHostDescription(MACAddress mac, short vlan,
            HostLocation loc, Set<IPv4> ips) {
        this.mac = mac;
        this.vlan = vlan;
        this.location = loc;
        this.ips = new HashSet<IPv4>(ips);
    }

    @Override
    public MACAddress hwAddress() {
        return mac;
    }

    @Override
    public short vlan() {
        return vlan;
    }

    @Override
    public HostLocation location() {
        return location;
    }

    @Override
    public Set<IPv4> ipAddresses() {
        return Collections.unmodifiableSet(ips);
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
