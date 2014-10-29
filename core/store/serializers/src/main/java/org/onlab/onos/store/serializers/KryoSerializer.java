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
package org.onlab.onos.store.serializers;

import org.onlab.util.KryoNamespace;
import java.nio.ByteBuffer;

/**
 * StoreSerializer implementation using Kryo.
 */
public class KryoSerializer implements StoreSerializer {

    protected KryoNamespace serializerPool;

    public KryoSerializer() {
        setupKryoPool();
    }

    /**
     * Sets up the common serialzers pool.
     */
    protected void setupKryoPool() {
        serializerPool = KryoNamespace.newBuilder()
                .register(KryoNamespaces.API)
                .build()
                .populate(1);
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

}
