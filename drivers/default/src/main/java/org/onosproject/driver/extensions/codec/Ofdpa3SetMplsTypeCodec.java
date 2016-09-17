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

package org.onosproject.driver.extensions.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.driver.extensions.Ofdpa3SetMplsType;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * JSON Codec for Ofdpa set mpls type class.
 */
public class Ofdpa3SetMplsTypeCodec extends JsonCodec<Ofdpa3SetMplsType>  {

    private static final String MPLS_TYPE = "mplsType";

    private static final String MISSING_MEMBER_MESSAGE = " member is required in Ofdpa3SetMplsType";
    private static final String MISSING_MPLS_TYPE_MESSAGE = "mplsType cannot be null";

    @Override
    public ObjectNode encode(Ofdpa3SetMplsType mplsType, CodecContext context) {
        checkNotNull(mplsType, MISSING_MPLS_TYPE_MESSAGE);
        return context.mapper().createObjectNode()
                .put(MPLS_TYPE, mplsType.mplsType());
    }

    @Override
    public Ofdpa3SetMplsType decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse ofdpa mpls type
        short mplsType = (short) nullIsIllegal(json.get(MPLS_TYPE),
                MPLS_TYPE + MISSING_MEMBER_MESSAGE).asInt();
        return new Ofdpa3SetMplsType(mplsType);
    }
}
