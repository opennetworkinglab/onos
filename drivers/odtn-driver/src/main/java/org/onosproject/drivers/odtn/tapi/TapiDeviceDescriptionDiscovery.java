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
import org.onosproject.net.Device;
import org.onosproject.net.PortNumber;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.PortDescription;
import org.onosproject.net.device.DefaultDeviceDescription;
import org.onosproject.net.device.DeviceDescription;
import org.onosproject.net.device.DeviceDescriptionDiscovery;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.GridType;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.protocol.rest.RestSBController;
import org.slf4j.Logger;

import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
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
    private static final String SIP_REQUEST_DATA_API = "/data/context/";
    public static final String SERVICE_INTERFACE_POINT = "service-interface-point";
    public static final String UUID = "uuid";
    public static final String MEDIA_CHANNEL_SERVICE_INTERFACE_POINT_SPEC =
            "media-channel-service-interface-point-spec";
    public static final String MC_POOL = "mc-pool";
    public static final String LAYER_PROTOCOL_NAME = "layer-protocol-name";
    public static final String PHOTONIC_MEDIA = "PHOTONIC_MEDIA";
    public static final String SUPPORTED_LAYER_PROTOCOL_QUALIFIER = "supported-layer-protocol-qualifier";
    public static final String PHOTONIC_LAYER_QUALIFIER_NMC = "PHOTONIC_LAYER_QUALIFIER_NMC";
    public static final String FREQUENCY_CONSTRAINT = "frequency-constraint";
    public static final String GRID_TYPE = "grid-type";
    public static final String ADJUSTMENT_GRANULARITY = "adjustment-granularity";
    public static final String UPPER_FREQUENCY = "upper-frequency";
    public static final String LOWER_FREQUENCY = "lower-frequency";
    public static final String AVAILABLE_SPECTRUM = "available-spectrum";
    private static PortNumber nPort = PortNumber.portNumber(1);
    private static final Object N_PORT_LOCK = new Object();
    private static final long BASE_FREQUENCY = 193100000;   //Working in Mhz

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
            return new DefaultDeviceDescription(deviceId.uri(),
                    Device.Type.OLS,
                    "tapi-swagger",
                    "0",
                    "2.1",
                    "Unknown",
                    new ChassisId());
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
            return parseTapiPorts(jsonNode);
        } catch (IOException e) {
            log.error("Exception discoverPortDetails() {}", did(), e);
            return ImmutableList.of();
        }
    }

    protected List<PortDescription> parseTapiPorts(JsonNode tapiContext) {
        List<PortDescription> ports = Lists.newArrayList();
        int counter = 0;

        /**
         This annotations are used to store persistent mapping information between TAPI SIP's uuid
         and ONOS device portNumbers. This is needed to be publicly available at least within ODTN app
         when connectivity services will be sent to OLS Controller.
         **/
        DefaultAnnotations.Builder annotations = DefaultAnnotations.builder();

        JsonNode sips = tapiContext.get(SERVICE_INTERFACE_POINT);
        Iterator<JsonNode> iter = sips.iterator();
        while (iter.hasNext()) {
            JsonNode sipAttributes = iter.next();
            if (checkValidEndpoint(sipAttributes)) {
                String uuid = sipAttributes.get(UUID).textValue();
                JsonNode mcPool = sipAttributes.get(MEDIA_CHANNEL_SERVICE_INTERFACE_POINT_SPEC).get(MC_POOL);

                OchSignal ochSignal = getOchSignal(mcPool);
                synchronized (N_PORT_LOCK) {
                    //annotations(portNumber-uuid)
                    annotations.set(nPort.toString(), uuid);

                    //add och port
                    ports.add(ochPortDescription(nPort, true, OduSignalType.ODU4,
                              false, ochSignal, annotations.build()));

                    nPort = PortNumber.portNumber(counter++);
                }
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
        return (sipAttributes.get(LAYER_PROTOCOL_NAME).toString().contains(PHOTONIC_MEDIA) &&
                sipAttributes.get(SUPPORTED_LAYER_PROTOCOL_QUALIFIER).toString()
                        .contains(PHOTONIC_LAYER_QUALIFIER_NMC));
    }

    /**
     * If SIP info match our criteria, SIP component shall includes mc-pool information which must be obtained in order
     * to complete the OchSignal info. with the TAPI SIP information included in spectrum-supported, spectrum-available
     * and spectrum-occupied tapi objects.
     **/
    private OchSignal getOchSignal(JsonNode mcPool) {
        long availableUpperFrec = 0, availableLowerFrec = 0;
        String availableAdjustmentGranularity = "", availableGridType = "";
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
        }

        int spacingMult = 0, slotGranularity = 1;
        ChannelSpacing chSpacing = getChannelSpacing(availableAdjustmentGranularity);
        long spacingFrequency = chSpacing.frequency().asHz();
        long centralFrequency = (availableUpperFrec - (availableUpperFrec - availableLowerFrec) / 2);

        GridType gridType = getGridType(availableGridType);
        if (gridType == GridType.DWDM) {
            spacingMult = (int) ((centralFrequency - BASE_FREQUENCY) / toMbpsFromHz(spacingFrequency));
        } else if (gridType == GridType.CWDM) {
            log.warn("GridType CWDM. Not implemented");
        } else if (gridType == GridType.FLEX) {
            log.warn("GridType FLEX. Not implemented");
            slotGranularity = getSlotGranularity(chSpacing);
        } else {
            log.warn("Unknown GridType");
        }
        return new OchSignal(gridType, chSpacing, spacingMult, slotGranularity);
    }

    private int getSlotGranularity(ChannelSpacing chSpacing) {
        if (chSpacing.equals(ChannelSpacing.CHL_100GHZ)) {
            return 8;
        } else if (chSpacing.equals(ChannelSpacing.CHL_50GHZ)) {
            return 4;
        } else if (chSpacing.equals(ChannelSpacing.CHL_25GHZ)) {
            return 2;
        } else if (chSpacing.equals(ChannelSpacing.CHL_12P5GHZ)) {
            return 1;
        } else {
            return 0;
        }
    }

    private GridType getGridType(String gridType) {
        switch (gridType) {
            case "DWDM":
                return GridType.DWDM;
            case "CWDM":
                return GridType.CWDM;
            case "FLEX":
                return GridType.FLEX;
            default:
                return GridType.UNKNOWN;
        }
    }

    private ChannelSpacing getChannelSpacing(String adjustmentGranularity) {
        switch (adjustmentGranularity) {
            case "G_100GHZ ":
                return ChannelSpacing.CHL_100GHZ;
            case "G_50GHZ":
                return ChannelSpacing.CHL_50GHZ;
            case "G_25GHZ":
                return ChannelSpacing.CHL_25GHZ;
            case "G_12_5GHZ ":
                return ChannelSpacing.CHL_12P5GHZ;
            case "G_6_25GHZ ":
                return ChannelSpacing.CHL_6P25GHZ;
            default:
                return ChannelSpacing.CHL_0GHZ;
        }
    }

    private static long toMbpsFromHz(long speed) {
        return speed / 1000000;
    }

}
