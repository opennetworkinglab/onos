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
package org.onlab.util;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.commons.lang3.tuple.Pair;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.pool.KryoFactory;
import com.google.common.collect.ImmutableList;

// TODO Add tests for this class.
/**
 * Pool of Kryo instances, with classes pre-registered.
 */
//@ThreadSafe
public final class KryoNamespace implements KryoFactory {

    /**
     * Default buffer size used for serialization.
     *
     * @see #serialize(Object)
     */
    public static final int DEFAULT_BUFFER_SIZE = 4096;
    public static final int MAX_BUFFER_SIZE = 100 * 1000 * 1000;

    private final ConcurrentLinkedQueue<Kryo> pool = new ConcurrentLinkedQueue<>();
    private final ImmutableList<Pair<Class<?>, Serializer<?>>> registeredTypes;
    private final boolean registrationRequired;

    /**
     * KryoNamespace builder.
     */
    //@NotThreadSafe
    public static final class Builder {

        private final List<Pair<Class<?>, Serializer<?>>> types = new ArrayList<>();
        private boolean registrationRequired = true;

        /**
         * Builds a {@link KryoNamespace} instance.
         *
         * @return KryoNamespace
         */
        public KryoNamespace build() {
            return new KryoNamespace(types, registrationRequired);
        }

        /**
         * Registers classes to be serialized using Kryo default serializer.
         *
         * @param expectedTypes list of classes
         * @return this
         */
        public Builder register(final Class<?>... expectedTypes) {
            for (Class<?> clazz : expectedTypes) {
                types.add(Pair.<Class<?>, Serializer<?>>of(clazz, null));
            }
            return this;
        }

        /**
         * Registers a class and it's serializer.
         *
         * @param clazz the class to register
         * @param serializer serializer to use for the class
         * @return this
         */
        public Builder register(final Class<?> clazz, Serializer<?> serializer) {
            types.add(Pair.<Class<?>, Serializer<?>>of(clazz, serializer));
            return this;
        }

        /**
         * Registers all the class registered to given KryoNamespace.
         *
         * @param pool KryoNamespace
         * @return this
         */
        public Builder register(final KryoNamespace pool) {
            types.addAll(pool.registeredTypes);
            return this;
        }

        public Builder setRegistrationRequired(boolean registrationRequired) {
            this.registrationRequired = registrationRequired;
            return this;
        }
    }

    /**
     * Creates a new {@link KryoNamespace} builder.
     *
     * @return builder
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Creates a Kryo instance pool.
     *
     * @param registeredTypes types to register
     * @param registrationRequired
     */
    private KryoNamespace(final List<Pair<Class<?>, Serializer<?>>> registeredTypes, boolean registrationRequired) {
        this.registeredTypes = ImmutableList.copyOf(registeredTypes);
        this.registrationRequired = registrationRequired;
    }

    /**
     * Populates the Kryo pool.
     *
     * @param instances to add to the pool
     * @return this
     */
    public KryoNamespace populate(int instances) {
        List<Kryo> kryos = new ArrayList<>(instances);
        for (int i = 0; i < instances; ++i) {
            kryos.add(create());
        }
        pool.addAll(kryos);
        return this;
    }

    /**
     * Gets a Kryo instance from the pool.
     *
     * @return Kryo instance
     */
    public Kryo getKryo() {
        Kryo kryo = pool.poll();
        if (kryo == null) {
            return create();
        }
        return kryo;
    }

    /**
     * Returns a Kryo instance to the pool.
     *
     * @param kryo instance obtained from this pool.
     */
    public void putKryo(Kryo kryo) {
        if (kryo != null) {
            pool.add(kryo);
        }
    }

    /**
     * Serializes given object to byte array using Kryo instance in pool.
     * <p>
     * Note: Serialized bytes must be smaller than {@link #MAX_BUFFER_SIZE}.
     *
     * @param obj Object to serialize
     * @return serialized bytes
     */
    public byte[] serialize(final Object obj) {
        return serialize(obj, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Serializes given object to byte array using Kryo instance in pool.
     *
     * @param obj Object to serialize
     * @param bufferSize maximum size of serialized bytes
     * @return serialized bytes
     */
    public byte[] serialize(final Object obj, final int bufferSize) {
        ByteBufferOutput out = new ByteBufferOutput(bufferSize, MAX_BUFFER_SIZE);
        Kryo kryo = getKryo();
        try {
            kryo.writeClassAndObject(out, obj);
            out.flush();
            return out.toBytes();
        } finally {
            putKryo(kryo);
        }
    }

    /**
     * Serializes given object to byte buffer using Kryo instance in pool.
     *
     * @param obj Object to serialize
     * @param buffer to write to
     */
    public void serialize(final Object obj, final ByteBuffer buffer) {
        ByteBufferOutput out = new ByteBufferOutput(buffer);
        Kryo kryo = getKryo();
        try {
            kryo.writeClassAndObject(out, obj);
            out.flush();
        } finally {
            putKryo(kryo);
        }
    }

    /**
     * Deserializes given byte array to Object using Kryo instance in pool.
     *
     * @param bytes serialized bytes
     * @param <T> deserialized Object type
     * @return deserialized Object
     */
    public <T> T deserialize(final byte[] bytes) {
        Input in = new Input(bytes);
        Kryo kryo = getKryo();
        try {
            @SuppressWarnings("unchecked")
            T obj = (T) kryo.readClassAndObject(in);
            return obj;
        } finally {
            putKryo(kryo);
        }
    }

    /**
     * Deserializes given byte buffer to Object using Kryo instance in pool.
     *
     * @param buffer input with serialized bytes
     * @param <T> deserialized Object type
     * @return deserialized Object
     */
    public <T> T deserialize(final ByteBuffer buffer) {
        ByteBufferInput in = new ByteBufferInput(buffer);
        Kryo kryo = getKryo();
        try {
            @SuppressWarnings("unchecked")
            T obj = (T) kryo.readClassAndObject(in);
            return obj;
        } finally {
            putKryo(kryo);
        }
    }

    /**
     * Creates a Kryo instance with {@link #registeredTypes} pre-registered.
     *
     * @return Kryo instance
     */
    @Override
    public Kryo create() {
        Kryo kryo = new Kryo();
        kryo.setRegistrationRequired(registrationRequired);
        for (Pair<Class<?>, Serializer<?>> registry : registeredTypes) {
            final Serializer<?> serializer = registry.getRight();
            if (serializer == null) {
                kryo.register(registry.getLeft());
            } else {
                kryo.register(registry.getLeft(), serializer);
                if (serializer instanceof FamilySerializer) {
                    FamilySerializer<?> fser = (FamilySerializer<?>) serializer;
                    fser.registerFamilies(kryo);
                }
            }
        }
        return kryo;
    }

    /**
     * Serializer implementation, which required registration of family of Classes.
     * @param <T> base type of this serializer.
     */
    public abstract static class FamilySerializer<T> extends Serializer<T> {


        public FamilySerializer(boolean acceptsNull) {
            super(acceptsNull);
        }

        public FamilySerializer(boolean acceptsNull, boolean immutable) {
            super(acceptsNull, immutable);
        }

        /**
         * Registers other classes this Serializer supports.
         *
         * @param kryo instance to register classes to
         */
        public void registerFamilies(Kryo kryo) {
        }
    }
}
