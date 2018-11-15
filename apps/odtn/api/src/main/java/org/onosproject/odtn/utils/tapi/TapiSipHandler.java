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
import org.onosproject.yang.gen.v1.tapicommon.rev20181016.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20181016.tapicommon.LayerProtocolName;
import org.onosproject.yang.gen.v1.tapicommon.rev20181016.tapicommon.Uuid;

import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.PORT_TYPE;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.addNameList;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.getUuid;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.setUuid;
import static org.onosproject.yang.gen.v1.tapicommon.rev20181016.tapicommon.layerprotocolname.LayerProtocolNameEnum.DSR;
import org.onosproject.yang.gen.v1.tapicommon.rev20181016.tapicommon.tapicontext.DefaultServiceInterfacePoint;
import org.onosproject.yang.model.ModelObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class to deal with TAPI SIP with DCS.
 */
public final class TapiSipHandler extends TapiObjectHandler<DefaultServiceInterfacePoint> {

    private static final Logger log =
            LoggerFactory.getLogger(TapiSipHandler.class);

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
     *
     * @param port onos port object
     * @return is this handler for SIP or not
     */
    public static boolean isSip(Port port) {
        // RCAS Note: We may end up controlling more devices that do
        // not have annotations or do not have the PORT_TYPE
        // annotation. Let's accomodate other devices (e.g.
        // OpenROADM) and be less strict. In short, if the
        // annotation does not exist, simply return false.
        //
        // Note: for phase 1.5+ , we should also allow SIPs to
        // be the transceiver LINE side, when we establish
        // OpticalConnectivityIntent

        if (!port.annotations().keys().contains(PORT_TYPE)) {
            log.warn("No annotation of {} on port {}", PORT_TYPE, port);
            return false;
        }
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
        obj.layerProtocolName(LayerProtocolName.of(DSR));
        return this;
    }

}
