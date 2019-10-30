/*
 * Copyright 2019-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * This work was done in Nokia Bell Labs.
 *
 */

package org.onosproject.net.optical.util;


import org.onlab.util.Frequency;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * Optical Channel Utility is a set of methods to convert different
 * set of parameters to the OchSignal instance and backwards.
 */
public final class OpticalChannelUtility {

    private static final Logger log = getLogger(OpticalChannelUtility.class);
    private static final double SLOT_GRANULARITY_CONSTANT = 12.5;
    private static final int NOMINAL_CENTRAL_FREQUENCY_GHZ = 193100; // According to ITU-T 694.1

    private OpticalChannelUtility() {}

    /**
     * This method creates OchSignal instance based on Central Frequency and
     * the Slot Width of the channel.
     * @param centralFrequency - central frequency of the connection.
     * @param slotWidth - bandwidth of the optical channel.
     * @param gridType - type of the frequency grid.
     * @param channelSpacing - channel spacing.
     * @return - returns created instance of OchSignal.
     */
    public static final OchSignal createOchSignal(Frequency centralFrequency, Frequency slotWidth,
                                                  GridType gridType, ChannelSpacing channelSpacing) {

        int spacingMultiplier = computeSpacingMultiplier(centralFrequency, channelSpacing);
        int slotGranularity = computeSlotGranularity(slotWidth);

        return (new OchSignal(gridType, channelSpacing, spacingMultiplier, slotGranularity));
    }

    /**
     * This method creates OchSignal instance from frequency bounds.
     * @param lowerBound - lower bound of the frequency.
     * @param upperBound - upper bound of the frequency.
     * @param gridType - type of the frequency grid.
     * @param channelSpacing - channel spacing.
     * @return - returns created instance of OchSignal.
     */
    public static final OchSignal createOchSignalFromBounds(
            Frequency lowerBound, Frequency upperBound, GridType gridType,
            ChannelSpacing channelSpacing) {

        // Transferring everything to the frequencies
        Frequency slotWidth = upperBound.subtract(lowerBound);
        Frequency halfBw = slotWidth.floorDivision(2);
        Frequency centralFrequency = lowerBound.add(halfBw);

        int spacingMultiplier = computeSpacingMultiplier(centralFrequency, channelSpacing);
        int slotGranularity = computeSlotGranularity(slotWidth);

        return (new OchSignal(gridType, channelSpacing, spacingMultiplier, slotGranularity));
    }

    /**
     * This method extracts frequency bounds from OchSignal instance.
     * @param signal - OchSignal instance.
     * @param channelSpacing - channel spacing.
     * @return - HashMap with upper and lower bounds of frequency.
     */
    public static final Map<String, Frequency> extractOchFreqBounds(OchSignal signal, ChannelSpacing channelSpacing) {

        // Initializing variables
        int spacingMultiplier = signal.spacingMultiplier();
        int slotGranularity = signal.slotGranularity();

        // Computing central frequency
        Frequency central = computeCentralFrequency(spacingMultiplier, channelSpacing);

        // Computing HALF of slot width
        Frequency halfSlotWidth = computeSlotWidth(slotGranularity).floorDivision(2);

        // Getting frequency bounds
        Frequency minFreq = central.subtract(halfSlotWidth);
        Frequency maxFreq = central.add(halfSlotWidth);

        Map<String, Frequency> freqs = new HashMap<String, Frequency>();
        freqs.put("minFreq", minFreq);
        freqs.put("maxFreq", maxFreq);

        return freqs;
    }

    /**
     * This method extracts Central Frequency and Slot Width from OchSignal instance.
     * @param signal - OchSignal instance.
     * @param channelSpacing - channel spacing.
     * @return - HashMap with upper and lower bounds of frequency.
     */
    public static final Map<String, Frequency> extractOch(OchSignal signal, ChannelSpacing channelSpacing) {

        // Initializing variables
        int spacingMultiplier = signal.spacingMultiplier();
        int slotGranularity = signal.slotGranularity();

        // Computing central frequency
        Frequency central = computeCentralFrequency(spacingMultiplier, channelSpacing);
        // Computing slot width
        Frequency sw = computeSlotWidth(slotGranularity);

        Map<String, Frequency> freqs = new HashMap<String, Frequency>();
        freqs.put("centralFrequency", central);
        freqs.put("slotWidth", sw);

        return freqs;
    }

    /**
     * This method computes the Spacing Multiplier value
     * from Central Frequency and Channel Spacing values.
     * @param centralFrequency - central frequency.
     * @param channelSpacing - channel spacing.
     * @return - computed spacing multiplier.
     */
    public static final int computeSpacingMultiplier(
            Frequency centralFrequency, ChannelSpacing channelSpacing) {

        double centfreq = Double.parseDouble(String.valueOf(centralFrequency.asGHz()));
        // Computing spacing multiplier from definition (see OchSignal class comments)
        double spMult = (centfreq - NOMINAL_CENTRAL_FREQUENCY_GHZ) /
                Double.parseDouble(String.valueOf(channelSpacing.frequency().asGHz()));

        return ((int) (spMult));
    }

    /**
     * This method computes Slot Granularity from Slot Width value.
     * @param slotWidth - slot width.
     * @return - computed slot granularity.
     */
    public static final int computeSlotGranularity(Frequency slotWidth) {

        double slotw = Double.parseDouble(String.valueOf(slotWidth.asGHz()));
        // Computing according to the definition
        double slotgr = slotw / SLOT_GRANULARITY_CONSTANT;

        return ((int) slotgr);
    }

    /**
     * This method computes the Central Frequency value
     * from Spacing Multiplier and Channel Spacing values.
     * @param spacingMultiplier - spacing multiplier.
     * @param channelSpacing - channel spacing.
     * @return - central frequency as an instance of Frequency.
     */
    public static final Frequency computeCentralFrequency(
            int spacingMultiplier, ChannelSpacing channelSpacing) {

        // Computing central frequency
        double centralFreq = NOMINAL_CENTRAL_FREQUENCY_GHZ + spacingMultiplier *
                Double.parseDouble(String.valueOf(channelSpacing.frequency().asGHz()));

        return Frequency.ofGHz(centralFreq);
    }

    /**
     * This method computes Slot Width value from Slot Granularity value.
     * @param slotGranularity - slot granularity.
     * @return - slot width as an instance of Frequency.
     */
    public static final Frequency computeSlotWidth(int slotGranularity) {

        // Computing slot width
        double slotWidth = slotGranularity * SLOT_GRANULARITY_CONSTANT;

        return Frequency.ofGHz(slotWidth);
    }

}
