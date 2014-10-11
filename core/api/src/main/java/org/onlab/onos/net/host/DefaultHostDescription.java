package org.onlab.onos.net.host;

import org.onlab.onos.net.AbstractDescription;
import org.onlab.onos.net.HostLocation;
import org.onlab.onos.net.SparseAnnotations;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default implementation of an immutable host description.
 */
public class DefaultHostDescription extends AbstractDescription
        implements HostDescription {

    private final MacAddress mac;
    private final VlanId vlan;
    private final HostLocation location;
    private final IpPrefix ip;

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
        this(mac, vlan, location, null, annotations);
    }

    /**
     * Creates a host description using the supplied information.
     *
     * @param mac         host MAC address
     * @param vlan        host VLAN identifier
     * @param location    host location
     * @param ip          host IP address
     * @param annotations optional key/value annotations map
     */
    public DefaultHostDescription(MacAddress mac, VlanId vlan,
                                  HostLocation location, IpPrefix ip,
                                  SparseAnnotations... annotations) {
        super(annotations);
        this.mac = mac;
        this.vlan = vlan;
        this.location = location;
        this.ip = ip;
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
    public IpPrefix ipAddress() {
        return ip;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("mac", mac)
                .add("vlan", vlan)
                .add("location", location)
                .add("ipAddress", ip)
                .toString();
    }

}
