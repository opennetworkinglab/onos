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
package org.onosproject.net.meter;

import com.google.common.base.MoreObjects;
import org.onosproject.net.DeviceId;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of MeterFeatures.
 */
public final class DefaultMeterFeatures implements MeterFeatures {
    private DeviceId deviceId;
    private long maxMeter;
    private Set<Band.Type> bandTypes;
    private Set<Meter.Unit> units;
    private boolean burst;
    private boolean stats;
    private short maxBands;
    private short maxColor;

    private DefaultMeterFeatures(DeviceId did, long maxMeter,
                                 Set<Band.Type> bandTypes, Set<Meter.Unit> units,
                                 boolean burst, boolean stats,
                                 short maxBands, short maxColor) {
        this.deviceId = did;
        this.maxMeter = maxMeter;
        this.bandTypes = bandTypes;
        this.burst = burst;
        this.stats = stats;
        this.units = units;
        this.maxBands = maxBands;
        this.maxColor = maxColor;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public long maxMeter() {
        return maxMeter;
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
                .add("maxMeter", maxMeter())
                .add("maxBands", maxBands())
                .add("maxColor", maxColor())
                .add("bands", bandTypes())
                .add("burst", isBurstSupported())
                .add("stats", isStatsSupported())
                .add("units", unitTypes())
                .toString();
    }

    /**
     * A DefaultMeterFeatures builder.
     */
    public static final class Builder implements MeterFeatures.Builder {
        private DeviceId did;
        private long mmeter = 0L;
        private short mbands = 0;
        private short mcolors = 0;
        private Set<Band.Type> bandTypes = new HashSet<>();
        private Set<Meter.Unit> units1 = new HashSet<>();
        private boolean burst = false;
        private boolean stats = false;

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
        public MeterFeatures build() {
            checkNotNull(did, "Must specify a device");
            return new DefaultMeterFeatures(did, mmeter, bandTypes, units1, burst, stats, mbands, mcolors);
        }
    }
}
