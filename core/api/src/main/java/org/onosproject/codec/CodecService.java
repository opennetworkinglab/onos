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

import java.util.Set;

/**
 * Service for registering and retrieving JSON codecs for various entities.
 */
public interface CodecService {

    /**
     * Returns the set of classes with currently registered codecs.
     *
     * @return set of entity classes
     */
    Set<Class<?>> getCodecs();

    /**
     * Returns the JSON codec for the specified entity class.
     *
     * @param entityClass entity class
     * @param <T>         entity type
     * @return JSON codec; null if no codec available for the class
     */
    <T> JsonCodec<T> getCodec(Class<T> entityClass);

    /**
     * Registers the specified JSON codec for the given entity class.
     *
     * @param entityClass entity class
     * @param codec       JSON codec
     * @param <T>         entity type
     */
    <T> void registerCodec(Class<T> entityClass, JsonCodec<T> codec);

    /**
     * Unregisters the JSON codec for the specified entity class.
     *
     * @param entityClass entity class
     */
    void unregisterCodec(Class<?> entityClass);

}
