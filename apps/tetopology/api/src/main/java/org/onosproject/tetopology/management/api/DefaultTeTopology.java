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
package org.onosproject.tetopology.management.api;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.onosproject.net.DeviceId;
import org.onosproject.tetopology.management.api.link.TeLink;
import org.onosproject.tetopology.management.api.link.TeLinkTpKey;
import org.onosproject.tetopology.management.api.node.TeNode;

import java.util.BitSet;
import java.util.Map;

/**
 * Default implementation of TeTopology.
 */
public class DefaultTeTopology implements TeTopology {
    private final TeTopologyKey teKey;
    private final Map<Long, TeNode> teNodes;
    private final Map<TeLinkTpKey, TeLink> teLinks;
    private final String idString;
    private final CommonTopologyData common;

    /**
     * Creates an instance of DefaultTeTopology.
     *
     * @param teKey    the TE topology key used for searching
     * @param teNodes  the list of TE nodes in the topology
     * @param teLinks  the list of TE links in the topology
     * @param idString the TE Topology id string value
     * @param common   the common topology attributes
     */
    public DefaultTeTopology(TeTopologyKey teKey, Map<Long, TeNode> teNodes,
                             Map<TeLinkTpKey, TeLink> teLinks, String idString,
                             CommonTopologyData common) {
        this.teKey = teKey;
        this.teNodes = teNodes != null ? Maps.newHashMap(teNodes) : null;
        this.teLinks = teLinks != null ? Maps.newHashMap(teLinks) : null;
        this.idString = idString;
        this.common = common;
    }

    @Override
    public TeTopologyKey teTopologyId() {
        return teKey;
    }

    @Override
    public BitSet flags() {
        if (common == null) {
            return null;
        }
        return common.flags();
    }

    @Override
    public OptimizationType optimization() {
        if (common == null) {
            return null;
        }
        return common.optimization();
    }

    @Override
    public Map<Long, TeNode> teNodes() {
        if (teNodes == null) {
            return null;
        }
        return ImmutableMap.copyOf(teNodes);
    }

    @Override
    public TeNode teNode(long teNodeId) {
        return teNodes.get(teNodeId);
    }

    @Override
    public Map<TeLinkTpKey, TeLink> teLinks() {
        if (teLinks == null) {
            return null;
        }
        return ImmutableMap.copyOf(teLinks);
    }

    @Override
    public TeLink teLink(TeLinkTpKey teLinkId) {
        return teLinks.get(teLinkId);
    }

    @Override
    public String teTopologyIdStringValue() {
        return idString;
    }

    @Override
    public KeyId networkId() {
        if (common == null) {
            return null;
        }
        return common.networkId();
    }

    @Override
    public DeviceId ownerId() {
        if (common == null) {
            return null;
        }
        return common.ownerId();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(teKey, teNodes,
                                teLinks, common, idString);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object instanceof DefaultTeTopology) {
            DefaultTeTopology that = (DefaultTeTopology) object;
            return Objects.equal(teKey, that.teKey) &&
                    Objects.equal(teNodes, that.teNodes) &&
                    Objects.equal(teLinks, that.teLinks) &&
                    Objects.equal(common, that.common) &&
                    Objects.equal(idString, that.idString);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("teKey", teKey)
                .add("teNodes", teNodes)
                .add("teLinks", teLinks)
                .add("common", common)
                .add("idString", idString)
                .toString();
    }

}
