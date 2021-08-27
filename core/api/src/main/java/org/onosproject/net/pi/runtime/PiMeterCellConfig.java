/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.onosproject.net.DeviceId;

import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration of a meter cell of a protocol-independent pipeline.
 */
@Beta
public final class PiMeterCellConfig implements PiEntity {

    private final PiMeterCellId cellId;
    private final ImmutableMap<PiMeterBandType, PiMeterBand> piMeterBands;

    /**
     * Creates a new meter cell configuration for the given cell identifier and meter bands.
     *
     * @param cellId  meter cell identifier
     * @param piMeterBands meter bands
     */
    private PiMeterCellConfig(PiMeterCellId cellId, Map<PiMeterBandType, PiMeterBand> piMeterBands) {
        this.cellId = cellId;
        this.piMeterBands = ImmutableMap.copyOf(piMeterBands);
    }

    /**
     * Returns the cell identifier.
     *
     * @return cell identifier
     */
    public PiMeterCellId cellId() {
        return cellId;
    }

    /**
     * Returns the map of bands of this cell.
     *
     * @return meter bands
     */
    public Map<PiMeterBandType, PiMeterBand> meterBands() {
        return piMeterBands;
    }

    /**
     * Check if the config represents a modify operation.
     * Or it is a non-default config read from south bound.
     *
     * @return true if there are exactly 2 bands
     */
    public boolean isModifyConfig() {
        return piMeterBands.size() == 2;
    }

    /**
     * Check if the config represents a reset operation.
     * Or it is a default config read from south bound.
     *
     * @return true if there is no band
     */
    public boolean isDefaultConfig() {
        return piMeterBands.isEmpty();
    }

    /**
     * Returns the committed configuration if present.
     *
     * @return the committed band. Null otherwise
     */
    public PiMeterBand committedBand() {
        return piMeterBands.get(PiMeterBandType.COMMITTED);
    }

    /**
     * Returns the peak configuration if present.
     *
     * @return the peak band. Null otherwise
     */
    public PiMeterBand peakBand() {
        return piMeterBands.get(PiMeterBandType.PEAK);
    }

    /**
     * Returns a PiMeterCellConfig with no bands.
     * Used to reset a PI meter cell.
     *
     * @param piMeterCellId the PiMeterCellId need to be reset
     * @return a PiMeterCellConfig with no bands
     */
    public static PiMeterCellConfig reset(PiMeterCellId piMeterCellId) {
        return new PiMeterCellConfig(piMeterCellId, Collections.emptyMap());
    }

    @Override
    public PiEntityType piEntityType() {
        return PiEntityType.METER_CELL_CONFIG;
    }

    @Override
    public PiMeterCellHandle handle(DeviceId deviceId) {
        return PiMeterCellHandle.of(deviceId, this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PiMeterCellConfig)) {
            return false;
        }
        PiMeterCellConfig that = (PiMeterCellConfig) o;

        return Objects.equal(piMeterBands, that.piMeterBands) &&
                Objects.equal(cellId, that.cellId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cellId, piMeterBands);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("cellId", cellId)
                .add("meterBands", piMeterBands)
                .toString();
    }

    /**
     * Returns a meter cell configuration builder.
     *
     * @return a new builder
     */
    public static PiMeterCellConfig.Builder builder() {
        return new PiMeterCellConfig.Builder();
    }

    public static final class Builder {
        private PiMeterCellId cellId;
        private Map<PiMeterBandType, PiMeterBand> bands = Maps.newHashMap();


        private Builder() {
            // Hides constructor.
        }

        /**
         * Sets the meter cell identifier for this meter.
         *
         * @param meterCellId meter cell identifier
         * @return this
         */
        public PiMeterCellConfig.Builder withMeterCellId(PiMeterCellId meterCellId) {
            this.cellId = meterCellId;
            return this;
        }


        /**
         * Sets a meter band of this meter.
         *
         * @param band meter band
         * @return this
         */
        public PiMeterCellConfig.Builder withMeterBand(PiMeterBand band) {
            this.bands.put(band.type(), band);
            return this;
        }

        /**
         * Sets the committed band of this meter.
         *
         * @param rate committed rate
         * @param burst committed burst
         * @return this
         */
        public PiMeterCellConfig.Builder withCommittedBand(long rate, long burst) {
            this.bands.put(PiMeterBandType.COMMITTED, new PiMeterBand(PiMeterBandType.COMMITTED, rate, burst));
            return this;
        }

        /**
         * Sets the peak band of this meter.
         *
         * @param rate peak rate
         * @param burst peak burst
         * @return this
         */
        public PiMeterCellConfig.Builder withPeakBand(long rate, long burst) {
            this.bands.put(PiMeterBandType.PEAK, new PiMeterBand(PiMeterBandType.PEAK, rate, burst));
            return this;
        }

        /**
         * Builds the meter cell configuration.
         *
         * @return a new meter cell configuration
         */
        public PiMeterCellConfig build() {
            checkNotNull(cellId);
            return new PiMeterCellConfig(cellId, bands);
        }
    }
}
