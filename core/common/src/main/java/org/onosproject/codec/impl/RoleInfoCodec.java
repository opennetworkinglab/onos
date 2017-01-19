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
package org.onosproject.codec.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Codec for role info.
 */
public final class RoleInfoCodec extends JsonCodec<RoleInfo> {

    // JSON field names
    private static final String MASTER = "master";
    private static final String BACKUPS = "backups";

    private static final String MISSING_MEMBER_MESSAGE = " member is required in RoleInfo";

    @Override
    public ObjectNode encode(RoleInfo roleInfo, CodecContext context) {
        checkNotNull(roleInfo, "RoleInfo cannot be null");

        ObjectNode result = context.mapper().createObjectNode();

        if (roleInfo.master() != null) {
            result.put(MASTER, roleInfo.master().id());
        }

        ArrayNode backups = context.mapper().createArrayNode();
        roleInfo.backups().forEach(backup -> backups.add(backup.id()));

        if (!roleInfo.backups().isEmpty()) {
            result.set(BACKUPS, backups);
        }

        return result;
    }

    @Override
    public RoleInfo decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse node identifier of master
        NodeId nodeId = json.get(MASTER) == null ?
                null : NodeId.nodeId(json.get(MASTER).asText());

        // parse node identifier of backups
        List<NodeId> backups = new ArrayList<>();

        ArrayNode backupsJson = (ArrayNode) nullIsIllegal(json.get(BACKUPS),
                BACKUPS + MISSING_MEMBER_MESSAGE);

        IntStream.range(0, backupsJson.size()).forEach(i -> {
            JsonNode backupJson = nullIsIllegal(backupsJson.get(i),
                    "Backup node id cannot be null");
            backups.add(NodeId.nodeId(backupJson.asText()));
        });

        return new RoleInfo(nodeId, backups);
    }
}
