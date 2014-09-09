package org.onlab.onos.net.topology;

import org.onlab.onos.net.DeviceId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default implementation of a network topology cluster.
 */
public class DefaultTopologyCluster implements TopologyCluster {

    private final ClusterId id;
    private final int deviceCount;
    private final int linkCount;
    private final DeviceId root;

    /**
     * Creates a new topology cluster descriptor with the specified attributes.
     *
     * @param id          cluster id
     * @param deviceCount number of devices in the cluster
     * @param linkCount   number of links in the cluster
     * @param root        cluster root node
     */
    public DefaultTopologyCluster(ClusterId id, int deviceCount, int linkCount,
                                  DeviceId root) {
        this.id = id;
        this.deviceCount = deviceCount;
        this.linkCount = linkCount;
        this.root = root;
    }

    @Override
    public ClusterId id() {
        return id;
    }

    @Override
    public int deviceCount() {
        return deviceCount;
    }

    @Override
    public int linkCount() {
        return linkCount;
    }

    @Override
    public DeviceId root() {
        return root;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, deviceCount, linkCount, root);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DefaultTopologyCluster) {
            final DefaultTopologyCluster other = (DefaultTopologyCluster) obj;
            return Objects.equals(this.id, other.id) &&
                    Objects.equals(this.deviceCount, other.deviceCount) &&
                    Objects.equals(this.linkCount, other.linkCount) &&
                    Objects.equals(this.root, other.root);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("deviceCount", deviceCount)
                .add("linkCount", linkCount)
                .add("root", root)
                .toString();
    }
}
