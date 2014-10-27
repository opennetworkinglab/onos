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
package org.onlab.netty;

import org.onlab.util.KryoNamespace;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;

//FIXME: Should be move out to test or app
/**
 * Kryo Serializer.
 */
public class KryoSerializer {

    private KryoNamespace serializerPool;

    public KryoSerializer() {
        setupKryoPool();
    }

    /**
     * Sets up the common serialzers pool.
     */
    protected void setupKryoPool() {
        // FIXME Slice out types used in common to separate pool/namespace.
        serializerPool = KryoNamespace.newBuilder()
                .register(ArrayList.class,
                          HashMap.class,
                          ArrayList.class,
                          InternalMessage.class,
                          Endpoint.class,
                          byte[].class
                )
                .build()
                .populate(1);
    }


    public <T> T decode(byte[] data) {
        return serializerPool.deserialize(data);
    }

    public byte[] encode(Object payload) {
        return serializerPool.serialize(payload);
    }

    public <T> T decode(ByteBuffer buffer) {
        return serializerPool.deserialize(buffer);
    }

    public void encode(Object obj, ByteBuffer buffer) {
        serializerPool.serialize(obj, buffer);
    }
}
