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
package org.onosproject.incubator.net.virtual.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.virtual.DefaultVirtualDevice;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualDevice;
import org.onosproject.net.DeviceId;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Codec for the VirtualDevice class.
 */
public class VirtualDeviceCodec extends JsonCodec<VirtualDevice> {

    // JSON field names
    private static final String ID = "deviceId";
    private static final String NETWORK_ID = "networkId";

    private static final String NULL_OBJECT_MSG = "VirtualDevice cannot be null";
    private static final String MISSING_MEMBER_MSG = " member is required in VirtualDevice";

    @Override
    public ObjectNode encode(VirtualDevice vDev, CodecContext context) {
        checkNotNull(vDev, NULL_OBJECT_MSG);

        ObjectNode result = context.mapper().createObjectNode()
                .put(NETWORK_ID, vDev.networkId().toString())
                .put(ID, vDev.id().toString());

        return result;
    }

    @Override
    public VirtualDevice decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        DeviceId dId = DeviceId.deviceId(extractMember(ID, json));
        NetworkId nId = NetworkId.networkId(Long.parseLong(extractMember(NETWORK_ID, json)));
        return new DefaultVirtualDevice(nId, dId);
    }

    /**
     * Extract member from JSON ObjectNode.
     *
     * @param key key for which value is needed
     * @param json JSON ObjectNode
     * @return member value
     */
    private String extractMember(String key, ObjectNode json) {
        return nullIsIllegal(json.get(key), key + MISSING_MEMBER_MSG).asText();
    }
}
