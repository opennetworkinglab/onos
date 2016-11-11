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
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.link.TeLinkTpGlobalKey;
import org.onosproject.tetopology.management.api.node.CommonNodeData;
import org.onosproject.tetopology.management.api.node.ConnectivityMatrix;
import org.onosproject.tetopology.management.api.node.ConnectivityMatrixKey;
import org.onosproject.tetopology.management.api.node.NetworkNodeKey;
import org.onosproject.tetopology.management.api.node.TeNode;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TtpKey;
import org.onosproject.tetopology.management.api.node.TunnelTerminationPoint;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

/**
 * The Node representation in store.
 */
public class InternalTeNode {
    private CommonNodeData teData;
    private TeTopologyKey underlayTopologyKey;
    private TeNodeKey supportNodeKey;
    private TeNodeKey sourceTeNodeKey;
    private List<ConnectivityMatrixKey> connMatrixKeys;
    private List<TeLinkTpGlobalKey> teLinkTpKeys;
    private List<TeLinkTpGlobalKey> teTpKeys;
    private List<TtpKey> ttpKeys;
    private NetworkNodeKey networkNodeKey;
    private boolean parentUpdate;
    private boolean childUpdate;

    // Next available TE link Id egressing from the TE node.
    private long nextTeLinkId;

    /**
     * Creates an instance of InternalTeNode.
     *
     * @param nodeKey the TE node key
     * @param node the TE node
     * @param networkNodeKey the network node key
     * @param parentUpdate the flag if the data is updated by parent
     */
    public InternalTeNode(TeNodeKey nodeKey, TeNode node,
           NetworkNodeKey networkNodeKey, boolean parentUpdate) {
        this.networkNodeKey = networkNodeKey;
        this.parentUpdate = parentUpdate;
        // Underlay topology
        this.underlayTopologyKey = node.underlayTeTopologyId();
        // Supporting topology
        this.supportNodeKey = node.supportingTeNodeId();
        // Source topology
        this.sourceTeNodeKey = node.sourceTeNodeId();
        // Common data
        this.teData = new CommonNodeData(node);
        // Connectivity matrix
        if (MapUtils.isNotEmpty(node.connectivityMatrices())) {
            this.connMatrixKeys = Lists.newArrayList();
            for (Map.Entry<Long, ConnectivityMatrix> entry : node.connectivityMatrices().entrySet()) {
                this.connMatrixKeys.add(new ConnectivityMatrixKey(nodeKey, entry.getKey()));
            }
        }
        // Tunnel termination point
        if (MapUtils.isNotEmpty(node.tunnelTerminationPoints())) {
            this.ttpKeys = Lists.newArrayList();
            for (Map.Entry<Long, TunnelTerminationPoint> entry : node.tunnelTerminationPoints().entrySet()) {
                this.ttpKeys.add(new TtpKey(nodeKey, entry.getKey()));
            }
        }
        // teLink Keys
        if (CollectionUtils.isNotEmpty(node.teLinkIds())) {
            this.teLinkTpKeys = Lists.newArrayList();
            for (Long linkId : node.teLinkIds()) {
                this.teLinkTpKeys.add(new TeLinkTpGlobalKey(nodeKey, linkId));
            }

        }
        // teTp Keys
        if (CollectionUtils.isNotEmpty(node.teTerminationPointIds())) {
            this.teTpKeys = Lists.newArrayList();
            for (Long tpId : node.teTerminationPointIds()) {
                this.teTpKeys.add(new TeLinkTpGlobalKey(nodeKey, tpId));
            }
        }
    }

    /**
     * Returns the node common data.
     *
     * @return the teData
     */
    public CommonNodeData teData() {
        return teData;
    }

    /**
     * Sets the node common data.
     *
     * @param teData the teData to set
     */
    public void setTeData(CommonNodeData teData) {
        this.teData = teData;
    }

    /**
     * Returns the node underlay topology key.
     *
     * @return the underlayTopologyKey
     */
    public TeTopologyKey underlayTopologyKey() {
        return underlayTopologyKey;
    }

    /**
     * Sets the node underlay topology key.
     *
     * @param underlayTopologyKey the underlayTopologyKey to set
     */
    public void setUnderlayTopologyKey(TeTopologyKey underlayTopologyKey) {
        this.underlayTopologyKey = underlayTopologyKey;
    }

    /**
     * Returns the supporting node key.
     *
     * @return the supportNodeKey
     */
    public TeNodeKey supportNodeKey() {
        return supportNodeKey;
    }

    /**
     * Sets the supporting node key.
     *
     * @param supportNodeKey the supportNodeKey to set
     */
    public void setSupportNodeKey(TeNodeKey supportNodeKey) {
        this.supportNodeKey = supportNodeKey;
    }

