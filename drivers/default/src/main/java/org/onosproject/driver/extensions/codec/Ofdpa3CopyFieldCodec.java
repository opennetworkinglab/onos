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

package org.onosproject.driver.extensions.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.driver.extensions.Ofdpa3CopyField;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * JSON Codec for Ofdpa copy field class.
 */
public class Ofdpa3CopyFieldCodec extends JsonCodec<Ofdpa3CopyField> {

    private static final String N_BITS = "nBits";
    private static final String SRC = "src";
    private static final String DST = "dst";
    private static final String SRC_OFFSET = "srcOffset";
    private static final String DST_OFFSET = "dstOffset";

    private static final String MISSING_MEMBER_MESSAGE = " member is required in Ofdpa3CopyField";
    private static final String MISSING_COPY_FIELD_MESSAGE = "CopyField can not be null";

    @Override
    public ObjectNode encode(Ofdpa3CopyField copyField, CodecContext context) {
        checkNotNull(copyField, MISSING_COPY_FIELD_MESSAGE);
        return context.mapper().createObjectNode()
                .put(N_BITS, copyField.getnBits())
                .put(SRC_OFFSET, copyField.getSrcOffset())
                .put(DST_OFFSET, copyField.getDstOffset())
                .put(SRC, copyField.getSrc())
                .put(DST, copyField.getDst());
    }

    @Override
    public Ofdpa3CopyField decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse members of copy field action
        int nBits = (int) nullIsIllegal(json.get(N_BITS),
                                        N_BITS + MISSING_MEMBER_MESSAGE).asInt();
        int src = (int) nullIsIllegal(json.get(SRC),
                                      SRC + MISSING_MEMBER_MESSAGE).asInt();
        int dst = (int) nullIsIllegal(json.get(DST),
                                      DST + MISSING_MEMBER_MESSAGE).asInt();
        int srcOffset = (int) nullIsIllegal(json.get(SRC_OFFSET),
                                            SRC_OFFSET + MISSING_MEMBER_MESSAGE).asInt();
        int dstOffset = (int) nullIsIllegal(json.get(DST_OFFSET),
                                            DST_OFFSET + MISSING_MEMBER_MESSAGE).asInt();
        return new Ofdpa3CopyField(nBits, src, dst, srcOffset, dstOffset);
    }
}
