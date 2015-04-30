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

import org.onlab.util.Frequency;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Implementation of OCh port (Optical Channel).
 * Also referred to as a line side port (L-port) or narrow band port.
 * See ITU G.709 "Interfaces for the Optical Transport Network (OTN)"
 */
public class OchPort extends DefaultPort {

    public static final Frequency CENTER_FREQUENCY = Frequency.ofTHz(193.1);
    public static final Frequency FLEX_GRID_SLOT = Frequency.ofGHz(12.5);

    private final OduSignalType signalType;
    private final boolean isTunable;
    private final GridType gridType;
    private final ChannelSpacing channelSpacing;
    // Frequency = 193.1 THz + spacingMultiplier * channelSpacing
    private final int spacingMultiplier;
    // Slot width = slotGranularity * 12.5 GHz
    private final int slotGranularity;


    /**
     * Creates an OCh port in the specified network element.
     *
     * @param element               parent network element
     * @param number                port number
     * @param isEnabled             port enabled state
     * @param signalType            ODU signal type
     * @param isTunable             maximum frequency in MHz
     * @param gridType              grid type
     * @param channelSpacing        channel spacing
     * @param spacingMultiplier     channel spacing multiplier
     * @param slotGranularity       slot width granularity
     * @param annotations           optional key/value annotations
     */
    public OchPort(Element element, PortNumber number, boolean isEnabled, OduSignalType signalType,
                   boolean isTunable, GridType gridType, ChannelSpacing channelSpacing,
                   int spacingMultiplier, int slotGranularity, Annotations... annotations) {
        super(element, number, isEnabled, Type.OCH, 0, annotations);
        this.signalType = signalType;
        this.isTunable = isTunable;
        this.gridType = gridType;
        this.channelSpacing = channelSpacing;
        this.spacingMultiplier = spacingMultiplier;
        this.slotGranularity = slotGranularity;
    }

    /**
     * Returns ODU signal type.
     *
     * @return ODU signal type
     */
    public OduSignalType signalType() {
        return signalType;
    }

    /**
     * Returns true if port is wavelength tunable.
     *
     * @return tunable wavelength capability
     */
    public boolean isTunable() {
        return isTunable;
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
        return Objects.hash(number(), isEnabled(), type(), signalType, isTunable,
                gridType, channelSpacing, spacingMultiplier, slotGranularity, annotations());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OchPort) {
            final OchPort other = (OchPort) obj;
            return Objects.equals(this.element().id(), other.element().id()) &&
                    Objects.equals(this.number(), other.number()) &&
                    Objects.equals(this.isEnabled(), other.isEnabled()) &&
                    Objects.equals(this.signalType, other.signalType) &&
                    Objects.equals(this.isTunable, other.isTunable) &&
                    Objects.equals(this.gridType, other.gridType) &&
                    Objects.equals(this.channelSpacing, other.channelSpacing) &&
                    Objects.equals(this.spacingMultiplier, other.spacingMultiplier) &&
                    Objects.equals(this.slotGranularity, other.slotGranularity) &&
                    Objects.equals(this.annotations(), other.annotations());
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("element", element().id())
                .add("number", number())
                .add("isEnabled", isEnabled())
                .add("type", type())
                .add("signalType", signalType)
                .add("isTunable", isTunable)
                .add("gridType", gridType)
                .add("channelSpacing", channelSpacing)
                .add("spacingMultiplier", spacingMultiplier)
                .add("slotGranularity", slotGranularity)
                .toString();
    }
}
