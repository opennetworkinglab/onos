/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.drivers.lisp.extensions.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.drivers.lisp.extensions.LispMulticastAddress;
import org.onosproject.mapping.addresses.MappingAddress;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * LISP multicast address codec.
 */
public final class LispMulticastAddressCodec extends JsonCodec<LispMulticastAddress> {

    protected static final String INSTANCE_ID = "instanceId";
    protected static final String SRC_MASK_LENGTH = "srcMaskLength";
    protected static final String GRP_MASK_LENGTH = "grpMaskLength";
    protected static final String SRC_ADDRESS = "srcAddress";
    protected static final String GRP_ADDRESS = "grpAddress";

    private static final String MISSING_MEMBER_MESSAGE =
                                " member is required in LispMulticastAddress";

    @Override
    public ObjectNode encode(LispMulticastAddress address, CodecContext context) {
        checkNotNull(address, "LispMulticastAddress cannot be null");

        final ObjectNode result = context.mapper().createObjectNode()
                .put(INSTANCE_ID, address.getInstanceId())
                .put(SRC_MASK_LENGTH, address.getSrcMaskLength())
                .put(GRP_MASK_LENGTH, address.getGrpMaskLength());

        final JsonCodec<MappingAddress> addressCodec =
                context.codec(MappingAddress.class);

        if (address.getSrcAddress() != null) {
            ObjectNode srcAddressNode = addressCodec.encode(address.getSrcAddress(), context);
            result.set(SRC_ADDRESS, srcAddressNode);
        }

        if (address.getGrpAddress() != null) {
            ObjectNode grpAddressNode = addressCodec.encode(address.getGrpAddress(), context);
            result.set(GRP_ADDRESS, grpAddressNode);
        }

        return result;
    }

    @Override
    public LispMulticastAddress decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        int instanceId = nullIsIllegal(json.get(INSTANCE_ID),
                INSTANCE_ID + MISSING_MEMBER_MESSAGE).asInt();
        byte srcMaskLength = (byte) nullIsIllegal(json.get(SRC_MASK_LENGTH),
                SRC_MASK_LENGTH + MISSING_MEMBER_MESSAGE).asInt();
        byte grpMaskLength = (byte) nullIsIllegal(json.get(GRP_MASK_LENGTH),
                GRP_MASK_LENGTH + MISSING_MEMBER_MESSAGE).asInt();

        final JsonCodec<MappingAddress> addressCodec =
                context.codec(MappingAddress.class);
        ObjectNode srcAddressJson = get(json, SRC_ADDRESS);
        MappingAddress srcAddress = null;

        if (srcAddressJson != null) {
            srcAddress = addressCodec.decode(srcAddressJson, context);
        }

        ObjectNode grpAddressJson = get(json, GRP_ADDRESS);
        MappingAddress grpAddress = null;

        if (grpAddressJson != null) {
            grpAddress = addressCodec.decode(grpAddressJson, context);
        }

        return new LispMulticastAddress.Builder()
                        .withInstanceId(instanceId)
                        .withSrcMaskLength(srcMaskLength)
                        .withGrpMaskLength(grpMaskLength)
                        .withSrcAddress(srcAddress)
                        .withGrpAddress(grpAddress)
                        .build();
    }
}
