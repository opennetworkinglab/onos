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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.MastershipRole;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;
import static org.onosproject.net.MastershipRole.MASTER;
import static org.onosproject.net.MastershipRole.NONE;
import static org.onosproject.net.MastershipRole.STANDBY;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Codec for mastership role.
 */
public final class MastershipRoleCodec extends JsonCodec<MastershipRole> {
    private final Logger log = getLogger(getClass());

    // JSON field names
    private static final String ROLE = "role";

    private static final String MISSING_MEMBER_MESSAGE = " member is required in MastershipRole";

    @Override
    public ObjectNode encode(MastershipRole mastershipRole, CodecContext context) {
        checkNotNull(mastershipRole, "MastershipRole cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put(ROLE, mastershipRole.name());
        return result;
    }

    @Override
    public MastershipRole decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String roleJson = nullIsIllegal(json.get(ROLE),
                ROLE + MISSING_MEMBER_MESSAGE).asText();
        MastershipRole mastershipRole;
        switch (roleJson) {
            case "MASTER":
                mastershipRole = MASTER;
                break;
            case "STANDBY":
                mastershipRole = STANDBY;
                break;
            case "NONE":
                mastershipRole = NONE;
                break;
            default:
                log.warn("The mastership role {} is not defined.", roleJson);
                return null;
        }

        return mastershipRole;
    }
}
