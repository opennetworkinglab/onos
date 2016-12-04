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

import static org.onosproject.tetopology.management.api.TeConstants.NIL_LONG_VALUE;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.onosproject.tetopology.management.api.CommonTopologyData;
import org.onosproject.tetopology.management.api.TeTopology;
import org.onosproject.tetopology.management.api.link.TeLink;
import org.onosproject.tetopology.management.api.link.TeLinkTpGlobalKey;
import org.onosproject.tetopology.management.api.link.TeLinkTpKey;
import org.onosproject.tetopology.management.api.node.TeNode;
import org.onosproject.tetopology.management.api.node.TeNodeKey;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

/**
 * TE topology representation in store.
 */
public class InternalTeTopology {
    private String teTopologyId;
    private List<TeNodeKey> teNodeKeys;
    private List<TeLinkTpGlobalKey> teLinkKeys;
    private CommonTopologyData topologyData;
    private long nextTeNodeId = NIL_LONG_VALUE;
    private boolean childUpdate;

    /**
     * Creates an instance of InternalTeTopology.
     *
     * @param teTopology the TE Topology object
     */
    public InternalTeTopology(TeTopology teTopology) {
        this.teTopologyId = teTopology.teTopologyIdStringValue();
        this.topologyData = new CommonTopologyData(teTopology);
        // teNodeKeys
        if (MapUtils.isNotEmpty(teTopology.teNodes())) {
            this.teNodeKeys = Lists.newArrayList();
            for (Map.Entry<Long, TeNode> entry : teTopology.teNodes().entrySet()) {
                this.teNodeKeys.add(new TeNodeKey(teTopology.teTopologyId(), entry.getKey()));
            }
        }
        // teLink Keys
        if (MapUtils.isNotEmpty(teTopology.teLinks())) {
            this.teLinkKeys = Lists.newArrayList();
            for (Map.Entry<TeLinkTpKey, TeLink> entry : teTopology.teLinks().entrySet()) {
                this.teLinkKeys.add(new TeLinkTpGlobalKey(teTopology.teTopologyId(), entry.getKey()));
            }
        }
    }

    /**
     * Creates a default instance of InternalNetwork.
     *
     * @param teTopologyId string value of id
     */
    public InternalTeTopology(String teTopologyId) {
        this.teTopologyId = teTopologyId;
    }

    /**
     * Returns the TE Topology Id string value.
     *
     * @return the teTopologyId
     */
    public String teTopologyId() {
        return teTopologyId;
    }

    /**
     * Returns the list of TE node keys in the topology.
     *
     * @return the teNodeKeys
     */
    public List<TeNodeKey> teNodeKeys() {
        return teNodeKeys;
    }

    /**
     * Sets the list of TE node keys.
     *
     * @param teNodeKeys the teNodeKeys to set
     */
    public void setTeNodeKeys(List<TeNodeKey> teNodeKeys) {
        this.teNodeKeys = teNodeKeys;
    }

    /**
     * Returns the list of TE link keys in the topology.
     *
     * @return the teLinkKeys
     */
    public List<TeLinkTpGlobalKey> teLinkKeys() {
        return teLinkKeys;
    }

    /**
     * Sets the list of TE link keys.
     *
     * @param teLinkKeys the teLinkKeys to set
     */
    public void setTeLinkKeys(List<TeLinkTpGlobalKey> teLinkKeys) {
        this.teLinkKeys = teLinkKeys;
    }

    /**
     * Returns the common TE topology data.
     *
     * @return the topology data
     */
    public CommonTopologyData topologyData() {
        return topologyData;
    }

    /**
     * Sets the common TE topology data.
     *
     * @param topologyData the topologyData to set
     */
    public void setTopologydata(CommonTopologyData topologyData) {
        this.topologyData = topologyData;
    }

    /**
     * Returns the next available TE node Id.
     *
     * @return the next TE nodeId
     */
    public long nextTeNodeId() {
        return nextTeNodeId;
    }

    /**
     * Sets the next available TE node Id.
     *
     * @param nextTeNodeId the nextTeNodeId to set
     */
    public void setNextTeNodeId(long nextTeNodeId) {
        this.nextTeNodeId = nextTeNodeId;
    }

    /**
     * Returns the flag if the data was updated by child change.
     *
     * @return value of childUpdate
     */
    public boolean childUpdate() {
        return childUpdate;
    }

    /**
     * Sets the flag if the data was updated by child change.
     *
     * @param childUpdate the childUpdate value to set
     */
    public void setChildUpdate(boolean childUpdate) {
        this.childUpdate = childUpdate;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(teTopologyId, teNodeKeys, teLinkKeys,
                topologyData);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof InternalTeTopology) {
            InternalTeTopology that = (InternalTeTopology) object;
            return Objects.equal(teTopologyId, that.teTopologyId)
                    && Objects.equal(teNodeKeys, that.teNodeKeys)
                    && Objects.equal(teLinkKeys, that.teLinkKeys)
                    && Objects.equal(topologyData, that.topologyData);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("teTopologyId", teTopologyId)
                .add("teNodeKeys", teNodeKeys)
                .add("teLinkKeys", teLinkKeys)
                .add("topologyData", topologyData)
                .add("nextTeNodeId", nextTeNodeId)
                .add("childUpdate", childUpdate)
                .toString();
    }
}
