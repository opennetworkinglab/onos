/*
 * Copyright 2022-present Open Networking Foundation
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

package org.onosproject.net.behaviour.upf;

import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.behaviour.upf.UpfEntityType.APPLICATION_METER;
import static org.onosproject.net.behaviour.upf.UpfEntityType.SESSION_METER;
import static org.onosproject.net.behaviour.upf.UpfEntityType.SLICE_METER;
import static org.onosproject.net.meter.Band.Type.MARK_RED;
import static org.onosproject.net.meter.Band.Type.MARK_YELLOW;

/**
 * A structure representing a UPF meter, either for metering session (UE),
 * application traffic, or slice traffic.
 * UPF meters represent PFCP QER MBR and GBR information and slice maximum rate.
 * UPF meters of type session and slice support only the peak band.
 * UPF meters of type application support both peak and committed bands.
 */
@Beta
public final class UpfMeter implements UpfEntity {
    private final int cellId;
    private final ImmutableMap<Band.Type, Band> meterBands;
    private final UpfEntityType type;

    private UpfMeter(int cellId, Map<Band.Type, Band> meterBands, UpfEntityType type) {
        this.cellId = cellId;
        this.meterBands = ImmutableMap.copyOf(meterBands);
        this.type = type;
    }

    @Override
    public String toString() {
        return String.format("UpfMeter(type=%s, index=%d, committed=%s, peak=%s)",
                             type, cellId, committedBand().orElse(null),
                             peakBand().orElse(null));
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (object == null) {
            return false;
        }

        if (getClass() != object.getClass()) {
            return false;
        }

        UpfMeter that = (UpfMeter) object;
        return this.type.equals(that.type) &&
                this.cellId == that.cellId &&
                this.meterBands.equals(that.meterBands);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, cellId, meterBands);
    }

    @Override
    public UpfEntityType type() {
        return this.type;
    }

    /**
     * Get the meter cell index of this meter.
     *
     * @return the cell index
     */
    public int cellId() {
        return this.cellId;
    }

    /**
     * Get the committed band of this meter.
     *
     * @return the committed band, Empty if none
     */
    public Optional<Band> committedBand() {
        return Optional.ofNullable(meterBands.getOrDefault(MARK_YELLOW, null));
    }

    /**
     * Get the peak band of this meter.
     *
     * @return the peak band, Empty if none
     */
    public Optional<Band> peakBand() {
        return Optional.ofNullable(meterBands.getOrDefault(MARK_RED, null));
    }

    /**
     * Check if this UPF meter is for sessions (UE) traffic.
     *
     * @return true if the meter is for session traffic
     */
    public boolean isSession() {
        return type.equals(SESSION_METER);
    }

    /**
     * Check if this UPF meter is for application traffic.
     *
     * @return true if the meter is for application traffic
     */
    public boolean isApplication() {
        return type.equals(APPLICATION_METER);
    }

    /**
     * Check if this UPF meter is a reset.
     *
     * @return true if this represents a meter reset.
     */
    public boolean isReset() {
        return meterBands.isEmpty();
    }

    /**
     * Return a session UPF meter with no bands. Used to reset the meter.
     *
     * @param cellId the meter cell index of this meter
     * @return a UpfMeter of type session with no bands
     */
    public static UpfMeter resetSession(int cellId) {
        return new UpfMeter(cellId, Maps.newHashMap(), SESSION_METER);
    }

    /**
     * Return an application UPF meter with no bands. Used to reset the meter.
     *
     * @param cellId the meter cell index of this meter
     * @return a UpfMeter of type application with no bands
     */
    public static UpfMeter resetApplication(int cellId) {
        return new UpfMeter(cellId, Maps.newHashMap(), APPLICATION_METER);
    }

    /**
     * Return a slice meter with no bands. Used to reset the meter.
     *
     * @param cellId the meter cell index of this meter
     * @return a UpfMeter of type slice with no bands
     */
    public static UpfMeter resetSlice(int cellId) {
        return new UpfMeter(cellId, Maps.newHashMap(), SLICE_METER);
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder of UpfMeter object. Use {@link #resetApplication(int)},
     * {@link #resetSession(int)}, or {@link #resetSlice(int)} to reset the
     * meter config.
     */
    public static class Builder {
        private Integer cellId = null;
        private Map<Band.Type, Band> bands = Maps.newHashMap();
        private UpfEntityType type;

        public Builder() {

        }

        /**
         * Set the meter cell index of this meter.
         *
         * @param cellId the meter cell index
         * @return this builder object
         */
        public Builder setCellId(int cellId) {
            this.cellId = cellId;
            return this;
        }

        /**
         * Set the committed band of this meter.
         * Valid only for meter of type application.
         *
         * @param cir    the Committed Information Rate in bytes/s
         * @param cburst the Committed Burst in bytes
         * @return this builder object
         */
        public Builder setCommittedBand(long cir, long cburst) {
            this.bands.put(MARK_YELLOW,
                           DefaultBand.builder()
                                   .ofType(MARK_YELLOW)
                                   .withRate(cir)
                                   .burstSize(cburst)
                                   .build()
            );
            return this;
        }

        /**
         * Set the peak band of this meter.
         *
         * @param pir    the Peak Information Rate in bytes/s
         * @param pburst the Peak Burst in bytes
         * @return this builder object
         */
        public Builder setPeakBand(long pir, long pburst) {
            this.bands.put(MARK_RED,
                           DefaultBand.builder()
                                   .ofType(MARK_RED)
                                   .withRate(pir)
                                   .burstSize(pburst)
                                   .build()
            );
            return this;
        }

        /**
         * Make this meter a session meter.
         *
         * @return this builder object
         */
        public Builder setSession() {
            this.type = SESSION_METER;
            return this;
        }

        /**
         * Make this meter an application meter.
         *
         * @return this builder object
         */
        public Builder setApplication() {
            this.type = APPLICATION_METER;
            return this;
        }

        /**
         * Make this meter a slice meter.
         *
         * @return this builder object
         */
        public Builder setSlice() {
            this.type = SLICE_METER;
            return this;
        }

        public UpfMeter build() {
            checkNotNull(type, "A meter type must be assigned");
            switch (type) {
                case SESSION_METER:
                case SLICE_METER:
                    checkArgument(!bands.containsKey(MARK_YELLOW),
                                  "Committed band can not be provided for " + type + " meter!");
                    break;
                case APPLICATION_METER:
                    checkArgument((bands.containsKey(MARK_YELLOW) && bands.containsKey(MARK_RED)) || bands.isEmpty(),
                                  "Bands (committed and peak) must be provided together or not at all!");
                    break;
                default:
                    // I should never reach this point
                    throw new IllegalArgumentException("Invalid meter type, I should never reach this point");
            }
            checkNotNull(cellId, "Meter cell ID must be provided!");
            return new UpfMeter(cellId, bands, type);
        }
    }
}
