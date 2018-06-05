/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstacktelemetry.api;

import java.nio.ByteBuffer;

/**
 * Abstraction of a codec capable for encoding/decoding arbitrary objects to/from ByteBuffer.
 */
public abstract class ByteBufferCodec<T> {

    /**
     * Encodes the specified entity into ByteBuffer.
     *
     * @param entity entity to encode
     * @return ByteBuffer
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                  support encode operations
     */
    public ByteBuffer encode(T entity) {
        throw new UnsupportedOperationException("encode() not supported");
    }

    /**
     * Decodes the specified entity from ByteBuffer.
     *
     * @param buffer ByteBuffer to decode
     * @return decoded entity
     */
    public T decode(ByteBuffer buffer) {
        throw new UnsupportedOperationException("decode() not supported");
    }
}
