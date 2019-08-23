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
 *
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */
package org.onosproject.drivers.odtn.tapi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.onlab.packet.ChassisId;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.protocol.rest.RestSBController;
import org.slf4j.Logger;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.CONTEXT;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.LAYER_PROTOCOL_NAME;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.MC_POOL;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.MEDIA_CHANNEL_SERVICE_INTERFACE_POINT_SPEC;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.PHOTONIC_LAYER_QUALIFIER_NMC;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.PHOTONIC_MEDIA;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.SERVICE_INTERFACE_POINT;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.SUPPORTED_LAYER_PROTOCOL_QUALIFIER;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.UUID;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.getOchSignal;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.removeInitalConnectivityServices;
import static org.onosproject.net.optical.device.OchPortHelper.ochPortDescription;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Driver Implementation of the DeviceDescrption discovery for ONF Transport-API (TAPI) v2.1 based
 * open line systems (OLS).
 */

public class TapiDeviceDescriptionDiscovery
        extends AbstractHandlerBehaviour
        implements DeviceDescriptionDiscovery {

    private static final Logger log = getLogger(TapiDeviceDescriptionDiscovery.class);
    private static final String SIP_REQUEST_DATA_API = "/restconf/data/tapi-common:context";

    /**
     * Get the deviceId for which the methods apply.
     *
     * @return The deviceId as contained in the handler data
     */
    private DeviceId did() {
        return handler().data().deviceId();
    }

    @Override
    public DeviceDescription discoverDeviceDetails() {
        log.debug("Getting device description");
        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        DeviceId deviceId = handler().data().deviceId();
        Device device = deviceService.getDevice(deviceId);

        if (device == null) {
            //TODO need to obtain from the device.
            return new DefaultDeviceDescription(deviceId.uri(),
                    Device.Type.OLS,
                    "Tapi",
                    "0",
                    "2.1",
                    "Unknown",
                    new ChassisId(),
                    DefaultAnnotations.builder().set("protocol", "REST").build());
        } else {
            return new DefaultDeviceDescription(device.id().uri(),
                    Device.Type.OLS,
                    device.manufacturer(),
                    device.hwVersion(),
                    device.swVersion(),
                    device.serialNumber(),
                    device.chassisId());
        }
    }

    @Override
    public List<PortDescription> discoverPortDetails() {
        log.debug("Discovering port details.");
        RestSBController controller = checkNotNull(handler().get(RestSBController.class));
        DeviceId deviceId = handler().data().deviceId();

        try {
            InputStream inputStream = controller.get(deviceId, SIP_REQUEST_DATA_API, MediaType.APPLICATION_JSON_TYPE);
            JsonNode jsonNode = new ObjectMapper().readTree(inputStream);
            if (jsonNode == null) {
                log.error("Can't discover port details at {}", SIP_REQUEST_DATA_API);
                return ImmutableList.of();
            }
            List<PortDescription> ports = parseTapiPorts(jsonNode);
            //Removing any initial connectivity services
            removeInitalConnectivityServices(deviceId, handler());
            return ports;
        } catch (IOException e) {
            log.error("Exception discoverPortDetails() {}", did(), e);
            removeInitalConnectivityServices(deviceId, handler());
            return ImmutableList.of();
        }
    }

    protected List<PortDescription> parseTapiPorts(JsonNode tapiContext) {
        List<PortDescription> ports = Lists.newArrayList();
        /*
         This annotations are used to store persistent mapping information between TAPI SIP's uuid
         and ONOS device portNumbers. This is needed to be publicly available at least within ODTN app
         when connectivity services will be sent to OLS Controller.
         */
        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder();
        Iterator<JsonNode> iter = tapiContext.get(CONTEXT).get(SERVICE_INTERFACE_POINT).iterator();
        while (iter.hasNext()) {
            JsonNode sipAttributes = iter.next();
            if (checkValidEndpoint(sipAttributes)) {
                String uuid = sipAttributes.get(UUID).textValue();
                String[] uuidSeg = uuid.split("-");
                PortNumber portNumber = PortNumber.portNumber(uuidSeg[uuidSeg.length - 1]);
                annotations.set(UUID, uuid);

                JsonNode mcPool = sipAttributes.get(MEDIA_CHANNEL_SERVICE_INTERFACE_POINT_SPEC).get(MC_POOL);
                // We get the first OCH signal as reported by the device.
                OchSignal ochSignal = getOchSignal(mcPool).iterator().next();
                //add och port
                ports.add(ochPortDescription(portNumber, true, OduSignalType.ODU4,
                        false, ochSignal, annotations.build()));


            } else {
                log.error("SIP {} is not valid", sipAttributes);
            }
        }
        log.debug("PortList: {}", ports);
        return ImmutableList.copyOf(ports);
    }

    /**
     * Create a filter method to identify just valid OLS SIPs.This method must check the
     * tapi object: "layer-protocol-name" and matching only if equals to "PHOTONIC-MEDIA" SIPs.
     * Moreover,the filtering could be enhanced by reading also:
     * "supported-layer-protocol-qualifier", to identify valid protocol-qualifier values such:
     * [PHOTONIC_LAYER_QUALIFIER_NMC, PHOTONIC_LAYER_QUALIFIER_NMCA, PHOTONIC_LAYER_QUALIFIER_OTSI...]
     **/
    private boolean checkValidEndpoint(JsonNode sipAttributes) {
        return sipAttributes.has(LAYER_PROTOCOL_NAME) &&
                sipAttributes.get(LAYER_PROTOCOL_NAME).toString().contains(PHOTONIC_MEDIA) &&
                sipAttributes.has(SUPPORTED_LAYER_PROTOCOL_QUALIFIER) &&
                sipAttributes.get(SUPPORTED_LAYER_PROTOCOL_QUALIFIER).toString()
                        .contains(PHOTONIC_LAYER_QUALIFIER_NMC);
    }

}
