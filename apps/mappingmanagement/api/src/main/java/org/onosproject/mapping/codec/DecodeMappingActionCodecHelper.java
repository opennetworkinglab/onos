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
import org.onosproject.mapping.actions.MappingAction;
import org.onosproject.mapping.actions.MappingActions;

import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Decode portion of the mapping action codec.
 */
public final class DecodeMappingActionCodecHelper {
    private final ObjectNode json;

    /**
     * Creates a decode mapping action codec object.
     *
     * @param json    JSON object to decode
     */
    public DecodeMappingActionCodecHelper(ObjectNode json) {
        this.json = json;
    }

    /**
     * Decodes a no mapping action.
     *
     * @return mapping action object decoded from the JSON
     */
    private MappingAction decodeNoAction() {
        return MappingActions.noAction();
    }

    /**
     * Decodes a forward mapping action.
     *
     * @return mapping action object decoded from the JSON
     */
    private MappingAction decodeForwardAction() {
        return MappingActions.forward();
    }

    /**
     * Decodes a native forward mapping action.
     *
     * @return mapping action object decoded from the JSON
     */
    private MappingAction decodeNativeForwardAction() {
        return MappingActions.nativeForward();
    }

    /**
     * Decodes a drop mapping action.
     *
     * @return mapping action object decoded from the JSON
     */
    private MappingAction decodeDropAction() {
        return MappingActions.drop();
    }

    /**
     * Decodes the JSON into a mapping action object.
     *
     * @return MappingAction object
     * @throws IllegalArgumentException if the JSON is invalid
     */
    public MappingAction decode() {
       String type =  nullIsIllegal(json.get(MappingActionCodec.TYPE),
               MappingActionCodec.TYPE + MappingActionCodec.ERROR_MESSAGE).asText();

       if (type.equals(MappingAction.Type.DROP.name())) {
           return decodeDropAction();
       } else if (type.equals(MappingAction.Type.FORWARD.name())) {
           return decodeForwardAction();
       } else if (type.equals(MappingAction.Type.NATIVE_FORWARD.name())) {
           return decodeNativeForwardAction();
       } else if (type.equals(MappingAction.Type.NO_ACTION.name())) {
           return decodeNoAction();
       }
       throw new IllegalArgumentException("MappingAction type "
                + type + " is not supported");
    }
}
