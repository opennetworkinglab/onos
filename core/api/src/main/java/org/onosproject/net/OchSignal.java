/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.net;

import com.google.common.base.MoreObjects;
import org.onlab.util.Frequency;

import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Implementation of Lambda representing OCh (Optical Channel) Signal.
 *
 * <p>
 * See ITU G.709 "Interfaces for the Optical Transport Network (OTN)".
 * </p>
 */
public class OchSignal implements Lambda {

    public static final Frequency CENTER_FREQUENCY = Frequency.ofTHz(193.1);
    public static final Frequency FLEX_GRID_SLOT = Frequency.ofGHz(12.5);
    private static final GridType DEFAULT_OCH_GRIDTYPE = GridType.DWDM;
    private static final ChannelSpacing DEFAULT_CHANNEL_SPACING = ChannelSpacing.CHL_50GHZ;


    private final GridType gridType;
    private final ChannelSpacing channelSpacing;
    // Frequency = 193.1 THz + spacingMultiplier * channelSpacing
    private final int spacingMultiplier;
    // Slot width = slotGranularity * 12.5 GHz
    private final int slotGranularity;

    /**
     * Creates an instance with the specified arguments.
     * It it recommended to use {@link Lambda#ochSignal(GridType, ChannelSpacing, int, int)}
     * unless you want to use the concrete type, OchSignal, directly.
     *
     * @param gridType          grid type
     * @param channelSpacing    channel spacing
     * @param spacingMultiplier channel spacing multiplier
     * @param slotGranularity   slot width granularity
     */
    public OchSignal(GridType gridType, ChannelSpacing channelSpacing,
              int spacingMultiplier, int slotGranularity) {
        this.gridType = checkNotNull(gridType);
        this.channelSpacing = checkNotNull(channelSpacing);
        // Negative values are permitted for spacingMultiplier
        this.spacingMultiplier = spacingMultiplier;
        checkArgument(slotGranularity > 0, "slotGranularity must be larger than 0, received %s", slotGranularity);
        this.slotGranularity = slotGranularity;
    }

    /**
     * Create OCh signal from channel number.
     *
     * @param channel channel number
     * @param maxFrequency maximum frequency
     * @param grid grid spacing frequency
     */
    public OchSignal(int channel, Frequency maxFrequency, Frequency grid) {
        // Calculate center frequency
        Frequency centerFrequency = maxFrequency.subtract(grid.multiply(channel - 1));

        this.gridType = DEFAULT_OCH_GRIDTYPE;
        this.channelSpacing = DEFAULT_CHANNEL_SPACING;
        this.spacingMultiplier = (int) (centerFrequency.subtract(OchSignal.CENTER_FREQUENCY).asHz() / grid.asHz());
        this.slotGranularity = (int) Math.round((double) grid.asHz() / ChannelSpacing.CHL_12P5GHZ.frequency().asHz());
    }

    public OchSignal(Frequency centerFrequency, ChannelSpacing channelSpacing, int slotGranularity) {
        this.gridType = DEFAULT_OCH_GRIDTYPE;
        this.channelSpacing = channelSpacing;
        this.spacingMultiplier = (int) Math.round((double) centerFrequency.
                subtract(OchSignal.CENTER_FREQUENCY).asHz() / channelSpacing().frequency().asHz());
        this.slotGranularity = slotGranularity;
    }

    /**
     * Returns grid type.
     *
     * @return grid type
     */
    public GridType gridType() {
        return gridType;
    }

    /**
     * Returns channel spacing.
     *
     * @return channel spacing
     */
    public ChannelSpacing channelSpacing() {
        return channelSpacing;
    }

    /**
     * Returns spacing multiplier.
     *
     * @return spacing multiplier
     */
    public int spacingMultiplier() {
        return spacingMultiplier;
    }

    /**
     * Returns slow width granularity.
     *
     * @return slow width granularity
     */
    public int slotGranularity() {
        return slotGranularity;
    }

    /**
     * Returns central frequency in MHz.
     *
     * @return frequency in MHz
     */
    public Frequency centralFrequency() {
        return CENTER_FREQUENCY.add(channelSpacing().frequency().multiply(spacingMultiplier));
    }

    /**
     * Returns slot width.
     *
     * @return slot width
     */
    public Frequency slotWidth() {
        return FLEX_GRID_SLOT.multiply(slotGranularity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gridType, channelSpacing, spacingMultiplier, slotGranularity);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof OchSignal)) {
            return false;
        }
        final OchSignal other = (OchSignal) obj;
        return Objects.equals(this.gridType, other.gridType)
                && Objects.equals(this.channelSpacing, other.channelSpacing)
                && Objects.equals(this.spacingMultiplier, other.spacingMultiplier)
                && Objects.equals(this.slotGranularity, other.slotGranularity);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("gridType", gridType)
                .add("channelSpacing", channelSpacing)
                .add("spacingMultiplier", spacingMultiplier)
                .add("slotGranularity", slotGranularity)
                .toString();
    }
}
