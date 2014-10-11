package org.onlab.onos.store.link.impl;

import java.util.Objects;

import org.onlab.onos.net.LinkKey;
import org.onlab.onos.net.provider.ProviderId;

import com.google.common.base.MoreObjects;

/**
 * Identifier for LinkDescription from a Provider.
 */
public final class LinkFragmentId {
    public final ProviderId providerId;
    public final LinkKey linkKey;

    public LinkFragmentId(LinkKey linkKey, ProviderId providerId) {
        this.providerId = providerId;
        this.linkKey = linkKey;
    }

    public LinkKey linkKey() {
        return linkKey;
    }

    public ProviderId providerId() {
        return providerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId, linkKey);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LinkFragmentId)) {
            return false;
        }
        LinkFragmentId that = (LinkFragmentId) obj;
        return Objects.equals(this.linkKey, that.linkKey) &&
               Objects.equals(this.providerId, that.providerId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("providerId", providerId)
                .add("linkKey", linkKey)
                .toString();
    }

    // for serializer
    @SuppressWarnings("unused")
    private LinkFragmentId() {
        this.providerId = null;
        this.linkKey = null;
    }
}
