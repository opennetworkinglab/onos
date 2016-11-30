/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.provider.of.meter.util;

import com.google.common.collect.Sets;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultMeterFeatures;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterFeatures;
import org.projectfloodlight.openflow.protocol.OFMeterFeatures;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.meter.Band.Type.DROP;
import static org.onosproject.net.meter.Band.Type.REMARK;
import static org.onosproject.net.meter.Meter.Unit.KB_PER_SEC;
import static org.onosproject.net.meter.Meter.Unit.PKTS_PER_SEC;
import static org.projectfloodlight.openflow.protocol.ver13.OFMeterBandTypeSerializerVer13.DROP_VAL;
import static org.projectfloodlight.openflow.protocol.ver13.OFMeterBandTypeSerializerVer13.DSCP_REMARK_VAL;
import static org.projectfloodlight.openflow.protocol.ver13.OFMeterFlagsSerializerVer13.*;

/**
 * OpenFlow builder of MeterFeatures.
 */
public class MeterFeaturesBuilder {
    private static final Logger log = LoggerFactory.getLogger(MeterFeaturesBuilder.class);

    private final OFMeterFeatures ofMeterFeatures;
    private DeviceId deviceId;

    public MeterFeaturesBuilder(OFMeterFeatures features, DeviceId deviceId) {
        this.ofMeterFeatures = checkNotNull(features);
        this.deviceId = deviceId;
    }

    /**
     * To build a MeterFeatures using the openflow object
     * provided by the southbound.
     *
     * @return the meter features object
     */
    public MeterFeatures build() {
        /*
         * We set the basic values before to extract the other information.
         */
        MeterFeatures.Builder builder = DefaultMeterFeatures.builder()
                .forDevice(deviceId)
                .withMaxBands(ofMeterFeatures.getMaxBands())
                .withMaxColors(ofMeterFeatures.getMaxColor())
                .withMaxMeters(ofMeterFeatures.getMaxMeter());
        /*
         * We extract the supported band types.
         */
        Set<Band.Type> bands = Sets.newHashSet();
        if ((DROP_VAL & ofMeterFeatures.getCapabilities()) != 0) {
            bands.add(DROP);
        }
        if ((DSCP_REMARK_VAL & ofMeterFeatures.getCapabilities()) != 0) {
            bands.add(REMARK);
        }
        builder.withBandTypes(bands);
        /*
         * We extract the supported units;
         */
        Set<Meter.Unit> units = Sets.newHashSet();
        if ((PKTPS_VAL & ofMeterFeatures.getCapabilities()) != 0) {
            units.add(PKTS_PER_SEC);
        }
        if ((KBPS_VAL & ofMeterFeatures.getCapabilities()) != 0) {
            units.add(KB_PER_SEC);
        }
        if (units.isEmpty()) {
            units.add(PKTS_PER_SEC);
        }
        builder.withUnits(units);
        /*
         * Burst is supported ?
         */
        builder.hasBurst((BURST_VAL & ofMeterFeatures.getCapabilities()) != 0);
        /*
         * Stats are supported ?
         */
        builder.hasStats((STATS_VAL & ofMeterFeatures.getCapabilities()) != 0);
        return builder.build();
    }

    /**
     * To build an empty meter features.
     * @param deviceId the device id
     * @return the meter features
     */
    public static MeterFeatures noMeterFeatures(DeviceId deviceId) {
        return DefaultMeterFeatures.noMeterFeatures(deviceId);
    }

}
