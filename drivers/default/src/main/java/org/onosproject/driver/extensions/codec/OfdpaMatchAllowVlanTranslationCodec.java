/*
 * Copyright 2017-present Open Networking Foundation
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
import org.onosproject.driver.extensions.OfdpaMatchAllowVlanTranslation;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * JSON Codec for Ofdpa match allow vlan translation class.
 */
public class OfdpaMatchAllowVlanTranslationCodec extends JsonCodec<OfdpaMatchAllowVlanTranslation> {

    private static final String ALLOW_VLAN_TRANSLATION = "allowVlanTranslation";
    private static final String MISSING_MEMBER_MESSAGE = " member is required in OfdpaMatchAllowVlanTranslation";
    private static final String MISSING_ALLOW_VLAN_TRANSLATION_MESSAGE = "Allow Vlan Translation cannot be null";

    @Override
    public ObjectNode encode(OfdpaMatchAllowVlanTranslation allowVlanTranslation, CodecContext context) {
        checkNotNull(allowVlanTranslation, MISSING_ALLOW_VLAN_TRANSLATION_MESSAGE);
        return context.mapper().createObjectNode().put(ALLOW_VLAN_TRANSLATION,
                                                       allowVlanTranslation.allowVlanTranslation());
    }

    @Override
    public OfdpaMatchAllowVlanTranslation decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse ofdpa match allow vlan translation
        short allowVlanTranslation = (short) nullIsIllegal(json.get(ALLOW_VLAN_TRANSLATION),
                                                           ALLOW_VLAN_TRANSLATION + MISSING_MEMBER_MESSAGE).asLong();
        return new OfdpaMatchAllowVlanTranslation(allowVlanTranslation);
    }
}
