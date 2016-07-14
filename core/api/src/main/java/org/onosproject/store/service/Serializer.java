/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.store.service;

import java.util.Arrays;
import java.util.List;

import org.onlab.util.KryoNamespace;

import com.google.common.collect.Lists;

/**
 * Interface for serialization of store artifacts.
 */
public interface Serializer {
    /**
     * Serialize the specified object.
     * @param object object to serialize.
     * @return serialized bytes.
     * @param <T> encoded type
     */
    <T> byte[] encode(T object);

    /**
     * Deserialize the specified bytes.
     * @param bytes byte array to deserialize.
     * @return deserialized object.
     * @param <T> decoded type
     */
    <T> T decode(byte[] bytes);

    /**
     * Creates a new Serializer instance from a KryoNamespace.
     *
     * @param kryo kryo namespace
     * @return Serializer instance
     */
    static Serializer using(KryoNamespace kryo) {
        return new Serializer() {

            @Override
            public <T> byte[] encode(T object) {
                return kryo.serialize(object);
            }

            @Override
            public <T> T decode(byte[] bytes) {
                return kryo.deserialize(bytes);
            }
        };
    }

    /**
     * Creates a new Serializer instance from a KryoNamespace and some additional classes.
     *
     * @param namespace kryo namespace
     * @param classes variable length array of classes to register
     * @return Serializer instance
     */
    static Serializer using(KryoNamespace namespace, Class<?>... classes) {
        return using(Arrays.asList(namespace), classes);
    }

    /**
     * Creates a new Serializer instance from a list of KryoNamespaces and some additional classes.
     *
     * @param namespaces kryo namespaces
     * @param classes variable length array of classes to register
     * @return Serializer instance
     */
    static Serializer using(List<KryoNamespace> namespaces, Class<?>... classes) {
        KryoNamespace.Builder builder = new KryoNamespace.Builder();
        namespaces.forEach(builder::register);
        Lists.newArrayList(classes).forEach(builder::register);
        KryoNamespace namespace = builder.build();
        return new Serializer() {
            @Override
            public <T> byte[] encode(T object) {
                return namespace.serialize(object);
            }

            @Override
            public <T> T decode(byte[] bytes) {
                return namespace.deserialize(bytes);
            }
        };
    }

    static Serializer forTypes(Class<?>... classes) {
        return using(KryoNamespace.newBuilder()
                                  .register(classes)
                                  .register(MapEvent.class, MapEvent.Type.class)
                                  .build());
    }
}
