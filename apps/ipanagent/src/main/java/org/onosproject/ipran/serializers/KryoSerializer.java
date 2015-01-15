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
package org.onosproject.ipran.serializers;

import org.onlab.netty.Endpoint;
import org.onlab.util.KryoNamespace;
import org.onosproject.ipran.IpranMessage;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.nio.ByteBuffer;

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
        serializerPool = KryoNamespace.newBuilder()
                .register(byte[].class)
                .register(new IpranMessageSerializer(), IpranMessage.class)
                .register(new EndPointSerializer(), Endpoint.class)
                .build();
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

    public static final class IpranMessageSerializer
            extends Serializer<IpranMessage> {

        @Override
        public void write(Kryo kryo, Output output, IpranMessage object) {
            output.writeLong(object.id());
            kryo.writeClassAndObject(output, object.sender());
            output.writeString(object.type());
            output.writeInt(object.payload().length, true);
            output.writeBytes(object.payload());
        }

        @Override
        public IpranMessage read(Kryo kryo, Input input,
                                    Class<IpranMessage> type) {
            long id = input.readLong();
            Endpoint sender = (Endpoint) kryo.readClassAndObject(input);
            String msgtype = input.readString();
            int length = input.readInt(true);
            byte[] payload = input.readBytes(length);
            return new IpranMessage(id, sender, msgtype, payload);
        }
    }
    public static final class EndPointSerializer extends Serializer<Endpoint> {

        @Override
        public void write(Kryo kryo, Output output, Endpoint object) {
            output.writeString(object.host());
            output.writeInt(object.port());
        }

        @Override
        public Endpoint read(Kryo kryo, Input input, Class<Endpoint> type) {
            String host = input.readString();
            int port = input.readInt();
            return new Endpoint(host, port);
        }
    }
}
