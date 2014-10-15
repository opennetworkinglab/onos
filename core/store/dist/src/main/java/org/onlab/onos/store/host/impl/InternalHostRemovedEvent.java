package org.onlab.onos.store.host.impl;

import org.onlab.onos.net.HostId;
import org.onlab.onos.store.Timestamp;

/**
 * Information published by GossipHostStore to notify peers of a host
 * removed event.
 */
public class InternalHostRemovedEvent {

    private final HostId hostId;
    private final Timestamp timestamp;

    public InternalHostRemovedEvent(HostId hostId, Timestamp timestamp) {
        this.hostId = hostId;
        this.timestamp = timestamp;
    }

    public HostId hostId() {
        return hostId;
    }

    public Timestamp timestamp() {
        return timestamp;
    }

    // for serialization.
    @SuppressWarnings("unused")
    private InternalHostRemovedEvent() {
        hostId = null;
        timestamp = null;
    }
}
