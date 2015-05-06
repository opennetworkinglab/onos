/*
 * Copyright 2014 Open Networking Laboratory
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

import com.fasterxml.jackson.databind.ObjectMapper;

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
     * @return JSON codec; null if no codec available for the class
     */
    <T> T getService(Class<T> serviceClass);

}
