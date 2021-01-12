/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.kubevirtnetworking.api.KubevirtIpPool;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Kubevirt IP pool codec used for serializing and de-serializing JSON string.
 */
public final class KubevirtIpPoolCodec extends JsonCodec<KubevirtIpPool> {

    private final Logger log = getLogger(getClass());

    private static final String START = "start";
    private static final String END = "end";

    private static final String MISSING_MESSAGE = " is required in KubevirtIpPool";

    @Override
    public ObjectNode encode(KubevirtIpPool ipPool, CodecContext context) {
        checkNotNull(ipPool, "Kubevirt IP pool cannot be null");

        return context.mapper().createObjectNode()
                .put(START, ipPool.start().toString())
                .put(END, ipPool.end().toString());
    }

    @Override
    public KubevirtIpPool decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String start = nullIsIllegal(json.get(START).asText(), START + MISSING_MESSAGE);
        String end = nullIsIllegal(json.get(END).asText(), END + MISSING_MESSAGE);

        return new KubevirtIpPool(IpAddress.valueOf(start), IpAddress.valueOf(end));
    }
}
