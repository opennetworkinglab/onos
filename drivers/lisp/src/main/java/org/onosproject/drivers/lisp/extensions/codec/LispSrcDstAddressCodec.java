/*
 * Copyright 2017-present Open Networking Laboratory
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
import org.onosproject.drivers.lisp.extensions.LispSrcDstAddress;
import org.onosproject.mapping.addresses.MappingAddress;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * LISP source destination address codec.
 */
public final class LispSrcDstAddressCodec extends JsonCodec<LispSrcDstAddress> {

    protected static final String SRC_MASK_LENGTH = "srcMaskLength";
    protected static final String DST_MASK_LENGTH = "dstMaskLength";
    protected static final String SRC_PREFIX = "srcPrefix";
    protected static final String DST_PREFIX = "dstPrefix";

    private static final String MISSING_MEMBER_MESSAGE =
                                " member is required in LispSrcDstAddress";

    @Override
    public ObjectNode encode(LispSrcDstAddress address, CodecContext context) {
        checkNotNull(address, "LispSrcDstAddress cannot be null");

        final JsonCodec<MappingAddress> addressCodec =
                context.codec(MappingAddress.class);

        final ObjectNode result = context.mapper().createObjectNode()
                .put(SRC_MASK_LENGTH, address.getSrcMaskLength())
                .put(DST_MASK_LENGTH, address.getDstMaskLength());

        if (address.getSrcPrefix() != null) {
            ObjectNode srcPrefix = addressCodec.encode(address.getSrcPrefix(), context);
            result.set(SRC_PREFIX, srcPrefix);
        }

        if (address.getDstPrefix() != null) {
            ObjectNode dstPrefix = addressCodec.encode(address.getDstPrefix(), context);
            result.set(DST_PREFIX, dstPrefix);
        }

        return result;
    }

    @Override
    public LispSrcDstAddress decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        byte srcMaskLength = (byte) nullIsIllegal(json.get(SRC_MASK_LENGTH),
                SRC_MASK_LENGTH + MISSING_MEMBER_MESSAGE).asInt();
        byte dstMaskLength = (byte) nullIsIllegal(json.get(DST_MASK_LENGTH),
                DST_MASK_LENGTH + MISSING_MEMBER_MESSAGE).asInt();

        final JsonCodec<MappingAddress> addressCodec =
                context.codec(MappingAddress.class);

        ObjectNode srcPrefixJson = get(json, SRC_PREFIX);
        MappingAddress srcPrefix = null;

        ObjectNode dstPrefixJson = get(json, DST_PREFIX);
        MappingAddress dstPrefix = null;

        if (srcPrefixJson != null) {
            srcPrefix = addressCodec.decode(srcPrefixJson, context);
        }

        if (dstPrefixJson != null) {
            dstPrefix = addressCodec.decode(dstPrefixJson, context);
        }

        return new LispSrcDstAddress.Builder()
                            .withSrcMaskLength(srcMaskLength)
                            .withDstMaskLength(dstMaskLength)
                            .withSrcPrefix(srcPrefix)
                            .withDstPrefix(dstPrefix)
                            .build();
    }
}
