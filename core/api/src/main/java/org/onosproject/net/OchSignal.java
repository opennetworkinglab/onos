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
package org.onosproject.net;

import com.google.common.collect.ImmutableSet;
import org.onlab.util.Frequency;
import org.onlab.util.Spectrum;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.ChannelSpacing.CHL_12P5GHZ;
import static org.onosproject.net.ChannelSpacing.CHL_6P25GHZ;
import static org.onosproject.net.GridType.DWDM;
import static org.onosproject.net.GridType.FLEX;

/**
 * Implementation of Lambda representing OCh (Optical Channel) Signal.
 *
 * <p>
 * See ITU G.709 "Interfaces for the Optical Transport Network (OTN)".
 *     ITU G.694.1 "Spectral grids for WDM applications: DWDM frequency grid".
 * </p>
 */
public class OchSignal implements Lambda {

    public static final Set<Integer> FIXED_GRID_SLOT_GRANULARITIES = ImmutableSet.of(1, 2, 4, 8);
    private static final GridType DEFAULT_OCH_GRIDTYPE = GridType.DWDM;
    private static final ChannelSpacing DEFAULT_CHANNEL_SPACING = ChannelSpacing.CHL_50GHZ;

    private final GridType gridType;
    private final ChannelSpacing channelSpacing;
    // Nominal central frequency = 193.1 THz + spacingMultiplier * channelSpacing
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
     * Creates an instance of {@link OchSignal} representing a flex grid frequency slot.
     * @param index     slot index (relative to "the center frequency" 193.1THz)
     * @return FlexGrid {@link OchSignal}
     */
    public static OchSignal newFlexGridSlot(int index) {
        return new OchSignal(FLEX, CHL_6P25GHZ, index, 1);
    }

    /**
     * Creates an instance of {@link OchSignal} representing a fixed DWDM frequency slot.
     * @param spacing   channel spacing
     * @param index     slot index (relative to "the center frequency" 193.1THz)
     * @return DWDM {@link OchSignal}
     */
    public static OchSignal newDwdmSlot(ChannelSpacing spacing, int index) {
        return new OchSignal(DWDM, spacing, index,
                             (int) (spacing.frequency().asHz() / CHL_12P5GHZ.frequency().asHz()));
    }

    /**
     * Creates OCh signal.
     *
     * @param centerFrequency frequency
     * @param channelSpacing spacing
     * @param slotGranularity granularity
     * @deprecated 1.4.0 Emu Release
     */
    @Deprecated
    public OchSignal(Frequency centerFrequency, ChannelSpacing channelSpacing, int slotGranularity) {
        this.gridType = DEFAULT_OCH_GRIDTYPE;
        this.channelSpacing = channelSpacing;
        this.spacingMultiplier = (int) Math.round((double) centerFrequency.
                subtract(Spectrum.CENTER_FREQUENCY).asHz() / channelSpacing().frequency().asHz());
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
     * Returns slot width granularity.
     *
     * @return slot width granularity
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
        return Spectrum.CENTER_FREQUENCY.add(channelSpacing().frequency().multiply(spacingMultiplier));
    }

    /**
     * Returns slot width.
     *
     * @return slot width
     */
    public Frequency slotWidth() {
        return ChannelSpacing.CHL_12P5GHZ.frequency().multiply(slotGranularity);
    }

    /**
     * Convert fixed grid OCh signal to sorted set of flex grid slots with 6.25 GHz spacing and 12.5 GHz slot width.
     *
     * @param ochSignal fixed grid lambda
     * @return sorted set of flex grid OCh lambdas
     */
    public static SortedSet<OchSignal> toFlexGrid(OchSignal ochSignal) {
        checkArgument(ochSignal.gridType() != GridType.FLEX, ochSignal.gridType());
        checkArgument(ochSignal.channelSpacing() != ChannelSpacing.CHL_6P25GHZ, ochSignal.channelSpacing());
        checkArgument(FIXED_GRID_SLOT_GRANULARITIES.contains(ochSignal.slotGranularity()), ochSignal.slotGranularity());

        int startMultiplier = (int) (1 - ochSignal.slotGranularity() +
                ochSignal.spacingMultiplier() * ochSignal.channelSpacing().frequency().asHz() /
                        ChannelSpacing.CHL_6P25GHZ.frequency().asHz());

        return IntStream.range(0, ochSignal.slotGranularity())
                .mapToObj(i -> new OchSignal(GridType.FLEX, ChannelSpacing.CHL_6P25GHZ, startMultiplier + 2 * i, 1))
                .collect(Collectors.toCollection(DefaultOchSignalComparator::newOchSignalTreeSet));
    }

    /**
     * Convert list of lambdas with flex grid 6.25 GHz spacing and 12.5 GHz width into fixed grid OCh signal.
     *
     * @param lambdas list of flex grid lambdas in sorted order
     * @param spacing desired fixed grid spacing
     * @return fixed grid lambda
     */
    public static OchSignal toFixedGrid(List<OchSignal> lambdas, ChannelSpacing spacing) {
        // Number of slots of 12.5 GHz that fit into requested spacing
        int ratio = (int) (spacing.frequency().asHz() / ChannelSpacing.CHL_12P5GHZ.frequency().asHz());
        checkArgument(lambdas.size() == ratio, "%s != %s", lambdas.size(), ratio);
        lambdas.forEach(x -> checkArgument(x.gridType() == GridType.FLEX, x.gridType()));
        lambdas.forEach(x -> checkArgument(x.channelSpacing() == ChannelSpacing.CHL_6P25GHZ, x.channelSpacing()));
        lambdas.forEach(x -> checkArgument(x.slotGranularity() == 1, x.slotGranularity()));
        // Consecutive lambdas (multiplier increments by 2 because spacing is 6.25 GHz but slot width is 12.5 GHz)
        IntStream.range(1, lambdas.size())
                .forEach(i -> checkArgument(
                        lambdas.get(i).spacingMultiplier() == lambdas.get(i - 1).spacingMultiplier() + 2));
        // Is center frequency compatible with requested spacing
        Frequency center = lambdas.get(ratio / 2).centralFrequency().subtract(ChannelSpacing.CHL_6P25GHZ.frequency());
        checkArgument(Spectrum.CENTER_FREQUENCY.subtract(center).asHz() % spacing.frequency().asHz() == 0);

        // Multiplier sits in middle of given lambdas, then convert from 6.25 to requested spacing
        int spacingMultiplier = lambdas.stream()
            .mapToInt(OchSignal::spacingMultiplier)
            .sum() / lambdas.size()
            / (int) (spacing.frequency().asHz() / CHL_6P25GHZ.frequency().asHz());

        return new OchSignal(GridType.DWDM, spacing, spacingMultiplier, lambdas.size());
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
        return String.format("%s{%+d×%.2fGHz ± %.2fGHz}",
                this.getClass().getSimpleName(),
                spacingMultiplier,
                (double) channelSpacing.frequency().asHz() / Frequency.ofGHz(1).asHz(),
                (double) slotGranularity * ChannelSpacing.CHL_12P5GHZ.frequency().asHz()
                        / Frequency.ofGHz(1).asHz() / 2.0);
    }
}
