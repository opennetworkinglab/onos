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

import java.util.List;

import org.onosproject.tetopology.management.api.KeyId;
import org.onosproject.tetopology.management.api.node.TerminationPointKey;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * NetworkLink implementation.
 * <p>
 * The Set/Get methods below are defined to accept and pass references because
 * the object class is treated as a "composite" object class that holds
 * references to various member objects and their relationships, forming a
 * data tree. Internal routines of the TE topology manager may use the
 * following example methods to construct and manipulate any piece of data in
 * the data tree:
 *<pre>
 * newNode.getTe().setAdminStatus(), or
 * newNode.getSupportingNodeIds().add(nodeId), etc.
 *</pre>
 * Same for constructors where, for example, a child list may be constructed
 * first and passed in by reference to its parent object constructor.
 */
public class DefaultNetworkLink implements NetworkLink {
    private final KeyId linkId;
    private TerminationPointKey source;
    private TerminationPointKey destination;
    private List<NetworkLinkKey> supportingLinkIds;
    private TeLink te;

    /**
     * Creates an instance of DefaultNetworkLink.
     *
     * @param linkId link identifier
     */
    public DefaultNetworkLink(KeyId linkId) {
        this.linkId = linkId;
    }

    /**
     * Sets the link source point.
     *
     * @param source the source to set
     */
    public void setSource(TerminationPointKey source) {
        this.source = source;
    }

    /**
     * Sets the link destination point.
     *
     * @param destination the destination to set
     */
    public void setDestination(TerminationPointKey destination) {
        this.destination = destination;
    }

    /**
     * Sets the supporting link Ids.
     *
     * @param supportingLinkIds the supportingLinkIds to set
     */
    public void setSupportingLinkIds(List<NetworkLinkKey> supportingLinkIds) {
        this.supportingLinkIds = supportingLinkIds;
    }

    /**
     * Sets the te extension.
     *
     * @param te the te to set
     */
    public void setTe(TeLink te) {
        this.te = te;
    }

    @Override
    public KeyId linkId() {
        return linkId;
    }

    @Override
    public TerminationPointKey getSource() {
        return source;
    }

    @Override
    public TerminationPointKey getDestination() {
        return destination;
    }
    @Override
    public List<NetworkLinkKey> getSupportingLinkIds() {
        return supportingLinkIds;
    }

    @Override
    public TeLink getTe() {
        return te;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(linkId, source, destination, supportingLinkIds, te);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof DefaultNetworkLink) {
            DefaultNetworkLink that = (DefaultNetworkLink) object;
            return Objects.equal(this.linkId, that.linkId) &&
                    Objects.equal(this.source, that.source) &&
                    Objects.equal(this.destination, that.destination) &&
                    Objects.equal(this.supportingLinkIds, that.supportingLinkIds) &&
                    Objects.equal(this.te, that.te);
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
                .add("te", te)
                .toString();
    }

}
