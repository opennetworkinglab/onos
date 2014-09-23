package org.onlab.onos.cluster;

import org.onlab.packet.IpPrefix;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default implementation of a controller instance descriptor.
 */
public class DefaultControllerNode implements ControllerNode {

    private final NodeId id;
    private final IpPrefix ip;

    /**
     * Creates a new instance with the specified id and IP address.
     *
     * @param id instance identifier
     * @param ip instance IP address
     */
    public DefaultControllerNode(NodeId id, IpPrefix ip) {
        this.id = id;
        this.ip = ip;
    }

    @Override
    public NodeId id() {
        return id;
    }

    @Override
    public IpPrefix ip() {
        return ip;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof DefaultControllerNode) {
            DefaultControllerNode that = (DefaultControllerNode) o;
            return Objects.equals(this.id, that.id);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("id", id).add("ip", ip).toString();
    }

}
