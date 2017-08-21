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
package org.onosproject.store.serializers;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.onlab.util.KryoNamespace;
import org.onosproject.store.service.Serializer;

/**
 * Service to serialize Objects into byte array.
 *
 * @deprecated since 1.11 ("Loon")
 */
@Deprecated
public interface StoreSerializer extends Serializer {

    /**
     * Serializes the specified object into bytes.
     *
     * @param obj object to be serialized
     * @return serialized bytes
     */
    @Override
    byte[] encode(final Object obj);

    /**
     * Serializes the specified object into bytes.
     *
     * @param obj object to be serialized
     * @param buffer to write serialized bytes
     */
    void encode(final Object obj, ByteBuffer buffer);

    /**
     * Serializes the specified object into bytes.
     *
     * @param obj object to be serialized
     * @param stream to write serialized bytes
     */
    void encode(final Object obj, final OutputStream stream);

    /**
     * Deserializes the specified bytes into an object.
     *
     * @param bytes bytes to be deserialized
     * @return deserialized object
     * @param <T> decoded type
     */
    @Override
    <T> T decode(final byte[] bytes);

    /**
     * Deserializes the specified bytes into an object.
     *
     * @param buffer bytes to be deserialized
     * @return deserialized object
     * @param <T> decoded type
     */
    <T> T decode(final ByteBuffer buffer);

    /**
     * Deserializes the specified bytes into an object.
     *
     * @param stream stream containing the bytes to be deserialized
     * @return deserialized object
     * @param <T> decoded type
     */
    <T> T decode(final InputStream stream);

    /**
     * Returns a copy of the specfied object.
     *
     * @param object object to copy
     * @return a copy of the object
     * @param <T> object type
     */
    <T> T copy(final T object);

    /**
     * Creates a new StoreSerializer instance from a KryoNamespace.
     *
     * @param ns kryo namespace
     * @return StoreSerializer instance
     */
    static StoreSerializer using(KryoNamespace ns) {
        return new StoreSerializer() {

            @Override
            public void encode(Object obj, OutputStream stream) {
                ns.serialize(obj, stream);
            }

            @Override
            public void encode(Object obj, ByteBuffer buffer) {
                ns.serialize(obj, buffer);
            }

            @Override
            public byte[] encode(Object obj) {
                return ns.serialize(obj);
            }

            @Override
            public <T> T decode(InputStream stream) {
                return ns.deserialize(stream);
            }

            @Override
            public <T> T decode(ByteBuffer buffer) {
                return ns.deserialize(buffer);
            }

            @Override
            public <T> T decode(byte[] bytes) {
                return ns.deserialize(bytes);
            }

            @Override
            public <T> T copy(T object) {
                return ns.run(kryo -> kryo.copy(object));
            }
        };
    }
}
