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
package org.onosproject.provider.of.device.impl;

import org.onosproject.net.OduSignalType;

import com.google.common.collect.BiMap;
import com.google.common.collect.EnumHashBiMap;

/**
 * Collection of helper methods to convert protocol agnostic models to values used in OpenFlow spec.
 */
final class OpenFlowDeviceValueMapper {

    // prohibit instantiation
    private OpenFlowDeviceValueMapper() {}

    private static final BiMap<OduSignalType, Byte> ODU_SIGNAL_TYPES = EnumHashBiMap.create(OduSignalType.class);
    static {
        // See ONF "Optical Transport Protocol Extensions Version 1.0" for the following values
        ODU_SIGNAL_TYPES.put(OduSignalType.ODU1, (byte) 1);         // OFPODUT_ODU1 of enum ofp_odu_signal_type
        ODU_SIGNAL_TYPES.put(OduSignalType.ODU2, (byte) 2);         // OFPODUT_ODU2 of enum ofp_odu_signal_type
        ODU_SIGNAL_TYPES.put(OduSignalType.ODU3, (byte) 3);         // OFPODUT_ODU3 of enum ofp_odu_signal_type
        ODU_SIGNAL_TYPES.put(OduSignalType.ODU4, (byte) 4);         // OFPODUT_ODU4 of enum ofp_odu_signal_type
        ODU_SIGNAL_TYPES.put(OduSignalType.ODU0, (byte) 10);        // OFPODUT_ODU0 of enum ofp_odu_signal_type
        ODU_SIGNAL_TYPES.put(OduSignalType.ODU2e, (byte) 11);       // OFPODUT_ODU2E of enum ofp_odu_signal_type
    }

    /**
     * Looks up the specified input value to the corresponding value with the specified map.
     *
     * @param map bidirectional mapping
     * @param input input value
     * @param cls class of output value
     * @param <I> type of input value
     * @param <O> type of output value
     * @return the corresponding value stored in the specified map
     */
    private static <I, O> O lookup(BiMap<I, O> map, I input, Class<O> cls) {
        if (!map.containsKey(input)) {
            throw new RuntimeException(
                    String.format("No mapping found for %s when converting to %s", input, cls.getName()));
        }

        return map.get(input);
    }

    /**
     * Looks up the the corresponding {@link OduSignalType} instance
     * from the specified byte value for ODU signal type defined in
     * ONF "Optical Transport Protocol Extensions Version 1.0".
     *
     * @param signalType byte value as ODU (Optical channel Data Unit) signal type defined the spec
     * @return the corresponding OchSignalType instance
     */
    static OduSignalType lookupOduSignalType(byte signalType) {
        return lookup(ODU_SIGNAL_TYPES.inverse(), signalType, OduSignalType.class);
    }

}
