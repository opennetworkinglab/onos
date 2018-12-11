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

import java.util.List;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.Uuid;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connectivitycontext.ConnectivityServiceKeys;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connectivitycontext.DefaultConnectivityService;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connectivityservice.DefaultConnection;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connectivityservice.EndPoint;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.context.augmentedtapicommoncontext.DefaultConnectivityContext;
import org.onosproject.yang.model.DefaultModelObjectData;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId;

import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.getUuid;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.setUuid;

/**
 * Utility class to deal with TAPI ConnectivityService with DCS.
 */
public final class TapiConnectivityServiceHandler extends TapiObjectHandler<DefaultConnectivityService> {

    private TapiConnectivityServiceHandler() {
        obj = new DefaultConnectivityService();
        setId();
    }

    public static TapiConnectivityServiceHandler create() {
        return new TapiConnectivityServiceHandler();
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

        ConnectivityServiceKeys key = new ConnectivityServiceKeys();
        key.uuid(getId());

        DefaultConnection mObj = new DefaultConnection();

        ModelObjectId mId = ModelObjectId.builder()
                .addChild(DefaultContext.class)
                .addChild(DefaultConnectivityContext.class)
                .addChild(DefaultConnectivityService.class, key)
                .build();

        return DefaultModelObjectData.builder()
                .addModelObject(mObj)
                .identifier(mId)
                .build();
    }

    public List<EndPoint> getEndPoint() {
        return obj.endPoint();
    }

    public TapiConnectivityServiceHandler addSep(EndPoint sep) {
        obj.addToEndPoint(sep);
        return this;
    }

    public TapiConnectivityServiceHandler addConnection(Uuid connectionUuid) {
        DefaultConnection connection = new DefaultConnection();
        connection.connectionUuid(connectionUuid.toString());
        obj.addToConnection(connection);
        return this;
    }
}
