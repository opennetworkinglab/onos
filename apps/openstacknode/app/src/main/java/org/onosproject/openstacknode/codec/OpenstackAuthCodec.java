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
import org.onosproject.openstacknode.api.DefaultOpenstackAuth;
import org.onosproject.openstacknode.api.OpenstackAuth;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Openstack keystone authentication codec used for serializing and
 * de-serializing JSON string.
 */
public class OpenstackAuthCodec extends JsonCodec<OpenstackAuth> {

    private static final String VERSION = "version";
    private static final String PROTOCOL = "protocol";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String PROJECT = "project";
    private static final String PERSPECTIVE = "perspective";

    private static final String MISSING_MESSAGE = " is required in OpenstackAuth";

    @Override
    public ObjectNode encode(OpenstackAuth auth, CodecContext context) {
        checkNotNull(auth, "Openstack auth cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(VERSION, auth.version())
                .put(PROTOCOL, auth.protocol().name())
                .put(USERNAME, auth.username())
                .put(PASSWORD, auth.password())
                .put(PROJECT, auth.project());

        if (auth.perspective() != null) {
            result.put(PERSPECTIVE, auth.perspective().name());
        }

        return result;
    }

    @Override
    public OpenstackAuth decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String version = nullIsIllegal(json.get(VERSION).asText(),
                VERSION + MISSING_MESSAGE);
        String protocol = nullIsIllegal(json.get(PROTOCOL).asText(),
                PROTOCOL + MISSING_MESSAGE);
        String username = nullIsIllegal(json.get(USERNAME).asText(),
                USERNAME + MISSING_MESSAGE);
        String password = nullIsIllegal(json.get(PASSWORD).asText(),
                PASSWORD + MISSING_MESSAGE);
        String project = nullIsIllegal(json.get(PROJECT).asText(),
                PROJECT + MISSING_MESSAGE);

        DefaultOpenstackAuth.Builder authBuilder = DefaultOpenstackAuth.builder()
                .version(version)
                .protocol(OpenstackAuth.Protocol.valueOf(protocol))
                .username(username)
                .password(password)
                .project(project);

        if (json.get(PERSPECTIVE) != null) {
            authBuilder.perspective(
                    OpenstackAuth.Perspective.valueOf(json.get(PERSPECTIVE).asText()));
        }

        return authBuilder.build();
    }
}
