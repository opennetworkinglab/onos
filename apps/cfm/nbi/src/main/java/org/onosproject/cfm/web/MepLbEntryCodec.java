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
package org.onosproject.cfm.web;

import static com.google.common.base.Preconditions.checkNotNull;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLbEntry;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Encode and decode to/from JSON to MepLbEntry object.
 */
public class MepLbEntryCodec extends JsonCodec<MepLbEntry> {

    /**
     * Encodes the MepLbEntry entity into JSON.
     *
     * @param mepLbEntry MepLbEntry to encode
     * @param context encoding context
     * @return JSON node
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    @Override
    public ObjectNode encode(MepLbEntry mepLbEntry, CodecContext context) {
        checkNotNull(mepLbEntry, "Mep Lb Entry cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put("nextLbmIdentifier", mepLbEntry.nextLbmIdentifier())
                .put("countLbrTransmitted", mepLbEntry.countLbrTransmitted())
                .put("countLbrReceived", mepLbEntry.countLbrReceived())
                .put("countLbrValidInOrder", mepLbEntry.countLbrValidInOrder())
                .put("countLbrValidOutOfOrder", mepLbEntry.countLbrValidOutOfOrder())
                .put("countLbrMacMisMatch", mepLbEntry.countLbrMacMisMatch());

        return result;
    }
}
