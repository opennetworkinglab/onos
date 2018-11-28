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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstacknode.api.DefaultDpdkConfig;
import org.onosproject.openstacknode.api.DpdkConfig;
import org.onosproject.openstacknode.api.DpdkInterface;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.IntStream;

import static org.onlab.util.Tools.nullIsIllegal;

/**
 * dpdk config codec used for serializing and de-serializing JSON string.
 */
public class DpdkConfigCodec extends JsonCodec<DpdkConfig> {
    private static final String DATA_PATH_TYPE = "datapathType";
    private static final String SOCKET_DIR = "socketDir";
    private static final String DPDK_INTFS = "dpdkIntfs";

    private static final String MISSING_MESSAGE = " is required in DpdkInterface";

    @Override
    public ObjectNode encode(DpdkConfig entity, CodecContext context) {
        ObjectNode result = context.mapper().createObjectNode()
                .put(DATA_PATH_TYPE, entity.datapathType().name());

        if (entity.socketDir() != null) {
            result.put(SOCKET_DIR, entity.socketDir());
        }

        ArrayNode dpdkIntfs = context.mapper().createArrayNode();
        entity.dpdkIntfs().forEach(dpdkIntf -> {
            ObjectNode dpdkIntfJson = context.codec(DpdkInterface.class)
                    .encode(dpdkIntf, context);
            dpdkIntfs.add(dpdkIntfJson);
        });
        result.set(DPDK_INTFS, dpdkIntfs);

        return result;
    }

    @Override
    public DpdkConfig decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String datapathType = nullIsIllegal(json.get(DATA_PATH_TYPE).asText(),
                DATA_PATH_TYPE + MISSING_MESSAGE);

        DefaultDpdkConfig.Builder builder = DefaultDpdkConfig.builder()
                .datapathType(DpdkConfig.DatapathType.valueOf(datapathType.toUpperCase(Locale.ENGLISH)));

        if (json.get(SOCKET_DIR) != null) {
            builder.socketDir(json.get(SOCKET_DIR).asText());
        }

        List<DpdkInterface> dpdkInterfaces = new ArrayList<>();
        JsonNode dpdkIntfsJson = json.get(DPDK_INTFS);

        if (dpdkIntfsJson != null) {
            final JsonCodec<DpdkInterface>
                    dpdkIntfCodec = context.codec(DpdkInterface.class);

            IntStream.range(0, dpdkIntfsJson.size()).forEach(i -> {
                ObjectNode intfJson = get(dpdkIntfsJson, i);
                dpdkInterfaces.add(dpdkIntfCodec.decode(intfJson, context));
            });
        }
        builder.dpdkIntfs(dpdkInterfaces);

        return builder.build();
    }
}
