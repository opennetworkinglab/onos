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
import org.onlab.packet.VlanId;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.driver.extensions.Ofdpa3SetOvid;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * JSON Codec for Ofdpa set ovid class.
 */
public class Ofdpa3SetOvidCodec extends JsonCodec<Ofdpa3SetOvid> {

    private static final String OVID = "oVid";
    private static final String MISSING_MEMBER_MESSAGE = " member is required in Ofdpa3SetOvid";
    private static final String MISSING_OVID_MESSAGE = "OVID cannot be null";

    @Override
    public ObjectNode encode(Ofdpa3SetOvid setOvid, CodecContext context) {
        checkNotNull(setOvid, MISSING_OVID_MESSAGE);
        return context.mapper().createObjectNode()
                .put(OVID, setOvid.vlanId().id());
    }

    @Override
    public Ofdpa3SetOvid decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse ofdpa set ovid
        short vlanVid = (short) nullIsIllegal(json.get(OVID),
                OVID + MISSING_MEMBER_MESSAGE).asInt();
        return new Ofdpa3SetOvid(VlanId.vlanId(vlanVid));
    }
}
