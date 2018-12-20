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
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.DefaultContext;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.LayerProtocolName;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.Uuid;

import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.PORT_TYPE;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.addNameList;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.getUuid;
import static org.onosproject.odtn.utils.tapi.TapiGlobalClassUtil.setUuid;
import static org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.layerprotocolname.LayerProtocolNameEnum.DSR;
import static org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.layerprotocolname.LayerProtocolNameEnum.PHOTONIC_MEDIA;
import org.onosproject.yang.gen.v1.tapicommon.rev20181210.tapicommon.tapicontext.DefaultServiceInterfacePoint;
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
        /*
         Note: We may end up controlling devices that do
         not have annotations or do not have the PORT_TYPE
         annotation (e.g. OpenROADM),
         Instead of throwing, if the annotation does not
         exist, simply return false.
        */
        if (!port.annotations().keys().contains(PORT_TYPE)) {
            log.warn("No annotation of {} on port {}", PORT_TYPE, port);
            return false;
        }

        // Port type will determine the SIP layer
        String portType = port.annotations().value(PORT_TYPE);
        OdtnDeviceDescriptionDiscovery.OdtnPortType odtnPortType
                = OdtnDeviceDescriptionDiscovery.OdtnPortType.fromValue(portType);

        /*
          Note: for phase 1.5+ , we allow SIPs to
          be the transceiver LINE side, when we establish
          OpticalConnectivityIntent (type PHOTONIC_MEDIA)
        */
        if (odtnPortType.value().equals(
                OdtnDeviceDescriptionDiscovery.OdtnPortType.CLIENT.value())) {
            return true;
        }
        if (odtnPortType.value().equals(
                OdtnDeviceDescriptionDiscovery.OdtnPortType.LINE.value())) {
            return true;
        }
        return false;
    }


    public TapiSipHandler setPort(Port port) {
        if (!isSip(port)) {
            throw new IllegalStateException("Not allowed to use this port as SIP.");
        }
        ConnectPoint cp = new ConnectPoint(port.element().id(), port.number());
        String portType = port.annotations().value(PORT_TYPE);
        return setConnectPoint(cp, portType);
    }


    /**
     * Set Connect Point for this SIP.
     * For backwards compatibility. Set SIP to client.
     *
     * @param cp the connect point
     * @return TapiSipHandler instance
     */
    public TapiSipHandler setConnectPoint(ConnectPoint cp) {
        return setConnectPoint(cp,
                OdtnDeviceDescriptionDiscovery.OdtnPortType.CLIENT.value());
    }

    /**
     * Set Connect Point for this SIP with a given port type.
     *
     * @param cp the connect point
     * @param portType the port type
     * @return TapiSipHandler instance
     */
    public TapiSipHandler setConnectPoint(ConnectPoint cp, String portType) {
        Map<String, String> kvs = new HashMap<>();
        kvs.put(ONOS_CP, cp.toString());
        addNameList(obj, kvs);
        if (portType.equals(
                OdtnDeviceDescriptionDiscovery.OdtnPortType.CLIENT.value())) {
            obj.layerProtocolName(LayerProtocolName.of(DSR));
        }
        if (portType.equals(
                OdtnDeviceDescriptionDiscovery.OdtnPortType.LINE.value())) {
            obj.layerProtocolName(LayerProtocolName.of(PHOTONIC_MEDIA));
        }
        return this;
    }

}
