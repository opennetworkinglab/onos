/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.net.optical.json;

import static com.google.common.base.Preconditions.checkArgument;

import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.Beta;

// TODO define common interface for JsonCodec for annotation?
// unlike existing JsonCodec, this use-case requires that encode/decode is
// reversible.  (e.g., obj.equals(decode(encode(obj))))
/**
 * JSON codec for OchSignal.
 */
@Beta
public abstract class OchSignalCodec {

    // TODO should probably use shared mapper across optical codecs.
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Creates an instance of {@link OchSignal} from JSON representation.
     *
     * @param obj JSON Object representing OchSignal
     * @return OchSignal
     * @throws IllegalArgumentException - if JSON object is ill-formed
     * @see OchSignalCodec#encode(OchSignal)
     */
    public static OchSignal decode(ObjectNode obj) {
        final GridType gridType;
        final ChannelSpacing channelSpacing;
        final int spacingMultiplier;
        final int slotGranularity;

        String s;
        s = obj.get("channelSpacing").textValue();
        checkArgument(s != null, "ill-formed channelSpacing");
        channelSpacing = Enum.valueOf(ChannelSpacing.class, s);

        s = obj.get("gridType").textValue();
        checkArgument(s != null, "ill-formed gridType");
        gridType = Enum.valueOf(GridType.class, s);

        JsonNode node;
        node = obj.get("spacingMultiplier");
        checkArgument(node.canConvertToInt(), "ill-formed spacingMultiplier");
        spacingMultiplier = node.asInt();

        node = obj.get("slotGranularity");
        checkArgument(node.canConvertToInt(), "ill-formed slotGranularity");
        slotGranularity = node.asInt();

        return new OchSignal(gridType, channelSpacing, spacingMultiplier, slotGranularity);
    }

    /**
     * Returns a JSON Object representation of this instance.
     *
     * @param j Och signal object
     * @return JSON Object representing OchSignal
     */
    public static ObjectNode encode(OchSignal j) {
        ObjectNode obj = MAPPER.createObjectNode();
        obj.put("channelSpacing", j.channelSpacing().toString());
        obj.put("gridType", j.gridType().toString());
        obj.put("slotGranularity", j.slotGranularity());
        obj.put("spacingMultiplier", j.spacingMultiplier());
        return obj;
    }

}
