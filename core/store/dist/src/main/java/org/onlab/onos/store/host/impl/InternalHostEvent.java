package org.onlab.onos.store.host.impl;

import org.onlab.onos.net.HostId;
import org.onlab.onos.net.host.HostDescription;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.Timestamp;

/**
 * Information published by GossipHostStore to notify peers of a host
 * change (create/update) event.
 */
public class InternalHostEvent {

    private final ProviderId providerId;
    private final HostId hostId;
    private final HostDescription hostDescription;
    private final Timestamp timestamp;

    public InternalHostEvent(ProviderId providerId, HostId hostId,
                             HostDescription hostDescription, Timestamp timestamp) {
        this.providerId = providerId;
        this.hostId = hostId;
        this.hostDescription = hostDescription;
        this.timestamp = timestamp;
    }

    public ProviderId providerId() {
        return providerId;
    }

    public HostId hostId() {
        return hostId;
    }

    public HostDescription hostDescription() {
        return hostDescription;
    }

    public Timestamp timestamp() {
        return timestamp;
    }

    // Needed for serialization.
    @SuppressWarnings("unused")
    private InternalHostEvent() {
        providerId = null;
        hostId = null;
        hostDescription = null;
        timestamp = null;
    }
}
