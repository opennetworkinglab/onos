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
import com.google.common.collect.ImmutableList;
import org.onosproject.net.DeviceId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configuration of a meter cell of a protocol-independent pipeline.
 */
@Beta
public final class PiMeterCellConfig implements PiEntity {

    private final PiMeterCellId cellId;
    private final ImmutableList<PiMeterBand> piMeterBands;

    /**
     * Creates a new meter cell configuration for the given cell identifier and meter bands.
     *
     * @param cellId  meter cell identifier
     * @param piMeterBands meter bands
     */
    private PiMeterCellConfig(PiMeterCellId cellId, Collection<PiMeterBand> piMeterBands) {
        this.cellId = cellId;
        this.piMeterBands = ImmutableList.copyOf(piMeterBands);
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
     * Returns the collection of bands of this cell.
     *
     * @return meter bands
     */
    public Collection<PiMeterBand> meterBands() {
        return piMeterBands;
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

        return piMeterBands.containsAll((that.piMeterBands)) &&
                piMeterBands.size() == that.piMeterBands.size() &&
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
        private  PiMeterCellId cellId;
        private List<PiMeterBand> bands = new ArrayList<>();


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
            this.bands.add(band);
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
