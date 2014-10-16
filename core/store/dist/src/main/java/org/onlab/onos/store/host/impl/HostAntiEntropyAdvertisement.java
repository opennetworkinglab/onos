package org.onlab.onos.store.host.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.HostId;
import org.onlab.onos.store.Timestamp;

/**
 * Host AE Advertisement message.
 */
public final class HostAntiEntropyAdvertisement {

    private final NodeId sender;
    private final Map<HostFragmentId, Timestamp> timestamps;
    private final Map<HostId, Timestamp> tombstones;


    public HostAntiEntropyAdvertisement(NodeId sender,
                Map<HostFragmentId, Timestamp> timestamps,
                Map<HostId, Timestamp> tombstones) {
        this.sender = checkNotNull(sender);
        this.timestamps = checkNotNull(timestamps);
        this.tombstones = checkNotNull(tombstones);
    }

    public NodeId sender() {
        return sender;
    }

    public Map<HostFragmentId, Timestamp> timestamps() {
        return timestamps;
    }

    public Map<HostId, Timestamp> tombstones() {
        return tombstones;
    }

    // For serializer
    @SuppressWarnings("unused")
    private HostAntiEntropyAdvertisement() {
        this.sender = null;
        this.timestamps = null;
        this.tombstones = null;
    }
}
