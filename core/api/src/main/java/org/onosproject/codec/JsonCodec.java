/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstraction of a codec capable for encoding/decoding arbitrary objects to/from JSON.
 */
public abstract class JsonCodec<T> {

    /**
     * Encodes the specified entity into JSON.
     *
     * @param entity  entity to encode
     * @param context encoding context
     * @return JSON node
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    public ObjectNode encode(T entity, CodecContext context) {
        throw new UnsupportedOperationException("encode() not supported");
    }

    /**
     * Decodes the specified entity from JSON.
     *
     * @param json    JSON to decode
     * @param context decoding context
     * @return decoded entity
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support decode operations
     */
    public T decode(ObjectNode json, CodecContext context) {
        throw new UnsupportedOperationException("decode() not supported");
    }

    /**
     * Encodes the collection of the specified entities.
     *
     * @param entities collection of entities to encode
     * @param context  encoding context
     * @return JSON array
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    public ArrayNode encode(Iterable<T> entities, CodecContext context) {
        ArrayNode result = context.mapper().createArrayNode();
        for (T entity : entities) {
            result.add(encode(entity, context));
        }
        return result;
    }

    /**
     * Decodes the specified JSON array into a collection of entities.
     *
     * @param json    JSON array to decode
     * @param context decoding context
     * @return collection of decoded entities
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support decode operations
     */
    public List<T> decode(ArrayNode json, CodecContext context) {
        List<T> result = new ArrayList<>();
        for (JsonNode node : json) {
            result.add(decode((ObjectNode) node, context));
        }
        return result;
    }

    /**
     * Gets a child Object Node from a parent by name. If the child is not found
     * or does nor represent an object, null is returned.
     *
     * @param parent parent object
     * @param childName name of child to query
     * @return child object if found, null if not found or if not an object
     */
    protected static ObjectNode get(ObjectNode parent, String childName) {
        JsonNode node = parent.path(childName);
        return node.isObject() && !node.isNull() ? (ObjectNode) node : null;
    }

    /**
     * Gets a child Object Node from a parent by index. If the child is not found
     * or does nor represent an object, null is returned.
     *
     * @param parent parent object
     * @param childIndex index of child to query
     * @return child object if found, null if not found or if not an object
     */
    protected static ObjectNode get(JsonNode parent, int childIndex) {
        JsonNode node = parent.path(childIndex);
        return node.isObject() && !node.isNull() ? (ObjectNode) node : null;
    }
}
