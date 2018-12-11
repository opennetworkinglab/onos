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
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.ceplist.DefaultConnectionEndPoint;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connectionendpoint.DefaultParentNodeEdgePoint;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.context.augmentedtapicommoncontext.DefaultTopologyContext;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.node.DefaultOwnedNodeEdgePoint;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.node.OwnedNodeEdgePointKeys;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topology.DefaultNode;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topology.NodeKeys;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topologycontext.DefaultTopology;
import org.onosproject.yang.gen.v1.tapitopology.rev20181210.tapitopology.topologycontext.TopologyKeys;
import org.onosproject.yang.model.ModelObjectId;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.getUuid;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.setUuid;

/**
 * Utility class to deal with TAPI CEP with DCS.
 */
public final class TapiCepHandler extends TapiObjectHandler<DefaultConnectionEndPoint> {

    private Uuid topologyUuid;
    private Uuid nodeUuid;
    private Uuid nepUuid;

    private TapiCepHandler() {
        obj = new DefaultConnectionEndPoint();
        setId();
    }

    public static TapiCepHandler create() {
        return new TapiCepHandler();
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
        checkNotNull(nodeUuid);
        checkNotNull(nodeUuid);

        TopologyKeys topologyKey = new TopologyKeys();
        topologyKey.uuid(topologyUuid);

        NodeKeys nodeKey = new NodeKeys();
        nodeKey.uuid(nodeUuid);

        OwnedNodeEdgePointKeys nepKey = new OwnedNodeEdgePointKeys();
        nepKey.uuid(nepUuid);

        return ModelObjectId.builder()
                .addChild(DefaultContext.class)
                .addChild(DefaultTopologyContext.class)
                .addChild(DefaultTopology.class, topologyKey)
                .addChild(DefaultNode.class, nodeKey)
                .addChild(DefaultOwnedNodeEdgePoint.class, nepKey)
                .build();
    }

    public TapiCepHandler setTopologyUuid(Uuid topologyUuid) {
        this.topologyUuid = topologyUuid;
        return this;
    }

    public TapiCepHandler setNodeUuid(Uuid nodeUuid) {
        this.nodeUuid = nodeUuid;
        return this;
    }

    public TapiCepHandler setNepUuid(Uuid nepUuid) {
        this.nepUuid = nepUuid;
        return this;
    }

    public TapiCepHandler setParentNep() {
        checkNotNull(topologyUuid);
        checkNotNull(nodeUuid);
        checkNotNull(nepUuid);

        DefaultParentNodeEdgePoint parentNep = new DefaultParentNodeEdgePoint();
        parentNep.topologyUuid(topologyUuid);
        parentNep.nodeUuid(nodeUuid);
        parentNep.nodeEdgePointUuid(nepUuid);
        obj.parentNodeEdgePoint(parentNep);

        return this;
    }

    public TapiCepHandler setParentNep(TapiNepRef nepRef) {
        topologyUuid = Uuid.fromString(nepRef.getTopologyId());
        nodeUuid = Uuid.fromString(nepRef.getNodeId());
        nepUuid = Uuid.fromString(nepRef.getNepId());

        setParentNep();
        return this;
    }
}
