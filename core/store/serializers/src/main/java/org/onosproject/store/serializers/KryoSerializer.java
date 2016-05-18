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
package org.onosproject.store.serializers;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.onlab.util.KryoNamespace;

import com.google.common.base.MoreObjects;

/**
 * StoreSerializer implementation using Kryo.
 *
 * @deprecated in Goldeneye (1.6.0)
 */
@Deprecated
public class KryoSerializer implements StoreSerializer {

    protected KryoNamespace serializerPool;

    public KryoSerializer() {
        setupKryoPool();
    }

    /**
     * Sets up the common serializers pool.
     */
    protected void setupKryoPool() {
        serializerPool = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .nextId(KryoNamespaces.BEGIN_USER_CUSTOM_ID)
                .build();
    }

    @Override
    public byte[] encode(final Object obj) {
        return serializerPool.serialize(obj);
    }

    @Override
    public <T> T decode(final byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        return serializerPool.deserialize(bytes);
    }

    @Override
    public void encode(Object obj, ByteBuffer buffer) {
        serializerPool.serialize(obj, buffer);
    }

    @Override
    public <T> T decode(ByteBuffer buffer) {
        return serializerPool.deserialize(buffer);
    }

    @Override
    public void encode(Object obj, OutputStream stream) {
        serializerPool.serialize(obj, stream);
    }

    @Override
    public <T> T decode(InputStream stream) {
        return serializerPool.deserialize(stream);
    }

    @Override
    public <T> T copy(T object) {
        return decode(encode(object));
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("serializerPool", serializerPool)
                .toString();
    }
}
