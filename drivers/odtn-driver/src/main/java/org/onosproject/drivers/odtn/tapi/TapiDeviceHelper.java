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
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.slf4j.Logger;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.IntStream;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Tapi 2.1 OLS device related helpers.
 */
public final class TapiDeviceHelper {

    private static final Logger log = getLogger(TapiDeviceHelper.class);

    public static final String SERVICE_INTERFACE_POINT = "service-interface-point";
    public static final String CONTEXT = "tapi-common:context";
    public static final String UUID = "uuid";
    public static final String MEDIA_CHANNEL_SERVICE_INTERFACE_POINT_SPEC =
            "tapi-photonic-media:media-channel-service-interface-point-spec";
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
    public static final long BASE_FREQUENCY = 193100000;   //Working in Mhz
    public static final String TAPI_CONNECTIVITY_CONNECTIVITY_SERVICE = "tapi-connectivity:connectivity-service";
    public static final String END_POINT = "end-point";
    public static final String SERVICE_LAYER = "service-layer";
    public static final String SERVICE_TYPE = "service-type";
    public static final String POINT_TO_POINT_CONNECTIVITY = "POINT_TO_POINT_CONNECTIVITY";
    public static final String LOCAL_ID = "local-id";
    public static final String LAYER_PROTOCOL_QUALIFIER = "layer-protocol-qualifier";
    public static final String TAPI_PHOTONIC_MEDIA_PHOTONIC_LAYER_QUALIFIER_NMC =
            "tapi-photonic-media:PHOTONIC_LAYER_QUALIFIER_NMC";
    public static final String SERVICE_INTERFACE_POINT_UUID = "service-interface-point-uuid";
    public static final String AVAILABLE_SPECTRUM = "available-spectrum";
    protected static final String SUPPORTABLE_SPECTRUM = "supportable-spectrum";

    private TapiDeviceHelper(){}

    /**
     * Returns the slot granularity corresponding to a channelSpacing.
     *
     * @param chSpacing      OchSingal channel spacing {@link ChannelSpacing}
     * @return OchSignal slot width granularity
     */
    public static int getSlotGranularity(ChannelSpacing chSpacing) {
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

    /**
     * Returns the ChannelSpacing corresponding to an adjustmentGranularity.
     *
     * @param adjustmentGranularity {@link String}
     * @return OchSingnal ChannelSpacing {@link ChannelSpacing}
     */
    public static ChannelSpacing getChannelSpacing(String adjustmentGranularity) {
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

    /**
     *  To Mbps conversion from Hz.
     *
     *  @param speed the speed in Hz
     *  @return the speed in Mbps
     */
    public static long toMbpsFromHz(long speed) {
        return speed / 1000000;
    }

    /**
     * If SIP info match our criteria, SIP component shall includes mc-pool information which must be obtained in order
     * to complete the OchSignal info. with the TAPI SIP information included in spectrum-supported, spectrum-available
     * and spectrum-occupied tapi objects.
     *
     * @param mcPool the MC_POOL json node
     * @return the set of OCH signals given the port's information
     **/
    protected static Set<OchSignal> getOchSignal(JsonNode mcPool) {

        Set<OchSignal> lambdas = new LinkedHashSet<>();
        long availableUpperFrec = 0;
        long availableLowerFrec = 0;
        String availableAdjustmentGranularity = "";
        String availableGridType = "";
        JsonNode availableSpectrum = mcPool.get(AVAILABLE_SPECTRUM);

        if (availableSpectrum == null) {
            availableSpectrum = mcPool.get(SUPPORTABLE_SPECTRUM);
        }

        Iterator<JsonNode> iterAvailable = availableSpectrum.iterator();
        while (iterAvailable.hasNext()) {
            JsonNode availableSpec = iterAvailable.next();
            availableUpperFrec = availableSpec.get(UPPER_FREQUENCY).asLong();
            availableLowerFrec = availableSpec.get(LOWER_FREQUENCY).asLong();
            log.info("availableUpperFrec {}, availableLowerFrec {}", availableUpperFrec,
                    availableLowerFrec);
            availableAdjustmentGranularity = availableSpec.get(FREQUENCY_CONSTRAINT)
                    .get(ADJUSTMENT_GRANULARITY).textValue();
            availableGridType = availableSpec.get(FREQUENCY_CONSTRAINT).get(GRID_TYPE).textValue();

            int slotGranularity;
            ChannelSpacing chSpacing = getChannelSpacing(availableAdjustmentGranularity);
            double spacingFrequency = chSpacing.frequency().asGHz();
            int lambdaCount = (int) ((availableUpperFrec - availableLowerFrec) / spacingFrequency);
            GridType gridType = GridType.valueOf(availableGridType);
            if (gridType == GridType.DWDM) {
                slotGranularity = getSlotGranularity(chSpacing);
                int finalSlotGranularity = slotGranularity;
                IntStream.range(0, lambdaCount).forEach(x -> lambdas.add(new OchSignal(GridType.DWDM, chSpacing,
                        x - (lambdaCount / 2), finalSlotGranularity)));
            } else if (gridType == GridType.CWDM) {
                log.warn("GridType CWDM. Not implemented");
            } else if (gridType == GridType.FLEX) {
                log.warn("GridType FLEX. Not implemented");
            } else {
                log.warn("Unknown GridType");
            }
        }
        return lambdas;
    }
}
