package org.onlab.onos.store.link.impl;

import org.onlab.onos.net.LinkKey;
import org.onlab.onos.store.Timestamp;

import com.google.common.base.MoreObjects;

/**
 * Information published by GossipLinkStore to notify peers of a link
 * being removed.
 */
public class InternalLinkRemovedEvent {

    private final LinkKey linkKey;
    private final Timestamp timestamp;

    /**
     * Creates a InternalLinkRemovedEvent.
     * @param linkKey identifier of the removed link.
     * @param timestamp timestamp of when the link was removed.
     */
    public InternalLinkRemovedEvent(LinkKey linkKey, Timestamp timestamp) {
        this.linkKey = linkKey;
        this.timestamp = timestamp;
    }

    public LinkKey linkKey() {
        return linkKey;
    }

    public Timestamp timestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("linkKey", linkKey)
                .add("timestamp", timestamp)
                .toString();
    }

    // for serializer
    @SuppressWarnings("unused")
    private InternalLinkRemovedEvent() {
        linkKey = null;
        timestamp = null;
    }
}