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
package org.onosproject.provider.of.message.impl;

import com.google.common.collect.BiMap;
import com.google.common.collect.EnumHashBiMap;
import org.onosproject.cpman.ControlMessage;
import org.projectfloodlight.openflow.protocol.OFType;

import static org.projectfloodlight.openflow.protocol.OFType.*;
import static org.onosproject.cpman.ControlMessage.Type.*;

/**
 * Collection of helper methods to convert protocol agnostic control message to
 * messages used in OpenFlow specification.
 */
public final class OpenFlowControlMessageMapper {

    // prohibit instantiation
    private OpenFlowControlMessageMapper() {
    }

    private static final BiMap<OFType, ControlMessage.Type> MESSAGE_TYPE =
            EnumHashBiMap.create(OFType.class);

    static {
        // key is OpenFlow specific OFType
        // value is protocol agnostic ControlMessage.Type
        MESSAGE_TYPE.put(PACKET_IN, INBOUND_PACKET);
        MESSAGE_TYPE.put(PACKET_OUT, OUTBOUND_PACKET);
        MESSAGE_TYPE.put(FLOW_MOD, FLOW_MOD_PACKET);
        MESSAGE_TYPE.put(FLOW_REMOVED, FLOW_REMOVED_PACKET);
        MESSAGE_TYPE.put(STATS_REQUEST, REQUEST_PACKET);
        MESSAGE_TYPE.put(STATS_REPLY, REPLY_PACKET);
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
     * Looks up the corresponding {@link ControlMessage.Type} instance
     * from the specified OFType value for OpenFlow message type.
     *
     * @param type OpenFlow message type
     * @return protocol agnostic control message type
     */
    public static ControlMessage.Type lookupControlMessageType(OFType type) {
        return lookup(MESSAGE_TYPE, type, ControlMessage.Type.class);
    }

    /**
     * Looks up the corresponding {@link OFType} instance from the specified
     * ControlMetricType value.
     *
     * @param type control message type
     * @return OpenFlow specific message type
     */
    public static OFType lookupOFType(ControlMessage.Type type) {
        return lookup(MESSAGE_TYPE.inverse(), type, OFType.class);
    }
}