    /**
     * Returns the source node key.
     *
     * @return the sourceTeNodeKey
     */
    public TeNodeKey sourceTeNodeKey() {
        return sourceTeNodeKey;
    }

    /**
     * Sets the source node key.
     *
     * @param sourceTeNodeKey the sourceTeNodeKey to set
     */
    public void setSourceTeNodeKey(TeNodeKey sourceTeNodeKey) {
        this.sourceTeNodeKey = sourceTeNodeKey;
    }

    /**
     * Returns the node connect matrix keys.
     *
     * @return the connMatrixKeys
     */
    public List<ConnectivityMatrixKey> connMatrixKeys() {
        return connMatrixKeys;
    }

    /**
     * Sets the node connect matrix keys.
     *
     * @param connMatrixKeys the connMatrixKeys to set
     */
    public void setConnMatrixKeys(List<ConnectivityMatrixKey> connMatrixKeys) {
        this.connMatrixKeys = connMatrixKeys;
    }

    /**
     * Returns the TE link Ids.
     *
     * @return the teLinkTpKeys
     */
    public List<TeLinkTpGlobalKey> teLinkTpKeys() {
        return teLinkTpKeys;
    }

    /**
     * Sets the TE link Ids from the node.
     *
     * @param teLinkTpKeys the teLinkTpKeys to set
     */
    public void setTeLinkTpKeys(List<TeLinkTpGlobalKey> teLinkTpKeys) {
        this.teLinkTpKeys = teLinkTpKeys;
    }

    /**
     * Returns the TE termitation point Ids.
     *
     * @return the teTpKeys
     */
    public List<TeLinkTpGlobalKey> teTpKeys() {
        return teTpKeys;
    }

    /**
     * Sets the TE termitation point Ids.
     *
     * @param teTpKeys the teTpKeys to set
     */
    public void setTeTpKeys(List<TeLinkTpGlobalKey> teTpKeys) {
        this.teTpKeys = teTpKeys;
    }

    /**
     * Returns the list of Tunnel Termination Point keys of the node.
     *
     * @return the ttpKeys
     */
    public List<TtpKey> ttpKeys() {
        return ttpKeys;
    }

    /**
     * Sets the list of Tunnel Termination Point keys.
     *
     * @param ttpKeys the ttpKeys to set
     */
    public void setTtpKeys(List<TtpKey> ttpKeys) {
        this.ttpKeys = ttpKeys;
    }

    /**
     * Returns the network node Key.
     *
     * @return the networkNodeKey
     */
    public NetworkNodeKey networkNodeKey() {
        return networkNodeKey;
    }

    /**
     * Returns the next available TE link id from the node.
     *
     * @return the nextTeLinkId
     */
    public long nextTeLinkId() {
        return nextTeLinkId;
    }

    /**
     * Sets the next available TE link id.
     *
     * @param nextTeLinkId the nextTeLinkId to set
     */
    public void setNextTeLinkId(long nextTeLinkId) {
        this.nextTeLinkId = nextTeLinkId;
    }

    /**
     * Returns the flag if the data was updated by parent change.
     *
     * @return value of parentUpdate
     */
    public boolean parentUpdate() {
        return parentUpdate;
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
        return Objects.hashCode(teData, underlayTopologyKey, supportNodeKey,
                sourceTeNodeKey, connMatrixKeys, teLinkTpKeys, ttpKeys, networkNodeKey);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof InternalTeNode) {
            InternalTeNode that = (InternalTeNode) object;
            return Objects.equal(teData, that.teData)
                    && Objects.equal(underlayTopologyKey,
                                     that.underlayTopologyKey)
                    && Objects.equal(supportNodeKey, that.supportNodeKey)
                    && Objects.equal(sourceTeNodeKey, that.sourceTeNodeKey)
                    && Objects.equal(connMatrixKeys, that.connMatrixKeys)
                    && Objects.equal(teLinkTpKeys, that.teLinkTpKeys)
                    && Objects.equal(ttpKeys, that.ttpKeys)
                    && Objects.equal(networkNodeKey, that.networkNodeKey);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("teData", teData)
                .add("underlayTopologyKey", underlayTopologyKey)
                .add("supportNodeKey", supportNodeKey)
                .add("sourceTeNodeKey", sourceTeNodeKey)
                .add("connMatrixKeys", connMatrixKeys)
                .add("teLinkTpKeys", teLinkTpKeys)
                .add("ttpKeys", ttpKeys)
                .add("nextTeLinkId", nextTeLinkId)
                .add("networkNodeKey", networkNodeKey)
                .add("parentUpdate", parentUpdate)
                .add("childUpdate", childUpdate)
                .toString();
    }
}
