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

import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.Uuid;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20180307.tapiconnectivity.ceplist.ConnectionEndPoint;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20180307.tapiconnectivity.connection.DefaultConnectionEndPoint;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20180307.tapiconnectivity.connectionendpoint.ParentNodeEdgePoint;
import org.onosproject.yang.model.ModelObjectId;

/**
 * Utility class to deal with TAPI CepRef with DCS.
 */
public final class TapiCepRefHandler extends TapiObjectHandler<DefaultConnectionEndPoint> {

    private TapiCepRefHandler() {
        obj = new DefaultConnectionEndPoint();
    }

    public static TapiCepRefHandler create() {
        return new TapiCepRefHandler();
    }

    @Override
    protected Uuid getIdDetail() {
        return (Uuid) obj.connectionEndPointId();
    }

    @Override
    protected void setIdDetail(Uuid uuid) {}

    @Override
    public ModelObjectId getParentModelObjectId() {
        return null;
    }

    public TapiCepRefHandler setCep(TapiCepRef cepRef) {
        obj.topologyId(cepRef.getTopologyId());
        obj.nodeId(cepRef.getNodeId());
        obj.ownedNodeEdgePointId(cepRef.getNepId());
        obj.connectionEndPointId(cepRef.getCepId());
        return this;
    }

    public TapiCepRefHandler setCep(ConnectionEndPoint cep) {
        obj.connectionEndPointId(cep.uuid());
        ParentNodeEdgePoint parentNep = cep.parentNodeEdgePoint().get(0);
        obj.topologyId(parentNep.topologyId());
        obj.nodeId(parentNep.nodeId());
        obj.ownedNodeEdgePointId(parentNep.ownedNodeEdgePointId());
        return this;
    }
}
