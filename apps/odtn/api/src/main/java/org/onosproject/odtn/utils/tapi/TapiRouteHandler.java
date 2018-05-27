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
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20180307.tapiconnectivity.connection.DefaultRoute;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20180307.tapiconnectivity.connectivitycontext.ConnectionKeys;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20180307.tapiconnectivity.connectivitycontext.DefaultConnection;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20180307.tapiconnectivity.route.ConnectionEndPoint;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20180307.tapiconnectivity.route.DefaultConnectionEndPoint;
import org.onosproject.yang.model.ModelObjectId;

import static org.onosproject.odtn.utils.tapi.TapiLocalClassUtil.getLocalId;
import static org.onosproject.odtn.utils.tapi.TapiLocalClassUtil.setLocalId;

/**
 * Utility class to deal with TAPI Route with DCS.
 */
public final class TapiRouteHandler extends TapiObjectHandler<DefaultRoute> {

    private Uuid connectionId;

    private TapiRouteHandler() {
        obj = new DefaultRoute();
        setId();
    }

    public static TapiRouteHandler create() {
        return new TapiRouteHandler();
    }

    @Override
    protected Uuid getIdDetail() {
        return Uuid.fromString(getLocalId(obj));
    }

    @Override
    protected void setIdDetail(Uuid uuid) {
        setLocalId(obj, uuid.toString());
    }

    @Override
    public ModelObjectId getParentModelObjectId() {
        ConnectionKeys connectionKeys = new ConnectionKeys();
        connectionKeys.uuid(connectionId);

        return ModelObjectId.builder()
                .addChild(DefaultContext.class)
                .addChild(DefaultConnection.class, connectionKeys)
                .build();
    }

    public TapiRouteHandler addCep(TapiCepRef cepRef) {
        DefaultConnectionEndPoint cep = new DefaultConnectionEndPoint();
        cep.topologyId(cepRef.getTopologyId());
        cep.nodeId(cepRef.getNodeId());
        cep.ownedNodeEdgePointId(cepRef.getNepId());
        cep.connectionEndPointId(cepRef.getCepId());

        obj.addToConnectionEndPoint(cep);
        return this;
    }

    public TapiCepRef getRouteStart() {
        ConnectionEndPoint cep = obj.connectionEndPoint().get(0);
        return TapiCepRef.create(cep.topologyId().toString(), cep.nodeId().toString(),
                cep.ownedNodeEdgePointId().toString(), cep.connectionEndPointId().toString());
    }

    public TapiCepRef getRouteEnd() {
        ConnectionEndPoint cep = obj.connectionEndPoint().get(obj.connectionEndPoint().size() - 1);
        return TapiCepRef.create(cep.topologyId().toString(), cep.nodeId().toString(),
                cep.ownedNodeEdgePointId().toString(), cep.connectionEndPointId().toString());
    }

    public TapiRouteHandler setConnectionId(Uuid connectionId) {
        this.connectionId = connectionId;
        return this;
    }
}
