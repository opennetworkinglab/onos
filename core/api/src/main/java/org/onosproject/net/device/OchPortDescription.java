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
package org.onosproject.net.device;

import com.google.common.base.MoreObjects;
import org.onosproject.net.OchPort;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;

/**
 * Default implementation of immutable OCh port description.
 */
public class OchPortDescription extends DefaultPortDescription {

    private final OchPort.SignalType signalType;
    private final boolean isTunable;
    private final OchPort.GridType gridType;
    private final OchPort.ChannelSpacing channelSpacing;
    // Frequency = 193.1 THz + spacingMultiplier * channelSpacing
    private final int spacingMultiplier;
    // Slot width = slotGranularity * 12.5 GHz
    private final int slotGranularity;

    /**
     * Creates OCH port description based on the supplied information.
     *
     * @param number            port number
     * @param isEnabled         port enabled state
     * @param signalType        ODU signal type
     * @param isTunable         tunable wavelength capability
     * @param gridType          grid type
     * @param channelSpacing    channel spacing
     * @param spacingMultiplier channel spacing multiplier
     * @param slotGranularity   slow width granularity
     * @param annotations       optional key/value annotations map
     */
    public OchPortDescription(PortNumber number, boolean isEnabled, OchPort.SignalType signalType,
                              boolean isTunable, OchPort.GridType gridType,
                              OchPort.ChannelSpacing channelSpacing,
                              int spacingMultiplier, int slotGranularity, SparseAnnotations... annotations) {
        super(number, isEnabled, Port.Type.OCH, 0, annotations);
        this.signalType = signalType;
        this.isTunable = isTunable;
        this.gridType = gridType;
        this.channelSpacing = channelSpacing;
        this.spacingMultiplier = spacingMultiplier;
        this.slotGranularity = slotGranularity;
    }

    /**
     * Creates OCH port description based on the supplied information.
     *
     * @param base              PortDescription to get basic information from
     * @param signalType        ODU signal type
     * @param isTunable         tunable wavelength capability
     * @param gridType          grid type
     * @param channelSpacing    channel spacing
     * @param spacingMultiplier channel spacing multiplier
     * @param slotGranularity   slot width granularity
     * @param annotations       optional key/value annotations map
     */
    public OchPortDescription(PortDescription base, OchPort.SignalType signalType, boolean isTunable,
                              OchPort.GridType gridType, OchPort.ChannelSpacing channelSpacing,
                              int spacingMultiplier, int slotGranularity, SparseAnnotations annotations) {
        super(base, annotations);
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
    public OchPort.SignalType signalType() {
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
    public OchPort.GridType gridType() {
        return gridType;
    }

    /**
     * Returns channel spacing.
     *
     * @return channel spacing
     */
    public OchPort.ChannelSpacing channelSpacing() {
        return channelSpacing;
    }

    /**
     * Returns channel spacing multiplier.
     *
     * @return channel spacing multiplier
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("number", portNumber())
                .add("isEnabled", isEnabled())
                .add("type", type())
                .add("signalType", signalType)
                .add("isTunable", isTunable)
                .add("gridType", gridType)
                .add("channelSpacing", channelSpacing)
                .add("spacingMultiplier", spacingMultiplier)
                .add("slotGranularity", slotGranularity)
                .add("annotations", annotations())
                .toString();
    }

}
