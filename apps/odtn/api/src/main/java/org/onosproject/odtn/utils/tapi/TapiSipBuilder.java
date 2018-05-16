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

import java.util.HashMap;
import java.util.Map;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Port;

import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.LayerProtocolName;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.Uuid;

import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.setNameList;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.setUuid;
import static org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.layerprotocolname.LayerProtocolNameEnum.DSR;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.tapicontext.DefaultServiceInterfacePoint;
import org.onosproject.yang.model.ModelObject;
import org.onosproject.yang.model.ModelObjectData;
import org.onosproject.yang.model.ModelObjectId;

/**
 * Utility builder class for TAPI sip creation with DCS.
 */
public final class TapiSipBuilder extends TapiInstanceBuilder {

    private DefaultServiceInterfacePoint sip = new DefaultServiceInterfacePoint();
    private Map<String, String> kvs = new HashMap<>();

    private TapiSipBuilder() {
        setUuid(sip);
    }

    public static TapiSipBuilder builder() {
        return new TapiSipBuilder();
    }

    /**
     * Check this builder dealing with port for SIP or not.
     * @param port onos port
     * @return Is this builder for SIP or not
     */
    public static boolean isSip(Port port) {
        // FIXME modify this method to appropriate way
        ConnectPoint cp = new ConnectPoint(port.element().id(), port.number());
        return cp.toString().contains("TRANSCEIVER");
    }

    public TapiSipBuilder setPort(Port port) {
        if (!isSip(port)) {
            throw new IllegalStateException("Not allowed to use this port as SIP.");
        }
        ConnectPoint cp = new ConnectPoint(port.element().id(), port.number());
        kvs.put(ONOS_CP, cp.toString());
        sip.addToLayerProtocolName(LayerProtocolName.of(DSR));
        return this;
    }

    @Override
    public ModelObjectData build() {
        setNameList(sip, kvs);
        ModelObjectId objId = ModelObjectId.builder()
                .addChild(DefaultContext.class)
                .build();
        return getModelObjectData(sip, objId);
    }

    @Override
    public ModelObject getModelObject() {
        return sip;
    }

    @Override
    public Uuid getUuid() {
        return sip.uuid();
    }
}
