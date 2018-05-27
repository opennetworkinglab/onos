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
import org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.LayerProtocolName;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.Uuid;

import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.PORT_TYPE;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.addNameList;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.getUuid;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.setUuid;
import static org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.layerprotocolname.LayerProtocolNameEnum.DSR;
import org.onosproject.yang.gen.v1.tapicommon.rev20180307.tapicommon.tapicontext.DefaultServiceInterfacePoint;
import org.onosproject.yang.model.ModelObjectId;

/**
 * Utility class to deal with TAPI SIP with DCS.
 */
public final class TapiSipHandler extends TapiObjectHandler<DefaultServiceInterfacePoint> {

    private TapiSipHandler() {
        obj = new DefaultServiceInterfacePoint();
        setId();
    }

    public static TapiSipHandler create() {
        return new TapiSipHandler();
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
                .build();
    }

    /**
     * Check this handler dealing with port for SIP or not.
     * @param port onos port object
     * @return is this handler for SIP or not
     */
    public static boolean isSip(Port port) {
        // FIXME modify this method to appropriate way
        String portType = port.annotations().value(PORT_TYPE);
        OdtnDeviceDescriptionDiscovery.OdtnPortType odtnPortType
                = OdtnDeviceDescriptionDiscovery.OdtnPortType.fromValue(portType);
        return odtnPortType.value().equals(OdtnDeviceDescriptionDiscovery.OdtnPortType.CLIENT.value());
    }

    public TapiSipHandler setPort(Port port) {
        if (!isSip(port)) {
            throw new IllegalStateException("Not allowed to use this port as SIP.");
        }
        ConnectPoint cp = new ConnectPoint(port.element().id(), port.number());
        return setConnectPoint(cp);
    }

    public TapiSipHandler setConnectPoint(ConnectPoint cp) {
        Map<String, String> kvs = new HashMap<>();
        kvs.put(ONOS_CP, cp.toString());
        addNameList(obj, kvs);
        obj.addToLayerProtocolName(LayerProtocolName.of(DSR));
        return this;
    }

}
