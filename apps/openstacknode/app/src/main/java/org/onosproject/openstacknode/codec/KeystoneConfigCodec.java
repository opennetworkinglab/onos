/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.openstacknode.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstacknode.api.KeystoneConfig;
import org.onosproject.openstacknode.api.OpenstackAuth;
import org.onosproject.openstacknode.api.DefaultKeystoneConfig;

import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Keystone config codec used for serializing and de-serializing JSON string.
 */
public final class KeystoneConfigCodec extends JsonCodec<KeystoneConfig> {

    private static final String ENDPOINT = "endpoint";
    private static final String AUTHENTICATION = "authentication";

    private static final String MISSING_MESSAGE = " is required in OpenstackNode";

    @Override
    public ObjectNode encode(KeystoneConfig entity, CodecContext context) {
        ObjectNode result = context.mapper().createObjectNode()
                .put(ENDPOINT, entity.endpoint());

        ObjectNode authJson = context.codec(OpenstackAuth.class)
                .encode(entity.authentication(), context);
        result.set(AUTHENTICATION, authJson);

        return result;
    }

    @Override
    public KeystoneConfig decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String endpoint = nullIsIllegal(json.get(ENDPOINT).asText(),
                ENDPOINT + MISSING_MESSAGE);

        // parse authentication
        JsonNode authJson = nullIsIllegal(json.get(AUTHENTICATION),
                AUTHENTICATION + MISSING_MESSAGE);


        final JsonCodec<OpenstackAuth> authCodec = context.codec(OpenstackAuth.class);
        OpenstackAuth auth = authCodec.decode((ObjectNode) authJson.deepCopy(), context);

        return DefaultKeystoneConfig.builder()
                .endpoint(endpoint)
                .authentication(auth)
                .build();
    }
}
