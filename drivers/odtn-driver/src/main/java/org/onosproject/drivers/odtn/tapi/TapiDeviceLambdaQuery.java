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

import com.google.common.collect.ImmutableSet;
import org.onosproject.net.Port;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.protocol.rest.RestSBController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.MEDIA_CHANNEL_SERVICE_INTERFACE_POINT_SPEC;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.AVAILABLE_SPECTRUM;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.ADJUSTMENT_GRANULARITY;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.BASE_FREQUENCY;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.FREQUENCY_CONSTRAINT;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.GRID_TYPE;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.LOWER_FREQUENCY;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.MC_POOL;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.UPPER_FREQUENCY;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.toMbpsFromHz;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.getChannelSpacing;
import static org.onosproject.drivers.odtn.tapi.TapiDeviceHelper.getSlotGranularity;

import static org.slf4j.LoggerFactory.getLogger;
import static com.google.common.base.Preconditions.checkNotNull;
import org.onosproject.net.device.DeviceService;
import javax.ws.rs.core.MediaType;



/**
 * Driver behavior of TAPI devices to discover the map of available lambdas of the ports.
 */
public class TapiDeviceLambdaQuery extends AbstractHandlerBehaviour
        implements LambdaQuery {

    private static final Logger log = getLogger(TapiDeviceLambdaQuery.class);
    private static final String SIP_REQUEST_DATA_API = "/data/context/service-interface-point=";

    /**
     * Get the deviceId for which the methods apply.
     *
     * @return The deviceId as contained in the handler data
     */
    private DeviceId did() {
        return handler().data().deviceId();
    }

    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {
        RestSBController controller = checkNotNull(handler().get(RestSBController.class));
        DeviceService deviceService = checkNotNull(handler().get(DeviceService.class));
        DeviceId deviceId = did();
        Device dev = deviceService.getDevice(deviceId);
        if (dev == null) {
            log.error("Device {} does not exist", deviceId);
            return ImmutableSet.of();
        }
        Port p = deviceService.getPort(dev.id(), port);
        if (p == null) {
            log.error("Port {} does not exist", port);
            return ImmutableSet.of();
        }
        String uuid = p.annotations().value(port.toString());

        try {
            InputStream inputStream = controller.get(deviceId, SIP_REQUEST_DATA_API + uuid,
                    MediaType.APPLICATION_JSON_TYPE);
            log.debug("Service interface point UUID: {}", uuid);
            JsonNode sipAttributes = new ObjectMapper().readTree(inputStream);
            JsonNode mcPool = sipAttributes.get(MEDIA_CHANNEL_SERVICE_INTERFACE_POINT_SPEC).get(MC_POOL);

            //This creates a hashset of OChSignals representing the spectrum availability at the target port.
            Set<OchSignal> lambdas = getOchSignal(mcPool);
            log.debug("Lambdas: {}", lambdas.toString());
            return lambdas;

        } catch (IOException e) {
            log.error("Exception discoverPortDetails() {}", did(), e);
            return ImmutableSet.of();
        }
    }

    /**
     * If SIP info match our criteria, SIP component shall includes mc-pool information which must be obtained in order
     * to complete the OchSignal info. with the TAPI SIP information included in spectrum-supported, spectrum-available
     * and spectrum-occupied tapi objects.
     **/
    private Set<OchSignal> getOchSignal(JsonNode mcPool) {

        Set<OchSignal> lambdas = new LinkedHashSet<>();
        long availableUpperFrec = 0;
        long availableLowerFrec = 0;
        String availableAdjustmentGranularity = "";
        String availableGridType = "";
        JsonNode availableSpectrum = mcPool.get(AVAILABLE_SPECTRUM);

        /**At this time only the latest availableSpectrum is used**/
        Iterator<JsonNode> iterAvailable = availableSpectrum.iterator();
        while (iterAvailable.hasNext()) {
            JsonNode availableSpec = iterAvailable.next();
            availableUpperFrec = availableSpec.get(UPPER_FREQUENCY).asLong();
            availableLowerFrec = availableSpec.get(LOWER_FREQUENCY).asLong();
            availableAdjustmentGranularity = availableSpec.get(FREQUENCY_CONSTRAINT)
                    .get(ADJUSTMENT_GRANULARITY).textValue();
            availableGridType = availableSpec.get(FREQUENCY_CONSTRAINT).get(GRID_TYPE).textValue();

            int spacingMult = 0;
            int slotGranularity = 1;
            ChannelSpacing chSpacing = getChannelSpacing(availableAdjustmentGranularity);
            long spacingFrequency = chSpacing.frequency().asHz();
            long centralFrequency = (availableUpperFrec - (availableUpperFrec - availableLowerFrec) / 2);

            GridType gridType = GridType.valueOf(availableGridType);
            if (gridType == GridType.DWDM) {
                spacingMult = (int) ((centralFrequency - BASE_FREQUENCY) / toMbpsFromHz(spacingFrequency));
                OchSignal ochSignal = new OchSignal(gridType, chSpacing, spacingMult, slotGranularity);
                lambdas.add(ochSignal);
            } else if (gridType == GridType.CWDM) {
                log.warn("GridType CWDM. Not implemented");
            } else if (gridType == GridType.FLEX) {
                log.warn("GridType FLEX. Not implemented");
                slotGranularity = getSlotGranularity(chSpacing);
            } else {
                log.warn("Unknown GridType");
            }
        }
        return lambdas;
    }

}