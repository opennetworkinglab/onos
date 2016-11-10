/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.api.link;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.node.NodeTpKey;

import java.util.List;

/**
 * Default implementation of a network link.
 */
public class DefaultNetworkLink implements NetworkLink {
    private final KeyId linkId;
    private final NodeTpKey source;
    private final NodeTpKey destination;
    private final List<NetworkLinkKey> supportingLinkIds;
    private final TeLink teLink;

    /**
     * Creates an instance of a network link.
     *
     * @param linkId            link identifier
     * @param source            source of termination point
     * @param destination       destination termination point
     * @param supportingLinkIds supporting links
     * @param teLink            TE link which this network link maps to
     */
    public DefaultNetworkLink(KeyId linkId,
                              NodeTpKey source,
                              NodeTpKey destination,
                              List<NetworkLinkKey> supportingLinkIds,
                              TeLink teLink) {
        this.linkId = linkId;
        this.source = source;
        this.destination = destination;
        this.supportingLinkIds = supportingLinkIds != null ?
                Lists.newArrayList(supportingLinkIds) : null;
        this.teLink = teLink;
    }

    @Override
    public KeyId linkId() {
        return linkId;
    }

    @Override
    public NodeTpKey source() {
        return source;
    }

    @Override
    public NodeTpKey destination() {
        return destination;
    }

    @Override
    public List<NetworkLinkKey> supportingLinkIds() {
        if (supportingLinkIds == null) {
            return null;
        }
        return ImmutableList.copyOf(supportingLinkIds);
    }

    @Override
    public TeLink teLink() {
        return teLink;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(linkId, source, destination,
                                supportingLinkIds, teLink);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof DefaultNetworkLink) {
            DefaultNetworkLink that = (DefaultNetworkLink) object;
            return Objects.equal(linkId, that.linkId) &&
                    Objects.equal(source, that.source) &&
                    Objects.equal(destination, that.destination) &&
                    Objects.equal(supportingLinkIds, that.supportingLinkIds) &&
                    Objects.equal(teLink, that.teLink);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("linkId", linkId)
                .add("source", source)
                .add("destination", destination)
                .add("supportingLinkIds", supportingLinkIds)
                .add("teLink", teLink)
                .toString();
    }

}
