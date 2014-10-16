package org.onlab.onos.store.host.impl;

import java.util.Objects;

import org.onlab.onos.net.HostId;
import org.onlab.onos.net.provider.ProviderId;

import com.google.common.base.MoreObjects;

/**
 * Identifier for HostDescription from a Provider.
 */
public final class HostFragmentId {
    public final ProviderId providerId;
    public final HostId hostId;

    public HostFragmentId(HostId hostId, ProviderId providerId) {
        this.providerId = providerId;
        this.hostId = hostId;
    }

    public HostId hostId() {
        return hostId;
    }

    public ProviderId providerId() {
        return providerId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(providerId, hostId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HostFragmentId)) {
            return false;
        }
        HostFragmentId that = (HostFragmentId) obj;
        return Objects.equals(this.hostId, that.hostId) &&
               Objects.equals(this.providerId, that.providerId);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("providerId", providerId)
                .add("hostId", hostId)
                .toString();
    }

    // for serializer
    @SuppressWarnings("unused")
    private HostFragmentId() {
        this.providerId = null;
        this.hostId = null;
    }
}
