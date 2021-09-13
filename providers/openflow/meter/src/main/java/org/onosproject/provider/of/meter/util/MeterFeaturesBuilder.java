/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onosproject.net.meter.MeterFeaturesFlag;
import org.projectfloodlight.openflow.protocol.OFMeterFeatures;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.meter.Band.Type.DROP;
import static org.onosproject.net.meter.Band.Type.REMARK;
import static org.onosproject.net.meter.Meter.Unit.KB_PER_SEC;
import static org.onosproject.net.meter.Meter.Unit.PKTS_PER_SEC;
import static org.onosproject.net.meter.MeterFeaturesFlag.ACTION_SET;
import static org.onosproject.net.meter.MeterFeaturesFlag.ANY_POSITION;
import static org.onosproject.net.meter.MeterFeaturesFlag.MULTI_LIST;
import static org.projectfloodlight.openflow.protocol.ver13.OFMeterBandTypeSerializerVer13.DROP_VAL;
import static org.projectfloodlight.openflow.protocol.ver13.OFMeterBandTypeSerializerVer13.DSCP_REMARK_VAL;
import static org.projectfloodlight.openflow.protocol.ver13.OFMeterFlagsSerializerVer13.*;
import static org.projectfloodlight.openflow.protocol.ver15.OFMeterFeatureFlagsSerializerVer15.ACTION_SET_VAL;
import static org.projectfloodlight.openflow.protocol.ver15.OFMeterFeatureFlagsSerializerVer15.ANY_POSITION_VAL;
import static org.projectfloodlight.openflow.protocol.ver15.OFMeterFeatureFlagsSerializerVer15.MULTI_LIST_VAL;

/**
 * OpenFlow builder of MeterFeatures.
 */
public class MeterFeaturesBuilder {
    private static final Logger log = LoggerFactory.getLogger(MeterFeaturesBuilder.class);

    private final OFMeterFeatures ofMeterFeatures;
    private DeviceId deviceId;

    private static final long OF_METER_START_INDEX = 1L;

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
                .withMaxColors(ofMeterFeatures.getMaxColor());

        /*
         * We extract the number of supported meters.
         */
        if (ofMeterFeatures.getMaxMeter() > 0) {
            builder.withStartIndex(OF_METER_START_INDEX)
                   .withEndIndex(ofMeterFeatures.getMaxMeter());
        }
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

        /*
         * Along with the OF1.5, we extract meter features flags
         */
        if (ofMeterFeatures.getVersion().wireVersion >= OFVersion.OF_15.wireVersion) {
            Set<MeterFeaturesFlag> meterFeaturesFlags = Sets.newHashSet();
            if ((ACTION_SET_VAL & ofMeterFeatures.getFeatures()) != 0) {
                meterFeaturesFlags.add(ACTION_SET);
            }
            if ((ANY_POSITION_VAL & ofMeterFeatures.getFeatures()) != 0) {
                meterFeaturesFlags.add(ANY_POSITION);
            }
            if ((MULTI_LIST_VAL & ofMeterFeatures.getFeatures()) != 0) {
                meterFeaturesFlags.add(MULTI_LIST);
            }
            builder.withFeatures(meterFeaturesFlags);
        }

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
