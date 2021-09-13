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
package org.onosproject.net.meter;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Sets;
import org.onosproject.net.DeviceId;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of MeterFeatures.
 */
public final class DefaultMeterFeatures implements MeterFeatures {
    private DeviceId deviceId;
    private long startIndex;
    private long endIndex;
    private Set<Band.Type> bandTypes;
    private Set<Meter.Unit> units;
    private boolean burst;
    private boolean stats;
    private short maxBands;
    private short maxColor;
    private Set<MeterFeaturesFlag> features;
    private MeterScope scope;

    private DefaultMeterFeatures(DeviceId did, long startIndex, long endIndex,
                                 Set<Band.Type> bandTypes, Set<Meter.Unit> units,
                                 boolean burst, boolean stats,
                                 short maxBands, short maxColor, Set<MeterFeaturesFlag> flag,
                                 MeterScope scope) {
        this.deviceId = did;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.bandTypes = bandTypes;
        this.burst = burst;
        this.stats = stats;
        this.units = units;
        this.maxBands = maxBands;
        this.maxColor = maxColor;
        this.features = flag;
        this.scope = scope;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public long maxMeter() {
        long maxMeter = 0;
        if (startIndex != -1 && endIndex != -1) {
            maxMeter = endIndex - startIndex + 1;
        }
        return maxMeter;
    }

    @Override
    public long startIndex() {
        return startIndex;
    }

    @Override
    public long endIndex() {
        return endIndex;
    }

    @Override
    public Set<Band.Type> bandTypes() {
        return bandTypes;
    }

    @Override
    public Set<Meter.Unit> unitTypes() {
        return units;
    }

    @Override
    public boolean isBurstSupported() {
        return burst;
    }

    @Override
    public boolean isStatsSupported() {
        return stats;
    }

    @Override
    public short maxBands() {
        return maxBands;
    }

    @Override
    public short maxColor() {
        return maxColor;
    }

    @Override
    public Set<MeterFeaturesFlag> features() {
        return features;
    }

    @Override
    public MeterScope scope() {
        return scope;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MeterFeatures noMeterFeatures(DeviceId deviceId) {
        return DefaultMeterFeatures.builder().forDevice(deviceId)
                .build();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("deviceId", deviceId())
                .add("startIndex", startIndex())
                .add("endIndex", endIndex())
                .add("maxBands", maxBands())
                .add("maxColor", maxColor())
                .add("bands", bandTypes())
                .add("burst", isBurstSupported())
                .add("stats", isStatsSupported())
                .add("units", unitTypes())
                .add("scope", scope())
                .toString();
    }

    /**
     * A DefaultMeterFeatures builder.
     */
    public static final class Builder implements MeterFeatures.Builder {
        private DeviceId did;
        private long mmeter = 0L;
        private long starti = -1L;
        private long endi = -1L;
        private short mbands = 0;
        private short mcolors = 0;
        private Set<Band.Type> bandTypes = new HashSet<>();
        private Set<Meter.Unit> units1 = new HashSet<>();
        private boolean burst = false;
        private boolean stats = false;
        private Set<MeterFeaturesFlag> features = Sets.newHashSet();
        private MeterScope mscope = MeterScope.globalScope();

        @Override
        public MeterFeatures.Builder forDevice(DeviceId deviceId) {
            did = deviceId;
            return this;
        }

        @Override
        public MeterFeatures.Builder withMaxMeters(long maxMeter) {
            mmeter = maxMeter;
            return this;
        }

        @Override
        public MeterFeatures.Builder withStartIndex(long startIndex) {
            starti = startIndex;
            return this;
        }

        @Override
        public MeterFeatures.Builder withEndIndex(long endIndex) {
            endi = endIndex;
            return this;
        }

        @Override
        public MeterFeatures.Builder withMaxBands(short maxBands) {
            mbands = maxBands;
            return this;
        }

        @Override
        public MeterFeatures.Builder withMaxColors(short maxColors) {
            mcolors = maxColors;
            return this;
        }

        @Override
        public MeterFeatures.Builder withBandTypes(Set<Band.Type> types) {
            bandTypes = types;
            return this;
        }

        @Override
        public MeterFeatures.Builder withUnits(Set<Meter.Unit> units) {
            units1 = units;
            return this;
        }

        @Override
        public MeterFeatures.Builder hasBurst(boolean hasBurst) {
            burst = hasBurst;
            return this;
        }

        @Override
        public MeterFeatures.Builder hasStats(boolean hasStats) {
            stats = hasStats;
            return this;
        }

        @Override
        public MeterFeatures.Builder withFeatures(Set<MeterFeaturesFlag> featureFlags) {
            features = featureFlags;
            return this;
        }

        @Override
        public MeterFeatures.Builder withScope(MeterScope scope) {
            mscope = scope;
            return this;
        }

        @Override
        public MeterFeatures build() {
            // In case some functions are using maxMeter
            // and both indexes are not set
            // Start index will be
            // 1, if it is global scope (An OpenFlow meter)
            // 0, for the rest (A P4RT meter)
            if (mmeter != 0L && starti == -1L && endi == -1L) {
                starti = mscope.isGlobal() ? 1 : 0;
                endi = mscope.isGlobal() ? mmeter : mmeter - 1;
            }
            // If one of the index is unset/unvalid value, treated as no meter features
            if (starti <= -1 || endi <= -1) {
                starti = -1;
                endi = -1;
            }

            checkNotNull(did, "Must specify a device");
            checkArgument(starti <= endi, "Start index must be less than or equal to end index");

            return new DefaultMeterFeatures(did, starti, endi, bandTypes, units1, burst,
                                            stats, mbands, mcolors, features, mscope);
        }
    }
}
