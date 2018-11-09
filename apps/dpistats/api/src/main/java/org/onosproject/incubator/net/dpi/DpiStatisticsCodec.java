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

package org.onosproject.incubator.net.dpi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of encoder for DpiStatistics codec.
 */
public final class DpiStatisticsCodec extends JsonCodec<DpiStatistics> {

    private static final String RECEIVED_TIME = "receivedTime";
    private static final String DPI_STATISTICS = "dpiStatistics";

    private final Logger log = getLogger(getClass());

    @Override
    public ObjectNode encode(DpiStatistics ds, CodecContext context) {
        checkNotNull(ds, "DpiStatistics cannot be null");

        final ObjectNode result = context.mapper().createObjectNode();

        result.put(RECEIVED_TIME, ds.receivedTime());

        final JsonCodec<DpiStatInfo> dpiStatInfoCodec =
                context.codec(DpiStatInfo.class);

        final ObjectNode jsonDpiStatInfo = dpiStatInfoCodec.encode(ds.dpiStatInfo(), context);
        result.set(DPI_STATISTICS, jsonDpiStatInfo);

        return result;
    }

    @Override
    public DpiStatistics decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        log.debug("receivedTime={}, full json={} ", json.get("receivedTime"), json);
        JsonNode receivedTimeJson = json.get(RECEIVED_TIME);
        String receivedTime = receivedTimeJson == null ? "" : receivedTimeJson.asText();

        final JsonCodec<DpiStatInfo> dpiStatInfoCodec =
                context.codec(DpiStatInfo.class);

        DpiStatInfo dpiStatInfo =
                dpiStatInfoCodec.decode(get(json, DPI_STATISTICS), context);

        return new DpiStatistics(receivedTime, dpiStatInfo);
    }
}
