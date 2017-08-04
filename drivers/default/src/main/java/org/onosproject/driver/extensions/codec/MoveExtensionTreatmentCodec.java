/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.driver.extensions.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.driver.extensions.DefaultMoveExtensionTreatment;
import org.onosproject.driver.extensions.MoveExtensionTreatment;
import org.onosproject.net.flow.instructions.ExtensionTreatmentType;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * JSON Codec for MoveExtensionTreatment class.
 */
public final class MoveExtensionTreatmentCodec extends JsonCodec<MoveExtensionTreatment> {

    private static final String SRC_OFS = "srcOfs";
    private static final String DST_OFS = "dstOfs";
    private static final String N_BITS = "nBits";
    private static final String SRC = "src";
    private static final String DST = "dst";
    private static final String TYPE = "type";

    private static final String MISSING_MEMBER_MESSAGE = " member is required in MoveExtensionTreatment";

    @Override
    public ObjectNode encode(MoveExtensionTreatment moveExtensionTreatment, CodecContext context) {
        checkNotNull(moveExtensionTreatment, "Move Extension Treatment cannot be null");
        ObjectNode root = context.mapper().createObjectNode()
                .put(SRC_OFS, moveExtensionTreatment.srcOffset())
                .put(DST_OFS, moveExtensionTreatment.dstOffset())
                .put(N_BITS, moveExtensionTreatment.nBits())
                .put(SRC, moveExtensionTreatment.src())
                .put(DST, moveExtensionTreatment.dst());
        return root;
    }

    @Override
    public MoveExtensionTreatment decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse extension treatment type
        ExtensionTreatmentType type = new ExtensionTreatmentType(nullIsIllegal(json.get(TYPE),
                TYPE + MISSING_MEMBER_MESSAGE).asInt());

        // parse src off set
        int srcOfs = nullIsIllegal(json.get(SRC_OFS), SRC_OFS + MISSING_MEMBER_MESSAGE).asInt();

        // parse dst off set
        int dstOfs = nullIsIllegal(json.get(DST_OFS), DST_OFS + MISSING_MEMBER_MESSAGE).asInt();

        // parse n bits
        int nBits = nullIsIllegal(json.get(N_BITS), N_BITS + MISSING_MEMBER_MESSAGE).asInt();

        // parse src
        int src = nullIsIllegal(json.get(SRC), SRC + MISSING_MEMBER_MESSAGE).asInt();

        // parse dst
        int dst = nullIsIllegal(json.get(DST), DST + MISSING_MEMBER_MESSAGE).asInt();

        return new DefaultMoveExtensionTreatment(srcOfs, dstOfs, nBits, src, dst, type);
    }
}
