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

import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.Uuid;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.context.augmentedtapicommoncontext.DefaultTopologyContext;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.link.DefaultNodeEdgePoint;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topology.DefaultLink;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topologycontext.DefaultTopology;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topologycontext.TopologyKeys;
import org.onosproject.yang.model.ModelObjectId;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.getUuid;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.setUuid;

/**
 * Utility class to deal with TAPI Link with DCS.
 */
public final class TapiLinkHandler extends TapiObjectHandler<DefaultLink> {

    private Uuid topologyUuid;

    private TapiLinkHandler() {
        obj = new DefaultLink();
        setId();
    }

    public static TapiLinkHandler create() {
        return new TapiLinkHandler();
    }

    @Override
    protected Uuid getIdDetail() {
        return getUuid(obj);
    }

    @Override
    protected void setIdDetail(Uuid uuid) {
        setUuid(obj, uuid);
    }

    @Override
    public ModelObjectId getParentModelObjectId() {
        checkNotNull(topologyUuid);

        TopologyKeys topologyKey = new TopologyKeys();
        topologyKey.uuid(topologyUuid);
        return ModelObjectId.builder()
                .addChild(DefaultContext.class)
                .addChild(DefaultTopologyContext.class)
                .addChild(DefaultTopology.class, topologyKey)
                .build();
    }

    public TapiLinkHandler setTopologyUuid(Uuid topologyUuid) {
        this.topologyUuid = topologyUuid;
        return this;
    }

    public TapiLinkHandler addNep(TapiNepRef nepRef) {
        DefaultNodeEdgePoint nep = new DefaultNodeEdgePoint();
        nep.topologyUuid(nepRef.getTopologyId());
        nep.nodeUuid(nepRef.getNodeId());
        nep.nodeEdgePointUuid(nepRef.getNepId());
        obj.addToNodeEdgePoint(nep);
        return this;
    }

}
