package org.onosproject.store.link.impl;

import com.google.common.base.MoreObjects;
import org.onosproject.net.link.LinkDescription;
import org.onosproject.net.provider.ProviderId;

public class LinkInjectedEvent {

    ProviderId providerId;
    LinkDescription linkDescription;

    public LinkInjectedEvent(ProviderId providerId, LinkDescription linkDescription) {
        this.providerId = providerId;
        this.linkDescription = linkDescription;
    }

    public ProviderId providerId() {
        return providerId;
    }

    public LinkDescription linkDescription() {
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
    protected LinkInjectedEvent() {
        this.providerId = null;
        this.linkDescription = null;
    }
}
