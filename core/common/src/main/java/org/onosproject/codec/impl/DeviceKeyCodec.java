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
import org.onosproject.net.key.DeviceKey;
import org.onosproject.net.key.DeviceKeyId;
import org.onosproject.net.key.DeviceKeyService;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Device key JSON codec.
 */
public class DeviceKeyCodec extends AnnotatedCodec<DeviceKey> {

    private final Logger log = getLogger(getClass());

    // JSON fieldNames
    private static final String ID = "id";
    private static final String TYPE = "type";
    private static final String LABEL = "label";
    private static final String COMMUNITY_NAME = "community_name";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";

    @Override
    public ObjectNode encode(DeviceKey deviceKey, CodecContext context) {
        checkNotNull(deviceKey, "Device key cannot be null");
        DeviceKeyService service = context.getService(DeviceKeyService.class);
        ObjectNode result = context.mapper().createObjectNode()
                .put(ID, deviceKey.deviceKeyId().id())
                .put(TYPE, deviceKey.type().toString())
                .put(LABEL, deviceKey.label());

        if (deviceKey.type().equals(DeviceKey.Type.COMMUNITY_NAME)) {
            result.put(COMMUNITY_NAME, deviceKey.asCommunityName().name());
        } else if (deviceKey.type().equals(DeviceKey.Type.USERNAME_PASSWORD)) {
            result.put(USERNAME, deviceKey.asUsernamePassword().username());
            result.put(PASSWORD, deviceKey.asUsernamePassword().password());
        }

        return annotate(result, deviceKey, context);
    }

    @Override
    public DeviceKey decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        DeviceKeyId id = DeviceKeyId.deviceKeyId(json.get(ID).asText());

        DeviceKey.Type type = DeviceKey.Type.valueOf(json.get(TYPE).asText());
        String label = extract(json, LABEL);

        if (type.equals(DeviceKey.Type.COMMUNITY_NAME)) {
            String communityName = extract(json, COMMUNITY_NAME);
            return DeviceKey.createDeviceKeyUsingCommunityName(id, label, communityName);
        } else if (type.equals(DeviceKey.Type.USERNAME_PASSWORD)) {
            String username = extract(json, USERNAME);
            String password = extract(json, PASSWORD);
            return DeviceKey.createDeviceKeyUsingUsernamePassword(id, label, username, password);
        } else {
            log.error("Unknown device key type: ", type);
            return null;
        }
    }

    /**
     * Extract the key from the json node.
     *
     * @param json json object
     * @param key key to use extract the value from the json object
     * @return extracted value from the json object
     */
    private String extract(ObjectNode json, String key) {
        return json.get(key) == null ? null : json.get(key).asText();
    }
}
