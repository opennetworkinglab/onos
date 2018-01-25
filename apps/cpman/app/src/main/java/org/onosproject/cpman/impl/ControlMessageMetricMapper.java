/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.cpman.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.EnumHashBiMap;
import org.onosproject.cpman.ControlMessage;
import org.onosproject.cpman.ControlMetricType;

import static org.onosproject.cpman.ControlMessage.Type;

/**
 * Collection of helper methods to convert protocol agnostic control message
 * type to control metric type.
 */
public final class ControlMessageMetricMapper {

    // prohibit instantiation
    private ControlMessageMetricMapper() {
    }

    private static final BiMap<ControlMessage.Type, ControlMetricType> MESSAGE_TYPE =
            EnumHashBiMap.create(ControlMessage.Type.class);

    static {
        // key is protocol agnostic ControlMessage.Type
        // value is ControlMetricType
        MESSAGE_TYPE.put(Type.INBOUND_PACKET, ControlMetricType.INBOUND_PACKET);
        MESSAGE_TYPE.put(Type.OUTBOUND_PACKET, ControlMetricType.OUTBOUND_PACKET);
        MESSAGE_TYPE.put(Type.FLOW_MOD_PACKET, ControlMetricType.FLOW_MOD_PACKET);
        MESSAGE_TYPE.put(Type.FLOW_REMOVED_PACKET, ControlMetricType.FLOW_REMOVED_PACKET);
        MESSAGE_TYPE.put(Type.REQUEST_PACKET, ControlMetricType.REQUEST_PACKET);
        MESSAGE_TYPE.put(Type.REPLY_PACKET, ControlMetricType.REPLY_PACKET);
    }

    /**
     * Looks up the specified input value to the corresponding value with the specified map.
     *
     * @param map   bidirectional mapping
     * @param input input type
     * @param cls   class of output value
     * @param <I>   type of input value
     * @param <O>   type of output value
     * @return the corresponding value stored in the specified map
     */
    private static <I, O> O lookup(BiMap<I, O> map, I input, Class<O> cls) {
        if (!map.containsKey(input)) {
            throw new IllegalArgumentException(
                    String.format("No mapping found for %s when converting to %s",
                            input, cls.getName()));
        }
        return map.get(input);
    }

    /**
     * Looks up the corresponding {@link ControlMetricType} instance
     * from the ControlMessage.Type for control message type.
     *
     * @param type control message type
     * @return control metric type
     */
    public static ControlMetricType lookupControlMetricType(ControlMessage.Type type) {
        return lookup(MESSAGE_TYPE, type, ControlMetricType.class);
    }

    /**
     * Looks up the corresponding {@link ControlMessage.Type} instance from
     * the specified control metric type.
     *
     * @param type control metric type
     * @return control message type
     */
    public static ControlMessage.Type lookupControlMessageType(ControlMetricType type) {
        return lookup(MESSAGE_TYPE.inverse(), type, ControlMessage.Type.class);
    }
}
