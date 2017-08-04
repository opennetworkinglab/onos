/*
 * Copyright 2014-present Open Networking Foundation
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

import static com.google.common.base.Preconditions.checkArgument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Context for codecs to use while encoding/decoding.
 */
public interface CodecContext {

    /**
     * Returns the JSON object mapper.
     *
     * @return object mapper
     */
    ObjectMapper mapper();

    /**
     * Returns the JSON codec for the specified entity class.
     *
     * @param entityClass entity class
     * @param <T>         entity type
     * @return JSON codec; null if no codec available for the class
     */
    <T> JsonCodec<T> codec(Class<T> entityClass);

    /**
     * Returns reference to the specified service implementation.
     *
     * @param serviceClass service class
     * @param <T>          service type
     * @return service implementation; null if no implementation available for the class
     */
    <T> T getService(Class<T> serviceClass);

    /**
     * Decodes the specified entity from JSON using codec
     * registered to this context.
     *
     * @param json    JSON to decode
     * @param entityClass entity class
     * @param <T> entity type
     * @return decoded entity
     */
    default <T> T decode(JsonNode json, Class<T> entityClass) {
        checkArgument(json.isObject());
        return codec(entityClass).decode((ObjectNode) json, this);
    }

    /**
     * Encodes the specified entity into JSON using codec
     * registered to this context.
     *
     * @param entity  entity to encode
     * @param entityClass entity class
     * @param <T> entity type
     * @return JSON node
     */
    default <T> ObjectNode encode(T entity, Class<T> entityClass) {
        return codec(entityClass).encode(entity, this);
    }

}
