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
package org.onosproject.tetopology.management.api.node;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onosproject.tetopology.management.api.TeStatus;
import org.onosproject.tetopology.management.api.TeTopologyKey;

import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The default implementation of TE Node.
 */
public class DefaultTeNode implements TeNode {
    private final long teNodeId;
    private final TeTopologyKey underlayTopologyId;
    private final TeNodeKey supportTeNodeId;
    private final TeNodeKey sourceTeNodeId;
    private final CommonNodeData teData;
    private final Map<Long, ConnectivityMatrix> connMatrices;
    private final List<Long> teLinkIds;
    private final Map<Long, TunnelTerminationPoint> ttps;
    private final List<Long> teTpIds;

    /**
     * Creates a TE node instance.
     *
     * @param teNodeId             TE node identifier
     * @param underlayTopologyIdId the node underlay TE topology id
     * @param supportTeNodeId      the supporting TE node id
     * @param sourceTeNodeId       the source TE node id
     * @param teData               the node common te data
     * @param connMatrices         the connectivity matrix table
     * @param teLinkIds            the list of TE link ids originating from the node
     * @param ttps                 the list of tunnel termination points
     * @param teTpIds              the currently known termination point ids
     */
    public DefaultTeNode(long teNodeId,
                         TeTopologyKey underlayTopologyIdId,
                         TeNodeKey supportTeNodeId,
                         TeNodeKey sourceTeNodeId,
                         CommonNodeData teData,
                         Map<Long, ConnectivityMatrix> connMatrices,
                         List<Long> teLinkIds,
                         Map<Long, TunnelTerminationPoint> ttps,
                         List<Long> teTpIds) {
        this.teNodeId = teNodeId;
        this.underlayTopologyId = underlayTopologyIdId;
        this.supportTeNodeId = supportTeNodeId;
        this.sourceTeNodeId = sourceTeNodeId;
        this.teData = teData;
        this.connMatrices = connMatrices != null ?
                Maps.newHashMap(connMatrices) : null;
        this.teLinkIds = teLinkIds != null ?
                Lists.newArrayList(teLinkIds) : null;
        this.ttps = ttps != null ? Maps.newHashMap(ttps) : null;
        this.teTpIds = teTpIds != null ?
                Lists.newArrayList(teTpIds) : null;
    }

    @Override
    public long teNodeId() {
        return teNodeId;
    }

    @Override
    public String name() {
        if (teData == null) {
            return null;
        }
        return teData.name();
    }

    @Override
    public BitSet flags() {
        if (teData == null) {
            return null;
        }
        return teData.flags();
    }

    @Override
    public TeTopologyKey underlayTeTopologyId() {
        return underlayTopologyId;
    }

    @Override
    public TeNodeKey supportingTeNodeId() {
        return supportTeNodeId;
    }

    @Override
    public TeNodeKey sourceTeNodeId() {
        return sourceTeNodeId;
    }

    @Override
    public Map<Long, ConnectivityMatrix> connectivityMatrices() {
        if (connMatrices == null) {
            return null;
        }
        return ImmutableMap.copyOf(connMatrices);
    }

    @Override
    public ConnectivityMatrix connectivityMatrix(long entryId) {
        return connMatrices.get(entryId);
    }

    @Override
    public List<Long> teLinkIds() {
        if (teLinkIds == null) {
            return null;
        }
        return ImmutableList.copyOf(teLinkIds);
    }

    @Override
    public Map<Long, TunnelTerminationPoint> tunnelTerminationPoints() {
        if (ttps == null) {
            return null;
        }
        return ImmutableMap.copyOf(ttps);
    }

    @Override
    public TunnelTerminationPoint tunnelTerminationPoint(long ttpId) {
        return ttps.get(ttpId);
    }

    @Override
    public TeStatus adminStatus() {
        if (teData == null) {
            return null;
        }
        return teData.adminStatus();
    }

    @Override
    public TeStatus opStatus() {
        if (teData == null) {
            return null;
        }
        return teData.opStatus();
    }

    @Override
    public List<Long> teTerminationPointIds() {
        return Collections.unmodifiableList(teTpIds);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(teNodeId, underlayTopologyId,
                                supportTeNodeId, sourceTeNodeId, teData,
                                connMatrices, teLinkIds, ttps, teTpIds);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof DefaultTeNode) {
            DefaultTeNode that = (DefaultTeNode) object;
            return Objects.equal(teNodeId, that.teNodeId) &&
                    Objects.equal(underlayTopologyId, that.underlayTopologyId) &&
                    Objects.equal(supportTeNodeId, that.supportTeNodeId) &&
                    Objects.equal(sourceTeNodeId, that.sourceTeNodeId) &&
                    Objects.equal(teData, that.teData) &&
                    Objects.equal(connMatrices, that.connMatrices) &&
                    Objects.equal(teLinkIds, that.teLinkIds) &&
                    Objects.equal(ttps, that.ttps) &&
                    Objects.equal(teTpIds, that.teTpIds);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("teNodeId", teNodeId)
                .add("underlayTopologyId", underlayTopologyId)
                .add("supportTeNodeId", supportTeNodeId)
                .add("sourceTeNodeId", sourceTeNodeId)
                .add("teData", teData)
                .add("connMatrices", connMatrices)
                .add("teLinkIds", teLinkIds)
                .add("ttps", ttps)
                .add("teTpIds", teTpIds)
                .toString();
    }
}
