package org.onlab.onos.store.link.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.onlab.onos.cluster.NodeId;
import org.onlab.onos.net.LinkKey;
import org.onlab.onos.store.Timestamp;

/**
 * Link AE Advertisement message.
 */
public class LinkAntiEntropyAdvertisement {

    private final NodeId sender;
    private final Map<LinkFragmentId, Timestamp> linkTimestamps;
    private final Map<LinkKey, Timestamp> linkTombstones;


    public LinkAntiEntropyAdvertisement(NodeId sender,
                Map<LinkFragmentId, Timestamp> linkTimestamps,
                Map<LinkKey, Timestamp> linkTombstones) {
        this.sender = checkNotNull(sender);
        this.linkTimestamps = checkNotNull(linkTimestamps);
        this.linkTombstones = checkNotNull(linkTombstones);
    }

    public NodeId sender() {
        return sender;
    }

    public Map<LinkFragmentId, Timestamp> linkTimestamps() {
        return linkTimestamps;
    }

    public Map<LinkKey, Timestamp> linkTombstones() {
        return linkTombstones;
    }

    // For serializer
    @SuppressWarnings("unused")
    private LinkAntiEntropyAdvertisement() {
        this.sender = null;
        this.linkTimestamps = null;
        this.linkTombstones = null;
    }
}
