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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstacknode.api.NeutronConfig;
import org.onosproject.openstacknode.api.DefaultNeutronConfig;

import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Neutron config codec used for serializing and de-serializing JSON string.
 */
public final class NeutronConfigCodec extends JsonCodec<NeutronConfig> {

    private static final String USE_METADATA_PROXY = "useMetadataProxy";
    private static final String METADATA_PROXY_SECRET = "metadataProxySecret";

    private static final String MISSING_MESSAGE = " is required in OpenstackNode";

    @Override
    public ObjectNode encode(NeutronConfig entity, CodecContext context) {
        return context.mapper().createObjectNode()
                .put(USE_METADATA_PROXY, entity.useMetadataProxy())
                .put(METADATA_PROXY_SECRET, entity.metadataProxySecret());
    }

    @Override
    public NeutronConfig decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        boolean useMetadataProxy = nullIsIllegal(json.get(USE_METADATA_PROXY).asBoolean(),
                USE_METADATA_PROXY + MISSING_MESSAGE);

        String metadataProxySecret = nullIsIllegal(json.get(METADATA_PROXY_SECRET).asText(),
                METADATA_PROXY_SECRET + MISSING_MESSAGE);

        return DefaultNeutronConfig.builder()
                .useMetadataProxy(useMetadataProxy)
                .metadataProxySecret(metadataProxySecret)
                .build();
    }
}
