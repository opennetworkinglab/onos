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

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.Uuid;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connection.ConnectionEndPoint;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connection.DefaultConnectionEndPoint;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connection.DefaultLowerConnection;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connection.DefaultRoute;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connection.LowerConnection;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connectivitycontext.ConnectionKeys;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connectivitycontext.DefaultConnection;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.context.augmentedtapicommoncontext.DefaultConnectivityContext;
import org.onosproject.yang.model.DefaultModelObjectData;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId;

import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.getUuid;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.setUuid;

/**
 * Utility class to deal with TAPI Connection with DCS.
 */
public final class TapiConnectionHandler extends TapiObjectHandler<DefaultConnection> {

    private TapiConnectionHandler() {
        obj = new DefaultConnection();
        setId();
    }

    public static TapiConnectionHandler create() {
        return new TapiConnectionHandler();
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
        return ModelObjectId.builder()
                .addChild(DefaultContext.class)
                .addChild(DefaultConnectivityContext.class)
                .build();
    }

    @Override
    public ModelObjectData getChildModelObjectData() {

        ConnectionKeys key = new ConnectionKeys();
        key.uuid(getId());

        DefaultConnectionEndPoint mObj = new DefaultConnectionEndPoint();

        ModelObjectId mId = ModelObjectId.builder()
                .addChild(DefaultContext.class)
                .addChild(DefaultConnectivityContext.class)
                .addChild(DefaultConnection.class, key)
                .build();

        return DefaultModelObjectData.builder()
                .addModelObject(mObj)
                .identifier(mId)
                .build();
    }

    public TapiConnectionHandler addCep(ConnectionEndPoint cep) {
        obj.addToConnectionEndPoint(cep);
        return this;
    }

    public TapiConnectionHandler addLowerConnection(DefaultConnection connection) {
        DefaultLowerConnection lowerConnection = new DefaultLowerConnection();
        lowerConnection.connectionUuid(connection.uuid());
        obj.addToLowerConnection(lowerConnection);
        return this;
    }

    public TapiConnectionHandler addRoute(DefaultRoute route) {
        obj.addToRoute(route);
        return this;
    }

    public List<ConnectionEndPoint> getCeps() {
        return obj.connectionEndPoint();
    }

    public List<TapiConnectionHandler> getLowerConnections() {

        try {
            return obj.lowerConnection().stream()
                    .map(LowerConnection::connectionUuid)
                    .map(id -> {
                        TapiConnectionHandler handler = new TapiConnectionHandler();
                        handler.setId(Uuid.fromString(id.toString()));
                        handler.read();
                        return handler;
                    }).collect(Collectors.toList());
        } catch (NullPointerException e) {
            return Collections.emptyList();
        }
    }
}
