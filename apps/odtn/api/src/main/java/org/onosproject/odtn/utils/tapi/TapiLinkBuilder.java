/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.odtn.utils.tapi;

import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.Uuid;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.link.DefaultNodeEdgePoint;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.topology.DefaultLink;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.topologycontext.DefaultTopology;
import org.onosproject.yang.gen.v1.tapitopology.rev20180307.tapitopology.topologycontext.TopologyKeys;
import org.onosproject.yang.model.ModelObjectId;

import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.setUuid;

/**
 * Utility builder class for TAPI link creation with DCS.
 */
public final class TapiLinkBuilder extends TapiInstanceBuilder {

    private Uuid topologyUuid;
    private DefaultLink link = new DefaultLink();

    private TapiLinkBuilder() {
        setUuid(link);
    }

    public static TapiLinkBuilder builder() {
        return new TapiLinkBuilder();
    }

    public TapiLinkBuilder setTopologyUuid(Uuid topologyUuid) {
        this.topologyUuid = topologyUuid;
        return this;
    }

    public TapiLinkBuilder addNep(TapiNepRef nepRef) {
        DefaultNodeEdgePoint nep = new DefaultNodeEdgePoint();
        nep.topologyId(nepRef.getTopologyId());
        nep.nodeId(nepRef.getNodeId());
        nep.ownedNodeEdgePointId(nepRef.getNepId());
        link.addToNodeEdgePoint(nep);
        return this;
    }

    @Override
    public ModelObjectId getModelObjectId() {

        TopologyKeys topologyKey = new TopologyKeys();
        topologyKey.uuid(topologyUuid);
        return ModelObjectId.builder()
                .addChild(DefaultContext.class)
                .addChild(DefaultTopology.class, topologyKey)
                .build();
    }

    @Override
    @SuppressWarnings("unchecked")
    public DefaultLink getModelObject() {
        return link;
    }

    @Override
    public Uuid getUuid() {
        return link.uuid();
    }

}
