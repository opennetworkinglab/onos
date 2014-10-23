/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
     * @param entity entity to encode
     * @param mapper object mapper
     * @return JSON node
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    public abstract ObjectNode encode(T entity, ObjectMapper mapper);

    /**
     * Decodes the specified entity from JSON.
     *
     * @param json JSON to decode
     * @return decoded entity
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support decode operations
     */
    public abstract T decode(ObjectNode json);

    /**
     * Encodes the collection of the specified entities.
     *
     * @param entities collection of entities to encode
     * @param mapper   object mapper
     * @return JSON array
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    public ArrayNode encode(Iterable<T> entities, ObjectMapper mapper) {
        ArrayNode result = mapper.createArrayNode();
        for (T entity : entities) {
            result.add(encode(entity, mapper));
        }
        return result;
    }

    /**
     * Decodes the specified JSON array into a collection of entities.
     *
     * @param json JSON array to decode
     * @return collection of decoded entities
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support decode operations
     */
    public List<T> decode(ArrayNode json) {
        List<T> result = new ArrayList<>();
        for (JsonNode node : json) {
            result.add(decode((ObjectNode) node));
        }
        return result;
    }

}
