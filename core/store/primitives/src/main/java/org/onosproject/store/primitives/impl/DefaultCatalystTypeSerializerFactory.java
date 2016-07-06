/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.store.primitives.impl;

import static org.slf4j.LoggerFactory.getLogger;

import org.onosproject.store.service.Serializer;
import org.slf4j.Logger;

import com.google.common.base.Throwables;

import io.atomix.catalyst.buffer.BufferInput;
import io.atomix.catalyst.buffer.BufferOutput;
import io.atomix.catalyst.serializer.TypeSerializer;
import io.atomix.catalyst.serializer.TypeSerializerFactory;

/**
 * {@link TypeSerializerFactory} for providing {@link TypeSerializer}s based on
 * {@code org.onosproject.store.service.Serializer}.
 */
public class DefaultCatalystTypeSerializerFactory implements TypeSerializerFactory {

    private final Logger log = getLogger(getClass());
    private final TypeSerializer<?> typeSerializer;

    public DefaultCatalystTypeSerializerFactory(Serializer serializer) {
        typeSerializer = new InternalSerializer<>(serializer);
    }

    @Override
    public TypeSerializer<?> createSerializer(Class<?> clazz) {
        return typeSerializer;
    }

    private class InternalSerializer<T> implements TypeSerializer<T> {

        private final Serializer serializer;

        InternalSerializer(Serializer serializer) {
            this.serializer = serializer;
        }

        @Override
        public void write(T object, BufferOutput buffer, io.atomix.catalyst.serializer.Serializer serializer) {
            try {
                byte[] payload = this.serializer.encode(object);
                buffer.writeInt(payload.length);
                buffer.write(payload);
            } catch (Exception e) {
                log.warn("Failed to serialize {}", object, e);
                throw Throwables.propagate(e);
            }
        }

        @Override
        public T read(Class<T> type, BufferInput buffer, io.atomix.catalyst.serializer.Serializer serializer) {
            int size = buffer.readInt();
            try {
                byte[] payload = new byte[size];
                buffer.read(payload);
                return this.serializer.decode(payload);
            } catch (Exception e) {
                log.warn("Failed to deserialize as type {}. Payload size: {}", type, size, e);
                throw Throwables.propagate(e);
            }
        }
    }
}
