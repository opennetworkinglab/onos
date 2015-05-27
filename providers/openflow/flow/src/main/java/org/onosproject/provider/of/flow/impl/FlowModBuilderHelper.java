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

import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignalType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collection of helper methods to convert protocol agnostic models to values used in OpenFlow spec.
 */
final class FlowModBuilderHelper {

    private static final Logger log = LoggerFactory.getLogger(FlowModBuilderHelper.class);

    // prohibit instantiation
    private FlowModBuilderHelper() {}

    /**
     * Converts a {@link GridType} to the corresponding byte value defined in
     * ONF "Optical Transport Protocol Extensions Version 1.0".
     *
     * @param type grid type
     * @return the byte value corresponding to the specified grid type
     * @throws UnsupportedGridTypeException if the specified grid type is not supported
     */
    static byte convertGridType(GridType type) {
        // See ONF "Optical Transport Protocol Extensions Version 1.0"
        // for the following values
        switch (type) {
            case DWDM:
                // OFPGRIDT_DWDM of enum ofp_grid_type
                return 1;
            case CWDM:
                // OFPGRIDT_CWDM of enum ofp_grid_type
                return 2;
            case FLEX:
                // OFPGRIDT_FLEX of enum ofp_grid_type
                return 3;
            default:
                throw new UnsupportedGridTypeException(type);
        }
    }

    /**
     * Converts a {@link ChannelSpacing} to the corresponding byte value defined in
     * ONF "Optical Transport Protocol Extensions Version 1.0".
     *
     * @param spacing channel spacing
     * @return byte value corresponding to the specified channel spacing
     * @throws UnsupportedChannelSpacingException if the specified channel spacing is not supported
     */
    static byte convertChannelSpacing(ChannelSpacing spacing) {
        // See ONF "Optical Transport Protocol Extensions Version 1.0"
        // for the following values
        switch (spacing) {
            case CHL_100GHZ:
                // OFPCS_100GHZ of enum ofp_chl_spacing
                return 1;
            case CHL_50GHZ:
                // OFPCS_50GHZ of enum ofp_chl_spacing
                return 2;
            case CHL_25GHZ:
                // OFPCS_25GHZ of enum ofp_chl_spacing
                return 3;
            case CHL_12P5GHZ:
                // OFPCS_12P5GHZ of enum ofp_chl_spacing
                return 4;
            case CHL_6P25GHZ:
                // OFPCS_6P25GHZ of enum ofp_chl_spacing
                return 5;
            default:
                throw new UnsupportedChannelSpacingException(spacing);
        }
    }

    /**
     * Converts a {@link OchSignalType} to the corresponding byte value.
     *
     * @param signalType optical signal type
     * @return byte value corresponding to the specified OCh signal type
     */
    static byte convertOchSignalType(OchSignalType signalType) {
        switch (signalType) {
            case FIXED_GRID:
                return (byte) 1;
            case FLEX_GRID:
                return (byte) 2;
            default:
                log.info("OchSignalType {} is not supported", signalType);
                return (byte) 0;
        }
    }
}
