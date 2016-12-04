/*
 * Copyright 2016-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.tetopology.management.impl;

import java.util.List;

import org.onosproject.tetopology.management.api.link.NetworkLink;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.tetopology.management.api.link.TeLinkTpGlobalKey;
import org.onosproject.tetopology.management.api.node.NodeTpKey;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Network Link representation in store.
 */
public class InternalNetworkLink {
    private NodeTpKey source;
    private NodeTpKey destination;
    private List<NetworkLinkKey> supportingLinkIds;
    private TeLinkTpGlobalKey teLinkKey;
    private boolean parentUpdate;

    /**
     * Creates an instance of InternalNetworkLink.
     *
     * @param link the network link
     * @param parentUpdate the flag if the data is updated by parent
     */
    public InternalNetworkLink(NetworkLink link,  boolean parentUpdate) {
        source = link.source();
        destination = link.destination();
        supportingLinkIds = link
                .supportingLinkIds() == null ? null
                                             : Lists.newArrayList(link
                                                     .supportingLinkIds());
        this.parentUpdate = parentUpdate;
    }

    /**
     * Returns the link source termination point.
     *
     * @return source link termination point id
     */
    public NodeTpKey source() {
        return source;
    }

    /**
     * Returns the link destination termination point.
     *
     * @return destination link termination point id
     */
    public NodeTpKey destination() {
        return destination;
    }

    /**
     * Returns the supporting link ids.
     *
     * @return list of the ids of the supporting links
     */
    public List<NetworkLinkKey> supportingLinkIds() {
        return supportingLinkIds == null ? null
                                         : ImmutableList
                                                 .copyOf(supportingLinkIds);
    }

    /**
     * Returns the TE link key.
     *
     * @return the teLinkKey
     */
    public TeLinkTpGlobalKey teLinkKey() {
        return teLinkKey;
    }

    /**
     * Sets the TE link key.
     *
     * @param teLinkKey the teLinkKey to set
     */
    public void setTeLinkKey(TeLinkTpGlobalKey teLinkKey) {
        this.teLinkKey = teLinkKey;
    }

    /**
     * Returns the flag if the data was updated by parent change.
     *
     * @return value of parentUpdate
     */
    public boolean parentUpdate() {
        return parentUpdate;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(source, destination, supportingLinkIds, teLinkKey);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof InternalNetworkLink) {
            InternalNetworkLink that = (InternalNetworkLink) object;
            return Objects.equal(source, that.source)
                    && Objects.equal(destination, that.destination)
                    && Objects.equal(supportingLinkIds, that.supportingLinkIds)
                    && Objects.equal(teLinkKey, that.teLinkKey);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("source", source)
                .add("destination", destination)
                .add("supportingLinkIds", supportingLinkIds)
                .add("teLinkKey", teLinkKey)
                .toString();
    }
}
