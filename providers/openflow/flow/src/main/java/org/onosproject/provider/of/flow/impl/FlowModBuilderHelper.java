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
package org.onosproject.provider.of.flow.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.EnumHashBiMap;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignalType;

/**
 * Collection of helper methods to convert protocol agnostic models to values used in OpenFlow spec.
 */
// TODO: Rename to a better name
final class FlowModBuilderHelper {

    // prohibit instantiation
    private FlowModBuilderHelper() {}

    private static final BiMap<GridType, Byte> GRID_TYPES = EnumHashBiMap.create(GridType.class);
    static {
        // See ONF "Optical Transport Protocol Extensions Version 1.0" for the following values
        GRID_TYPES.put(GridType.DWDM, (byte) 1); // OFPGRIDT_DWDM of enum ofp_grid_type
        GRID_TYPES.put(GridType.CWDM, (byte) 2); // OFPGRIDT_CWDM of enum ofp_grid_type
        GRID_TYPES.put(GridType.FLEX, (byte) 3); // OFPGRIDT_FLEX of enum ofp_grid_type
    }

    private static final BiMap<ChannelSpacing, Byte> CHANNEL_SPACING = EnumHashBiMap.create(ChannelSpacing.class);
    static {
        // See ONF "Optical Transport Protocol Extensions Version 1.0" for the following values
        CHANNEL_SPACING.put(ChannelSpacing.CHL_100GHZ, (byte) 1);   // OFPCS_100GHZ of enum ofp_chl_spacing
        CHANNEL_SPACING.put(ChannelSpacing.CHL_50GHZ, (byte) 2);    // OFPCS_50GHZ of enum ofp_chl_spacing
        CHANNEL_SPACING.put(ChannelSpacing.CHL_25GHZ, (byte) 3);    // OFPCS_25GHZ of enum ofp_chl_spacing
        CHANNEL_SPACING.put(ChannelSpacing.CHL_12P5GHZ, (byte) 4);  // OFPCS_12P5GHZ of enum ofp_chl_spacing
        CHANNEL_SPACING.put(ChannelSpacing.CHL_6P25GHZ, (byte) 5);  // OFPCS_6P25GHZ of enum ofp_chl_spacing
    }

    private static final BiMap<OchSignalType, Byte> OCH_SIGNAL_TYPES = EnumHashBiMap.create(OchSignalType.class);
    static {
        // See ONF "Optical Transport Protocol Extensions Version 1.0" for the following values
        OCH_SIGNAL_TYPES.put(OchSignalType.FIXED_GRID, (byte) 1); // OFPOCHT_FIX_GRID of enum ofp_och_signal_type
        OCH_SIGNAL_TYPES.put(OchSignalType.FLEX_GRID, (byte) 2);  // OFPOCHT_FLEX_GRID of enum ofp_och_signal_type
    }

    /**
     * Converts the specified input value to the corresponding value with the specified map.
     *
     * @param map bidirectional mapping
     * @param input input value
     * @param cls class of output value
     * @param <I> type of input value
     * @param <O> type of output value
     * @return the corresponding value stored in the specified map
     * @throws UnsupportedConversionException if no corresponding value is found
     */
    private static <I, O> O convert(BiMap<I, O> map, I input, Class<O> cls) {
        if (!map.containsKey(input)) {
            throw new UnsupportedConversionException(input, cls);
        }

        return map.get(input);
    }

    /**
     * Converts a {@link GridType} to the corresponding byte value defined in
     * ONF "Optical Transport Protocol Extensions Version 1.0".
     *
     * @param type grid type
     * @return the byte value corresponding to the specified grid type
     * @throws UnsupportedConversionException if the specified grid type is not supported
     */
    static byte convertGridType(GridType type) {
        return convert(GRID_TYPES, type, Byte.class);
    }

    /**
     * Converts a byte value for grid type
     * defined in ONF "Optical Transport Protocol Extensions Version 1.0"
     * to the corresponding {@link GridType} instance.
     *
     * @param type byte value as grid type defined the spec
     * @return the corresponding GridType instance
     */
    static GridType convertGridType(byte type) {
        return convert(GRID_TYPES.inverse(), type, GridType.class);
    }

    /**
     * Converts a {@link ChannelSpacing} to the corresponding byte value defined in
     * ONF "Optical Transport Protocol Extensions Version 1.0".
     *
     * @param spacing channel spacing
     * @return byte value corresponding to the specified channel spacing
     * @throws UnsupportedConversionException if the specified channel spacing is not supported
     */
    static byte convertChannelSpacing(ChannelSpacing spacing) {
        return convert(CHANNEL_SPACING, spacing, Byte.class);
    }

    /**
     * Converts a byte value for channel spacing
     * defined in ONF "Optical Transport Protocol Extensions Version 1.0"
     * to the corresponding {@link ChannelSpacing} instance.
     *
     * @param spacing byte value as channel spacing defined the spec
     * @return the corresponding ChannelSpacing instance
     */
    static ChannelSpacing convertChannelSpacing(byte spacing) {
        return convert(CHANNEL_SPACING.inverse(), spacing, ChannelSpacing.class);
    }

    /**
     * Converts a {@link OchSignalType} to the corresponding byte value.
     *
     * @param signalType optical signal type
     * @return byte value corresponding to the specified OCh signal type
     */
    static byte convertOchSignalType(OchSignalType signalType) {
        return convert(OCH_SIGNAL_TYPES, signalType, Byte.class);
    }

    /**
     * Converts a byte value for Och signal type
     * defined in ONF "Optical Transport Protocol Extensions Version 1.0"
     * to the corresponding {@link OchSignalType} instance.
     *
     * @param signalType byte value as Och singal type defined the spec
     * @return the corresponding OchSignalType instance
     */
    static OchSignalType convertOchSignalType(byte signalType) {
        return convert(OCH_SIGNAL_TYPES.inverse(), signalType, OchSignalType.class);
    }
}
