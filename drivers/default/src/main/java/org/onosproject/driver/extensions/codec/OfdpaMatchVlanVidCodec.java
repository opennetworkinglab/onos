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
import org.onosproject.driver.extensions.OfdpaMatchVlanVid;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * JSON Codec for Ofdpa match vlan vid class.
 */
public class OfdpaMatchVlanVidCodec extends JsonCodec<OfdpaMatchVlanVid> {

    private static final String VLAN_ID = "vlanId";
    private static final String MISSING_MEMBER_MESSAGE = " member is required in OfdpaMatchVlanVid";
    private static final String MISSING_VLAN_ID_MESSAGE = "Vlan ID cannot be null";

    @Override
    public ObjectNode encode(OfdpaMatchVlanVid vlanId, CodecContext context) {
        checkNotNull(vlanId, MISSING_VLAN_ID_MESSAGE);
        return context.mapper().createObjectNode()
                .put(VLAN_ID, vlanId.vlanId().id());
    }

    @Override
    public OfdpaMatchVlanVid decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse ofdpa match vlan vid
        short vlanVid = (short) nullIsIllegal(json.get(VLAN_ID),
                VLAN_ID + MISSING_MEMBER_MESSAGE).asInt();
        return new OfdpaMatchVlanVid(VlanId.vlanId(vlanVid));
    }
}
