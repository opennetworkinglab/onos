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
import org.onosproject.driver.extensions.OplinkAttenuation;

import static org.onlab.util.Tools.nullIsIllegal;

/**
 * JSON Codec for OplinkAttenuation class.
 */
public class OplinkAttenuationCodec extends JsonCodec<OplinkAttenuation> {
    private static final String ATTENUATION = "attenuation";
    private static final String MISSING_ATT = "Missing value for \"attenuation\"";

    @Override
    public OplinkAttenuation decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }
        String att = nullIsIllegal(json.get(ATTENUATION), MISSING_ATT).asText();
        return new OplinkAttenuation(Integer.valueOf(att));
    }
}
