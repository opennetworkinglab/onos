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

import java.util.Iterator;
import java.util.Map.Entry;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.MepEntry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Encode and decode to/from JSON to MepEntry object.
 */
public class MepEntryCodec extends JsonCodec<MepEntry> {

    /**
     * Encodes the MepEntry entity into JSON.
     *
     * @param mepEntry  MepEntry to encode
     * @param context encoding context
     * @return JSON node
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    @Override
    public ObjectNode encode(MepEntry mepEntry, CodecContext context) {
        checkNotNull(mepEntry, "Mep cannot be null");

        ObjectNode result = context.mapper().createObjectNode();

        //Get the common attributes
        Mep mep = mepEntry;
        ObjectNode mepAttrs = new MepCodec().encode(mep, context);
        Iterator<Entry<String, JsonNode>> elements = mepAttrs.fields();
        while (elements.hasNext()) {
            Entry<String, JsonNode> element = elements.next();
            result.set(element.getKey(), element.getValue());
        }

        if (mepEntry.macAddress() != null) {
            result.put("macAddress", mepEntry.macAddress().toString());
        }

        if (mepEntry.loopbackAttributes() != null) {
            result.set("loopback", new MepLbEntryCodec()
                    .encode(mepEntry.loopbackAttributes(), context));
        }

        if (mepEntry.activeRemoteMepList() != null) {
            result.set("remoteMeps", new RemoteMepEntryCodec()
                    .encode(mepEntry.activeRemoteMepList(), context));
        }

        if (mepEntry.activeErrorCcmDefect()) {
            result.put("activeErrorCcmDefect", true);
        }
        if (mepEntry.activeMacStatusDefect()) {
            result.put("activeMacStatusDefect", true);
        }
        if (mepEntry.activeRdiCcmDefect()) {
            result.put("activeRdiCcmDefect", true);
        }
        if (mepEntry.activeRemoteCcmDefect()) {
            result.put("activeRemoteCcmDefect", true);
        }
        if (mepEntry.activeXconCcmDefect()) {
            result.put("activeXconCcmDefect", true);
        }
        return result;
    }

    /**
     * Encodes the collection of the MepEntry entities.
     *
     * @param mepEntryEntities collection of MepEntry to encode
     * @param context  encoding context
     * @return JSON array
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    @Override
    public ArrayNode encode(Iterable<MepEntry> mepEntryEntities, CodecContext context) {
        ArrayNode an = context.mapper().createArrayNode();
        if (mepEntryEntities != null) {
            mepEntryEntities.forEach(mepEntry -> an.add(encode(mepEntry, context)));
        }
        return an;
    }

}
