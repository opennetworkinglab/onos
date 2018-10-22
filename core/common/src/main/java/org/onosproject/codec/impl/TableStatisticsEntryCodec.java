/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.codec.impl;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.flow.TableStatisticsEntry;

import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Table statistics entry JSON codec.
 */
public final class TableStatisticsEntryCodec extends JsonCodec<TableStatisticsEntry> {

    @Override
    public ObjectNode encode(TableStatisticsEntry entry, CodecContext context) {
        checkNotNull(entry, "Table Statistics entry cannot be null");

        final ObjectNode result = context.mapper().createObjectNode()
                .put("tableId", entry.table().toString())
                .put("deviceId", entry.deviceId().toString())
                .put("activeEntries", entry.activeFlowEntries())
                .put("packetsLookedUp", entry.packetsLookedup())
                .put("packetsMatched", entry.packetsMatched());

        return result;
    }

}
