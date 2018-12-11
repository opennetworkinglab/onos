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

import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.Uuid;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connectivityservice.DefaultEndPoint;
import org.onosproject.yang.gen.v1.tapiconnectivity.rev20181210.tapiconnectivity.connectivityserviceendpoint.DefaultServiceInterfacePoint;
import org.onosproject.yang.model.ModelObjectId;

import static org.onosproject.odtn.utils.tapi.TapiLocalClassUtil.getLocalId;
import static org.onosproject.odtn.utils.tapi.TapiLocalClassUtil.setLocalId;

/**
 * Utility class to deal with TAPI SEP with DCS.
 */
public final class TapiSepHandler extends TapiObjectHandler<DefaultEndPoint> {

    private TapiSepHandler() {
        obj = new DefaultEndPoint();
        setId();
    }

    public static TapiSepHandler create() {
        return new TapiSepHandler();
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
        return null;
    }

    public TapiSepHandler setSip(String sipId) {
        DefaultServiceInterfacePoint sip = new DefaultServiceInterfacePoint();
        sip.serviceInterfacePointUuid(sipId);
        obj.serviceInterfacePoint(sip);
        return this;
    }
}
