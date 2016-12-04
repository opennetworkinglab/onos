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

import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.link.CommonLinkData;
import org.onosproject.tetopology.management.api.link.NetworkLinkKey;
import org.onosproject.tetopology.management.api.link.TeLink;
import org.onosproject.tetopology.management.api.link.TeLinkTpGlobalKey;
import org.onosproject.tetopology.management.api.link.TeLinkTpKey;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;

/**
 * The TE link representation in store.
 */
public class InternalTeLink {
    private TeLinkTpKey peerTeLinkKey;
    private TeTopologyKey underlayTopologyKey;
    private TeLinkTpGlobalKey supportingLinkKey;
    private TeLinkTpGlobalKey sourceTeLinkKey;
    private CommonLinkData teData;
    private NetworkLinkKey networkLinkKey;
    private boolean parentUpdate;

    /**
     * Creates an instance of InternalLink.
     *
     * @param link the TE link
     * @param parentUpdate indicator the TE node is updated by parent
     */
    public InternalTeLink(TeLink link, boolean parentUpdate) {
        this.parentUpdate = parentUpdate;
        // Peer link key
        this.peerTeLinkKey = link.peerTeLinkKey();
        // Underlay topology
        this.underlayTopologyKey = link.underlayTeTopologyId();
        // Supporting topology
        this.supportingLinkKey = link.supportingTeLinkId();
        // Source topology
        this.sourceTeLinkKey = link.sourceTeLinkId();
        // Common data
        this.teData = new CommonLinkData(link);
    }

    /**
     * Returns the bi-directional peer link key.
     *
     * @return the peerTeLinkKey
     */
    public TeLinkTpKey peerTeLinkKey() {
        return peerTeLinkKey;
    }

    /**
     * Sets the bi-directional peer link key.
     *
     * @param peerTeLinkKey the peerTeLinkKey to set
     */
    public void setPeerTeLinkKey(TeLinkTpKey peerTeLinkKey) {
        this.peerTeLinkKey = peerTeLinkKey;
    }

    /**
     * Returns the link underlay topology key.
     *
     * @return the underlayTopologyKey
     */
    public TeTopologyKey underlayTopologyKey() {
        return underlayTopologyKey;
    }

    /**
     * Sets the link underlay topology key.
     *
     * @param underlayTopologyKey the underlayTopologyKey to set
     */
    public void setUnderlayTopologyKey(TeTopologyKey underlayTopologyKey) {
        this.underlayTopologyKey = underlayTopologyKey;
    }

    /**
     * Returns the supporting link key.
     *
     * @return the supportingLinkKey
     */
    public TeLinkTpGlobalKey supportingLinkKey() {
        return supportingLinkKey;
    }

    /**
     * Sets the supporting link key.
     *
     * @param supportingLinkKey the supportingLinkKey to set
     */
    public void setSupportingLinkKey(TeLinkTpGlobalKey supportingLinkKey) {
        this.supportingLinkKey = supportingLinkKey;
    }

    /**
     * Returns the source link key.
     *
     * @return the sourceTeLinkKey
     */
    public TeLinkTpGlobalKey sourceTeLinkKey() {
        return sourceTeLinkKey;
    }

    /**
     * Sets the source link key.
     *
     * @param sourceTeLinkKey the sourceTeLinkKey to set
     */
    public void setSourceTeNodeKey(TeLinkTpGlobalKey sourceTeLinkKey) {
        this.sourceTeLinkKey = sourceTeLinkKey;
    }

    /**
     * Returns the link common data.
     *
     * @return the teData
     */
    public CommonLinkData teData() {
        return teData;
    }

    /**
     * Sets the link common data.
     *
     * @param teData the teData to set
     */
    public void setTeData(CommonLinkData teData) {
        this.teData = teData;
    }

    /**
     * Sets the network link key.
     *
     * @param networkLinkKey the networkLinkKey to set
     */
    public void setNetworkLinkKey(NetworkLinkKey networkLinkKey) {
        this.networkLinkKey = networkLinkKey;
    }

    /**
     * Returns the network link key.
     *
     * @return the networkLinkKey
     */
    public NetworkLinkKey networkLinkKey() {
        return networkLinkKey;
    }

    /**
     * Returns the indicator if the data was updated by parent.
     *
     * @return value of parentUpdate
     */
    public boolean parentUpdate() {
        return parentUpdate;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(peerTeLinkKey, underlayTopologyKey,
                supportingLinkKey, sourceTeLinkKey, teData, networkLinkKey);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof InternalTeLink) {
            InternalTeLink that = (InternalTeLink) object;
            return Objects.equal(peerTeLinkKey, that.peerTeLinkKey)
                    && Objects.equal(underlayTopologyKey,
                                     that.underlayTopologyKey)
                    && Objects.equal(supportingLinkKey, that.supportingLinkKey)
                    && Objects.equal(sourceTeLinkKey, that.sourceTeLinkKey)
                    && Objects.equal(networkLinkKey, that.networkLinkKey)
                    && Objects.equal(teData, that.teData);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("peerTeLinkKey", peerTeLinkKey)
                .add("underlayTopologyKey", underlayTopologyKey)
                .add("supportingLinkKey", supportingLinkKey)
                .add("sourceTeLinkKey", sourceTeLinkKey)
                .add("teData", teData)
                .add("networkLinkKey", networkLinkKey)
                .add("parentUpdate", parentUpdate)
                .toString();
    }
}
