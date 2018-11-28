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
import org.onosproject.openstacknode.api.DefaultOpenstackSshAuth;
import org.onosproject.openstacknode.api.OpenstackSshAuth;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Node ssh authentication info codec used for serializing and
 * de-serializing JSON string.
 */
public class OpenstackSshAuthCodec extends JsonCodec<OpenstackSshAuth> {

    private static final String ID = "id";
    private static final String PASSWORD = "password";

    private static final String MISSING_MESSAGE = " is required in OpenstackSshAuth";
    private static final String ERROR_MSG_NOT_NULL = "Openstack SSH auth cannot be null";

    @Override
    public ObjectNode encode(OpenstackSshAuth sshAuth, CodecContext context) {
        checkNotNull(sshAuth, ERROR_MSG_NOT_NULL);

        return context.mapper().createObjectNode()
                .put(ID, sshAuth.id())
                .put(PASSWORD, sshAuth.password());
    }

    @Override
    public OpenstackSshAuth decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        String id = nullIsIllegal(json.get(ID).asText(),
                ID + MISSING_MESSAGE);

        String password = nullIsIllegal(json.get(PASSWORD).asText(),
                PASSWORD + MISSING_MESSAGE);

        return DefaultOpenstackSshAuth.builder()
                .id(id)
                .password(password)
                .build();
    }
}
