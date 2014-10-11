package org.onlab.onos.store.link.impl;

import com.google.common.base.MoreObjects;

import org.onlab.onos.net.link.LinkDescription;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.store.common.impl.Timestamped;

/**
 * Information published by GossipDeviceStore to notify peers of a device
 * change event.
 */
public class InternalLinkEvent {

    private final ProviderId providerId;
    private final Timestamped<LinkDescription> linkDescription;

    protected InternalLinkEvent(
            ProviderId providerId,
            Timestamped<LinkDescription> linkDescription) {
        this.providerId = providerId;
        this.linkDescription = linkDescription;
    }

    public ProviderId providerId() {
        return providerId;
    }

    public Timestamped<LinkDescription> linkDescription() {
        return linkDescription;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("providerId", providerId)
                .add("linkDescription", linkDescription)
                .toString();
    }

    // for serializer
    protected InternalLinkEvent() {
        this.providerId = null;
        this.linkDescription = null;
    }
}
