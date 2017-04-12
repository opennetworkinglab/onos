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
package org.onosproject.mapping.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.mapping.actions.MappingAction;
import org.onosproject.mapping.actions.NoMappingAction;
import org.onosproject.mapping.actions.DropMappingAction;
import org.onosproject.mapping.actions.ForwardMappingAction;
import org.onosproject.mapping.actions.NativeForwardMappingAction;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Encode portion of the mapping action codec.
 */
public final class EncodeMappingActionCodecHelper {
    protected static final Logger log = getLogger(EncodeMappingActionCodecHelper.class);
    private final MappingAction action;
    private final CodecContext context;

    /**
     * Creates a mapping action object encoder.
     *
     * @param action  mapping action to encode
     * @param context codec context for the encoding
     */
    public EncodeMappingActionCodecHelper(MappingAction action,
                                          CodecContext context) {
        this.action = action;
        this.context = context;
    }

    /**
     * Encodes a no mapping action.
     *
     * @param result json node that the mapping action attributes are added to
     */
    private void encodeNoMappingAction(ObjectNode result) {
        NoMappingAction noMappingAction = (NoMappingAction) action;
        result.put(MappingActionCodec.TYPE, noMappingAction.type().name());
    }

    /**
     * Encodes a drop mapping action.
     *
     * @param result json node that the mapping action attributes are added to
     */
    private void encodeDropMappingAction(ObjectNode result) {
        DropMappingAction dropMappingAction = (DropMappingAction) action;
        result.put(MappingActionCodec.TYPE, dropMappingAction.type().name());
    }

    /**
     * Encodes a forward mapping action.
     *
     * @param result json node that the mapping action attributes are added to
     */
    private void encodeForwardMappingAction(ObjectNode result) {
        ForwardMappingAction forwardMappingAction = (ForwardMappingAction) action;
        result.put(MappingActionCodec.TYPE, forwardMappingAction.type().name());
    }

    /**
     * Encodes a native forward mapping action.
     *
     * @param result json node that the mapping action attributes are added to
     */
    private void encodeNativeForwardMappingAction(ObjectNode result) {
        NativeForwardMappingAction nativeMappingAction = (NativeForwardMappingAction) action;
        result.put(MappingActionCodec.TYPE, nativeMappingAction.type().name());
    }

    /**
     * Encodes the given mapping instruction into JSON.
     *
     * @return JSON object node representing the mapping action
     */
    public ObjectNode encode() {
       final ObjectNode result = context.mapper().createObjectNode()
               .put(MappingActionCodec.TYPE, action.type().toString());

       switch (action.type()) {
           case DROP:
               encodeDropMappingAction(result);
               break;
           case FORWARD:
               encodeForwardMappingAction(result);
               break;
           case NATIVE_FORWARD:
               encodeNativeForwardMappingAction(result);
               break;
           case NO_ACTION:
               encodeNoMappingAction(result);
               break;
           default:
               log.info("Cannot convert mapping action type of {}", action.type());
               break;
       }
       return result;
    }
}
